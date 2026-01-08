/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.util.HashUtil;
import com.aetheris.rag.util.MarkdownProcessor;
import com.aetheris.rag.util.PdfProcessor;
import com.aetheris.rag.util.FileValidationUtil;
import com.aetheris.rag.exception.ConflictException;
import com.aetheris.rag.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源服务实现类。
 *
 * <p>实现资源上传、切片、向量化入库等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

  private final ResourceMapper resourceMapper;
  private final ChunkMapper chunkMapper;
  private final PdfProcessor pdfProcessor;
  private final MarkdownProcessor markdownProcessor;
  private final VectorService vectorService;
  private final RedissonClient redissonClient;
  private final StringRedisTemplate redisTemplate;

  /** 切片大小（字符数） */
  @Value("${chunk.size:1000}")
  private int chunkSize;

  /** 切片重叠大小（字符数） */
  @Value("${chunk.overlap:200}")
  private int chunkOverlap;

  /** 上传目录 */
  @Value("${upload.dir:uploads}")
  private String uploadDir;

  /** 分布式锁等待时间（秒） */
  private static final int LOCK_WAIT_TIME = 10;

  /** 分布式锁自动释放时间（秒） */
  private static final int LOCK_LEASE_TIME = 60;

  /**
   * 保存上传的文件到指定目录
   *
   * @param file 上传的文件
   * @return 保存后的文件路径
   * @throws IOException 如果文件保存失败
   */
  private Path saveUploadedFile(MultipartFile file) throws IOException {
    // 创建上传目录（如果不存在）
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
      log.info("创建上传目录: {}", uploadPath);
    }

    // 生成唯一文件名（时间戳 + 原始文件名）
    String originalFilename = file.getOriginalFilename();
    String fileName = System.currentTimeMillis() + "_" + originalFilename;
    Path targetPath = uploadPath.resolve(fileName).normalize();

    // 保存文件（使用 InputStream 复制，避免 transferTo 的路径问题）
    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
      log.info("文件已保存: {}", targetPath);
    }

    return targetPath;
  }

  @Override
  @Transactional
  public Resource uploadResource(
      MultipartFile file, String title, String tags, String description, Long uploadedBy)
      throws Exception {
    log.info("开始上传资源: {}, 文件: {}", title, file.getOriginalFilename());

    // 1. 检查空文件
    if (file.isEmpty() || file.getSize() == 0) {
      throw new IllegalArgumentException("文件为空，请上传非空文件");
    }

    // 2. 检查文件大小
    if (file.getSize() > 50 * 1024 * 1024) {
      throw new IllegalArgumentException("文件大小超过 50MB 限制");
    }

    // 3. 检查文件类型
    String fileName = file.getOriginalFilename();
    if (fileName == null ||
        (!fileName.endsWith(".md") && !fileName.endsWith(".markdown") && !fileName.endsWith(".pdf"))) {
      throw new IllegalArgumentException("不支持的文件类型，仅支持 Markdown 和 PDF");
    }

    // 3.5. 严格验证文件格式（在计算哈希之前）
    byte[] fileBytes = file.getBytes();
    FileValidationUtil.ValidationResult validationResult =
        FileValidationUtil.validateFileFormat(fileBytes, fileName);
    if (!validationResult.isValid()) {
      throw new BadRequestException(validationResult.getErrorMessage());
    }

    // 4. 计算文件内容哈希
    String contentHash = HashUtil.sha256(fileBytes);
    log.debug("文件内容哈希: {}", contentHash);

    // 2. 检查是否已存在（幂等性）
    Resource existingResource = resourceMapper.findByContentHash(contentHash);
    if (existingResource != null) {
      log.info("资源已存在（内容哈希重复）: {}", existingResource.getId());

      // 标记为重复上传
      existingResource.setDuplicate(true);

      // 检查是否需要重新向量化
      if (!existingResource.getVectorized()) {
        log.info("资源未向量化，触发向量化: 资源ID={}", existingResource.getId());

        // 异步触发向量化（避免阻塞）
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

    // 获取分布式锁，防止并发重复上传
    String lockKey = "resource:upload:" + contentHash;
    RLock lock = redissonClient.getLock(lockKey);

    boolean lockAcquired = false;
    Path filePath = null;

    try {
      // 尝试获取锁（等待时间、自动释放时间、时间单位）
      lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

      if (!lockAcquired) {
        log.warn("获取分布式锁失败，可能有其他进程正在上传相同内容: {}", contentHash);
        // 等待并重试查找
        Thread.sleep(1000);
        existingResource = resourceMapper.findByContentHash(contentHash);
        if (existingResource != null) {
          return existingResource;
        }
        throw new ConflictException("系统繁忙，请稍后重试");
      }

      // 3. 只对新资源保存文件到服务器
      filePath = saveUploadedFile(file);

      // 读取文件信息
      String savedFileName = filePath.getFileName().toString();
      String fileType = getFileType(savedFileName);
      long fileSize = Files.size(filePath);

      // 创建资源实体
      Resource resource =
          Resource.builder()
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

      // 插入资源记录
      resourceMapper.insert(resource);
      log.info("资源记录已创建: {}", resource.getId());

      // 处理文档并生成切片
      List<Chunk> chunks = processDocument(filePath.toString(), fileType, resource.getId());

      // 插入切片记录
      if (!chunks.isEmpty()) {
        chunkMapper.batchInsert(chunks);
        log.info("已插入 {} 个切片", chunks.size());

        // 更新资源切片数量
        resourceMapper.updateChunkStatus(resource.getId(), chunks.size(), false);
      }

      // 触发向量化
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

      // 抛出原始异常（事务会自动回滚数据库操作）
      throw e;
    } finally {
      // 释放分布式锁
      if (lockAcquired) {
        lock.unlock();
      }
    }
  }

  @Override
  public Resource getResourceById(Long id) {
    return resourceMapper.findById(id);
  }

  @Override
  public List<Resource> getResourceList(int offset, int limit) {
    return resourceMapper.findPaged(offset, limit);
  }

  @Override
  public List<Chunk> getChunksByResourceId(Long resourceId) {
    return chunkMapper.findByResourceId(resourceId);
  }

  @Override
  public Resource findByContentHash(String contentHash) {
    return resourceMapper.findByContentHash(contentHash);
  }

  @Override
  public Long getResourceCount() {
    return Long.valueOf(resourceMapper.count());
  }

  @Override
  @Transactional
  public Resource updateResource(Long id, String title, String tags, String description) {
    Resource resource = resourceMapper.findById(id);
    if (resource == null) {
      throw new RuntimeException("资源不存在");
    }

    // 更新字段
    resource.setTitle(title);
    resource.setTags(tags);
    resource.setDescription(description);

    // 持久化
    resourceMapper.update(resource);

    log.info("资源信息已更新: resourceId={}, title={}", id, title);
    return resource;
  }

  @Override
  @Transactional
  public Resource deleteResource(Long id, Long userId) {
    // 1. 查询资源
    Resource resource = resourceMapper.findById(id);
    if (resource == null) {
      throw new RuntimeException("资源不存在");
    }

    // 2. 权限检查
    if (!resource.getUploadedBy().equals(userId)) {
      throw new RuntimeException("无权删除此资源");
    }

    // 3. 删除 Redis 向量数据
    deleteVectorData(List.of(resource.getId()));

    // 4. 删除物理文件
    deletePhysicalFile(resource.getFilePath());

    // 5. 删除切片和资源记录
    resourceMapper.deleteChunksByResourceId(id);
    resourceMapper.deleteById(id);

    log.info("资源已删除: resourceId={}, title={}, userId={}", id, resource.getTitle(), userId);
    return resource;
  }

  @Override
  @Transactional
  public List<Resource> deleteResources(List<Long> ids, Long userId) {
    // 1. 批量查询资源（权限检查）
    List<Resource> resources = resourceMapper.findByIds(ids);
    if (resources.isEmpty()) {
      throw new RuntimeException("资源不存在");
    }

    // 2. 权限过滤：只保留用户自己的资源
    List<Resource> authorizedResources = resources.stream()
        .filter(r -> r.getUploadedBy().equals(userId))
        .collect(java.util.stream.Collectors.toList());

    if (authorizedResources.isEmpty()) {
      throw new RuntimeException("无权删除这些资源");
    }

    List<Long> authorizedIds = authorizedResources.stream()
        .map(Resource::getId)
        .collect(java.util.stream.Collectors.toList());

    // 3. 批量删除 Redis 向量数据（SQL 优化）
    deleteVectorData(authorizedIds);

    // 4. 批量删除物理文件
    for (Resource resource : authorizedResources) {
      deletePhysicalFile(resource.getFilePath());
    }

    // 5. 批量删除切片和资源记录（SQL 优化）
    resourceMapper.deleteChunksByResourceIds(authorizedIds);
    resourceMapper.deleteByIds(authorizedIds);

    log.info("批量删除完成: userId={}, 成功 {}/{}", userId, authorizedIds.size(), ids.size());
    return authorizedResources;
  }

  /**
   * 批量删除向量数据。
   */
  private void deleteVectorData(List<Long> resourceIds) {
    try {
      // 批量查询所有切片
      List<Chunk> chunks = chunkMapper.findByResourceIds(resourceIds);
      if (!chunks.isEmpty()) {
        // 批量删除 Redis keys
        List<String> keys = chunks.stream()
            .map(chunk -> "chunk:" + chunk.getId())
            .collect(java.util.stream.Collectors.toList());

        redisTemplate.delete(keys);
        log.info("已删除资源的向量数据，共 {} 个切片", chunks.size());
      }
    } catch (Exception e) {
      log.warn("批量删除向量数据失败: resourceIds={}", resourceIds, e);
      // 继续删除数据库记录
    }
  }

  /**
   * 删除物理文件。
   */
  private void deletePhysicalFile(String filePath) {
    try {
      Path path = Paths.get(filePath);
      if (Files.exists(path)) {
        Files.delete(path);
        log.info("已删除物理文件: {}", filePath);
      }
    } catch (IOException e) {
      log.warn("删除物理文件失败: {}", filePath, e);
      // 继续删除数据库记录
    }
  }

  /**
   * 处理文档并生成切片。
   *
   * @param filePath 文件路径
   * @param fileType 文件类型
   * @param resourceId 资源ID
   * @return 切片列表
   * @throws IOException 如果文件处理失败
   */
  private List<Chunk> processDocument(String filePath, String fileType, Long resourceId)
      throws IOException {
    List<Chunk> chunks;

    if ("PDF".equalsIgnoreCase(fileType)) {
      chunks = pdfProcessor.process(filePath, chunkSize, chunkOverlap);
    } else if ("MARKDOWN".equalsIgnoreCase(fileType)) {
      chunks = markdownProcessor.process(filePath, chunkSize, chunkOverlap);
    } else {
      throw new IllegalArgumentException("不支持的文件类型: " + fileType);
    }

    // 设置切片元数据
    Instant now = Instant.now();
    for (Chunk chunk : chunks) {
      chunk.setResourceId(resourceId);
      chunk.setVectorized(false);
      chunk.setCreatedAt(now);

      // 计算文本哈希
      String textHash = HashUtil.sha256(chunk.getChunkText());
      chunk.setTextHash(textHash);
    }

    return chunks;
  }

  /**
   * 计算文件内容哈希。
   *
   * @param filePath 文件路径
   * @return SHA-256 哈希值
   * @throws IOException 如果文件读取失败
   */
  private String calculateContentHash(Path filePath) throws IOException {
    byte[] content = Files.readAllBytes(filePath);
    return HashUtil.sha256(content);
  }

  /**
   * 根据文件名获取文件类型。
   *
   * @param fileName 文件名
   * @return 文件类型（PDF/MARKDOWN）
   */
  private String getFileType(String fileName) {
    String lowerName = fileName.toLowerCase();
    if (lowerName.endsWith(".pdf")) {
      return "PDF";
    } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
      return "MARKDOWN";
    } else {
      throw new IllegalArgumentException("不支持的文件类型: " + fileName);
    }
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
    List<Chunk> chunks = processDocument(filePath, fileType, resourceId);

    // 4. 插入新切片
    if (!chunks.isEmpty()) {
      chunkMapper.batchInsert(chunks);
      log.info("重新生成了 {} 个切片", chunks.size());

      // 更新资源切片数量
      resourceMapper.updateChunkStatus(resourceId, chunks.size(), false);

      // 5. 触发向量化
      vectorService.vectorizeChunks(resourceId);
      log.info("向量化任务已触发: 资源ID={}", resourceId);
    }

    return chunks.size();
  }
}
