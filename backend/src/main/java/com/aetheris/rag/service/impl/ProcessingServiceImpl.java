/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.exception.BadRequestException;
import com.aetheris.rag.exception.ConflictException;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.service.DocumentService;
import com.aetheris.rag.service.ProcessingService;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.util.FileOperationUtil;
import com.aetheris.rag.util.FileValidationUtil;
import com.aetheris.rag.util.PermissionCheckUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源处理协调服务实现类。
 *
 * <p>负责协调资源处理过程中的多个Service调用，包括文件验证、文档解析、切片生成、向量化等复合操作。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingServiceImpl implements ProcessingService {

  private final ResourceService resourceService;
  private final VectorService vectorService;
  private final DocumentService documentService;
  private final ResourceMapper resourceMapper;
  private final ChunkMapper chunkMapper;
  private final RedissonClient redissonClient;

  @Value("${upload.dir:uploads}")
  private String uploadDir;

  @Value("${chunk.size:1000}")
  private int chunkSize;

  @Value("${chunk.overlap:200}")
  private int chunkOverlap;

  /** 分布式锁等待时间（秒） */
  private static final int LOCK_WAIT_TIME = 10;

  /** 分布式锁自动释放时间（秒） */
  private static final int LOCK_LEASE_TIME = 60;

  @Override
  @Transactional
  public Resource processResourceUpload(MultipartFile file, String title, String tags,
      String description, Long uploadedBy) throws Exception {
    log.info("开始处理资源上传: title={}, file={}", title, file.getOriginalFilename());

    // 1. 文件验证
    FileOperationUtil.validateFile(file);

    byte[] fileBytes = file.getBytes();
    FileValidationUtil.ValidationResult validationResult =
        documentService.validateFileFormat(fileBytes, file.getOriginalFilename());
    if (!validationResult.isValid()) {
      throw new BadRequestException(validationResult.getErrorMessage());
    }

    // 2. 计算内容哈希
    String contentHash = documentService.calculateContentHash(fileBytes);
    log.debug("文件内容哈希: {}", contentHash);

    // 3. 去重检查
    Resource existingResource = resourceService.findByContentHash(contentHash);
    if (existingResource != null) {
      log.info("资源已存在（内容哈希重复）: {}", existingResource.getId());
      return handleDuplicateResource(existingResource);
    }

    // 4. 使用分布式锁处理新资源
    String lockKey = "resource:upload:" + contentHash;
    RLock lock = redissonClient.getLock(lockKey);

    boolean lockAcquired = false;
    Path filePath = null;

    try {
      lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

      if (!lockAcquired) {
        log.warn("获取分布式锁失败，可能有其他进程正在上传相同内容: {}", contentHash);
        Thread.sleep(1000);
        existingResource = resourceService.findByContentHash(contentHash);
        if (existingResource != null) {
          return existingResource;
        }
        throw new ConflictException("系统繁忙，请稍后重试");
      }

      // 5. 保存文件
      filePath = FileOperationUtil.saveUploadedFile(file, uploadDir);

      // 6. 创建资源记录
      Resource resource = createResourceRecord(file, title, tags, description, contentHash,
          uploadedBy, filePath);
      resourceMapper.insert(resource);
      log.info("资源记录已创建: {}", resource.getId());

      // 7. 处理文档并生成切片
      List<Chunk> chunks = documentService.processDocument(filePath.toString(),
          resource.getFileType(), resource.getId(), chunkSize, chunkOverlap);

      // 8. 插入切片记录
      if (!chunks.isEmpty()) {
        chunkMapper.batchInsert(chunks);
        log.info("已插入 {} 个切片", chunks.size());

        // 更新资源切片数量
        resourceService.updateChunkVectorizationStatus(resource.getId(), chunks.size(), false);
      }

      // 9. 触发向量化
      try {
        vectorService.vectorizeChunks(resource.getId());
        log.info("向量化任务已触发: 资源ID={}", resource.getId());
      } catch (Exception e) {
        log.error("向量化失败: 资源ID={}", resource.getId(), e);
        // 向量化失败不影响上传成功
      }

      return resource;
    } catch (Exception e) {
      // 清理已保存的文件
      if (filePath != null && Files.exists(filePath)) {
        try {
          Files.delete(filePath);
          log.info("上传失败，已删除文件: {}", filePath);
        } catch (IOException ex) {
          log.warn("删除文件失败: {}", filePath, ex);
        }
      }
      throw e;
    } finally {
      // 确保锁被正确释放
      if (lockAcquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
        log.debug("已释放分布式锁: {}", lockKey);
      }
    }
  }

  @Override
  @Transactional
  public Resource processResourceDeletion(Long id, Long userId) {
    log.info("开始删除资源: resourceId={}, userId={}", id, userId);

    // 1. 查询资源
    Resource resource = resourceMapper.findById(id);
    if (resource == null) {
      throw new IllegalArgumentException("资源不存在");
    }

    // 2. 权限检查
    PermissionCheckUtil.checkResourceOwnership(resource, userId);

    // 3. 删除向量数据
    vectorService.deleteVectorDataByResourceIds(List.of(resource.getId()));

    // 4. 删除物理文件
    FileOperationUtil.deletePhysicalFile(resource.getFilePath());

    // 5. 删除切片和资源记录
    resourceMapper.deleteChunksByResourceId(id);
    resourceMapper.deleteById(id);

    log.info("资源已删除: resourceId={}, title={}, userId={}", id, resource.getTitle(), userId);
    return resource;
  }

  @Override
  @Transactional
  public List<Resource> processBatchResourceDeletion(List<Long> ids, Long userId) {
    log.info("开始批量删除资源: count={}, userId={}", ids.size(), userId);

    // 1. 批量查询资源
    List<Resource> resources = resourceMapper.findByIds(ids);
    if (resources.isEmpty()) {
      throw new IllegalArgumentException("资源不存在");
    }

    // 2. 权限过滤
    List<Resource> authorizedResources = PermissionCheckUtil.filterOwnedResources(resources, userId);
    List<Long> authorizedIds = authorizedResources.stream()
        .map(Resource::getId)
        .toList();

    // 3. 批量删除向量数据
    vectorService.deleteVectorDataByResourceIds(authorizedIds);

    // 4. 批量删除物理文件
    for (Resource resource : authorizedResources) {
      FileOperationUtil.deletePhysicalFile(resource.getFilePath());
    }

    // 5. 批量删除切片和资源记录
    for (Long id : authorizedIds) {
      resourceMapper.deleteChunksByResourceId(id);
    }
    resourceMapper.deleteByIds(authorizedIds);

    log.info("批量删除完成: count={}", authorizedResources.size());
    return authorizedResources;
  }

  @Override
  @Transactional
  public int reprocessResource(Long resourceId) throws Exception {
    log.info("重新处理资源: resourceId={}", resourceId);

    // 1. 查询资源
    Resource resource = resourceMapper.findById(resourceId);
    if (resource == null) {
      throw new IllegalArgumentException("资源不存在: " + resourceId);
    }

    // 2. 删除旧切片
    List<Chunk> oldChunks = chunkMapper.findByResourceId(resourceId);
    if (!oldChunks.isEmpty()) {
      chunkMapper.deleteByResourceId(resourceId);
      log.info("删除了 {} 个旧切片", oldChunks.size());
    }

    // 3. 重新处理文档生成切片
    String filePath = resource.getFilePath();
    String fileType = resource.getFileType();
    List<Chunk> chunks = documentService.processDocument(filePath, fileType, resourceId,
        chunkSize, chunkOverlap);

    // 4. 插入新切片
    if (!chunks.isEmpty()) {
      chunkMapper.batchInsert(chunks);
      log.info("重新生成了 {} 个切片", chunks.size());

      // 更新资源切片数量
      resourceService.updateChunkVectorizationStatus(resourceId, chunks.size(), false);

      // 5. 触发向量化
      vectorService.vectorizeChunks(resourceId);
      log.info("向量化任务已触发: 资源ID={}", resourceId);
    }

    return chunks.size();
  }

  @Override
  public RebuildResult fullRebuild(RebuildConfig config) throws Exception {
    log.info("开始完全重建: config={}", config);

    // 委托给VectorService执行重建
    // 注意：VectorService.rebuildAllResourcesWithConfig 会处理：
    // 1. 删除所有向量数据和索引
    // 2. 重新解析所有文档（通过回调ProcessingService）
    // 3. 批量向量化
    return vectorService.rebuildAllResourcesWithConfig(config);
  }

  // ==================== 私有辅助方法 ====================

  /**
   * 处理重复资源。
   */
  private Resource handleDuplicateResource(Resource existingResource) {
    existingResource.setDuplicate(true);

    // 检查是否需要重新向量化
    if (!existingResource.getVectorized()) {
      log.info("资源未向量化，触发向量化: 资源ID={}", existingResource.getId());
      try {
        vectorService.vectorizeChunks(existingResource.getId());
        log.info("向量化任务已触发: 资源ID={}", existingResource.getId());
      } catch (Exception e) {
        log.error("向量化失败: 资源ID={}", existingResource.getId(), e);
        // 向量化失败不影响返回资源
      }
    } else {
      log.info("资源已向量化，跳过: 资源ID={}", existingResource.getId());
    }

    return existingResource;
  }

  /**
   * 创建资源记录。
   */
  private Resource createResourceRecord(MultipartFile file, String title, String tags,
      String description, String contentHash, Long uploadedBy, Path filePath)
      throws IOException {
    String savedFileName = filePath.getFileName().toString();
    String fileType = FileOperationUtil.getFileType(savedFileName);
    long fileSize = Files.size(filePath);

    return Resource.builder()
        .title(title)
        .tags(tags)
        .fileType(fileType)
        .filePath(filePath.toString())
        .fileSize(fileSize)
        .description(description)
        .contentHash(contentHash)
        .uploadedBy(uploadedBy)
        .uploadTime(Instant.now())
        .chunkCount(0)
        .vectorized(false)
        .build();
  }

  @Override
  public boolean cancelRebuild() {
    return vectorService.cancelRebuild();
  }

  @Override
  public boolean isRebuilding() {
    // 检查重建锁是否存在
    String lockKey = "rebuild:global:lock";
    RLock lock = redissonClient.getLock(lockKey);
    boolean isLocked = lock.isLocked();
    log.debug("重建状态检查: lockKey={}, isLocked={}", lockKey, isLocked);
    return isLocked;
  }
}
