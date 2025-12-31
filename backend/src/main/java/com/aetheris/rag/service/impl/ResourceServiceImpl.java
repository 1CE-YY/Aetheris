/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.model.Chunk;
import com.aetheris.rag.model.Resource;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.util.HashUtil;
import com.aetheris.rag.util.MarkdownProcessor;
import com.aetheris.rag.util.PdfProcessor;
import com.aetheris.rag.exception.ConflictException;
import jakarta.annotation.PostConstruct;
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

  /** 切片大小（字符数） */
  @Value("${chunk.size:1000}")
  private int chunkSize;

  /** 切片重叠大小（字符数） */
  @Value("${chunk.overlap:200}")
  private int chunkOverlap;

  /** 分布式锁等待时间（秒） */
  private static final int LOCK_WAIT_TIME = 10;

  /** 分布式锁自动释放时间（秒） */
  private static final int LOCK_LEASE_TIME = 60;

  @Override
  @Transactional
  public Resource uploadResource(
      Path filePath, String title, String tags, String description, Long uploadedBy)
      throws Exception {
    log.info("开始上传资源: {}, 文件: {}", title, filePath);

    // 计算文件内容哈希
    String contentHash = calculateContentHash(filePath);
    log.debug("文件内容哈希: {}", contentHash);

    // 检查是否已存在（幂等性）
    Resource existingResource = resourceMapper.findByContentHash(contentHash);
    if (existingResource != null) {
      log.info("资源已存在（内容哈希重复）: {}", existingResource.getId());
      return existingResource;
    }

    // 获取分布式锁，防止并发重复上传
    String lockKey = "resource:upload:" + contentHash;
    RLock lock = redissonClient.getLock(lockKey);

    boolean lockAcquired = false;
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

      // 读取文件信息
      String fileName = filePath.getFileName().toString();
      String fileType = getFileType(fileName);
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
}
