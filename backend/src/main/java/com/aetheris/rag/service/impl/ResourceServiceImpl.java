/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.service.ResourceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源服务实现类。
 *
 * <p>实现资源CRUD、元数据管理等功能。
 * 文档处理和向量化已移至DocumentService和ProcessingService。
 *
 * @author Aetheris Team
 * @version 2.0.0
 * @since 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

  private final ResourceMapper resourceMapper;
  private final ChunkMapper chunkMapper;
  private final RedissonClient redissonClient;
  private final StringRedisTemplate redisTemplate;

  /** 上传目录 */
  @Value("${upload.dir:uploads}")
  private String uploadDir;

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
    return (long) resourceMapper.count();
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
  public void updateVectorizationStatus(Long resourceId, boolean vectorized) {
    // 委托给updateChunkVectorizationStatus，自动查询chunkCount
    updateChunkVectorizationStatus(resourceId, null, vectorized);
  }

  @Override
  @Transactional
  public void updateChunkVectorizationStatus(Long resourceId, Integer chunkCount, boolean vectorized) {
    // 如果chunkCount为null，从数据库查询
    if (chunkCount == null) {
      Resource resource = resourceMapper.findById(resourceId);
      if (resource == null) {
        return;
      }
      chunkCount = resource.getChunkCount();
    }

    resourceMapper.updateChunkStatus(resourceId, chunkCount, vectorized);
    log.debug("资源切片状态已更新: resourceId={}, chunkCount={}, vectorized={}",
        resourceId, chunkCount, vectorized);
  }
}
