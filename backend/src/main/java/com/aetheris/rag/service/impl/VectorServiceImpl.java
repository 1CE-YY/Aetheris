/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.gateway.ModelGateway;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.service.VectorService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 向量化服务实现类。
 *
 * <p>批量处理未向量化的切片，调用 EmbeddingGateway 获取向量，写入 Redis 向量索引。
 *
 * <p><strong>注意：</strong>当前 EmbeddingGateway 为 stub 实现，返回 dummy vectors。
 * 完整的向量化功能将在 Phase 5 实现 EmbeddingGateway 后自动生效。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

  private final ChunkMapper chunkMapper;
  private final ModelGateway modelGateway;
  private final StringRedisTemplate redisTemplate;

  /** 向量维度（智谱 embedding-v2） */
  @Value("${embedding.vectorSize:1024}")
  private int vectorSize;

  /** Redis 向量索引名称 */
  private static final String INDEX_NAME = "chunk_vector_index";

  /** 批量处理大小 */
  @Value("${vectorization.batchSize:10}")
  private int batchSize;

  /** 向量索引是否已初始化 */
  private boolean indexInitialized = false;

  @Override
  @PostConstruct
  public void initializeVectorIndex() {
    if (indexInitialized) {
      return;
    }

    try {
      // 索引检查将在 Phase 5 完整实现时执行
      // 当前 stub 实现不需要检查索引状态
      log.info("向量索引将在 Phase 5 完整实现时创建: {}", INDEX_NAME);
      indexInitialized = true;
    } catch (Exception e) {
      log.warn("初始化向量索引失败，将在首次向量化时重试", e);
    }
  }

  @Override
  public void vectorizeChunks(Long resourceId) {
    log.info("开始向量化资源切片: resourceId={}", resourceId);

    // 查询未向量化的切片
    List<Chunk> chunks = chunkMapper.findUnvectorizedByResourceId(resourceId);

    if (chunks.isEmpty()) {
      log.info("没有需要向量化的切片: resourceId={}", resourceId);
      return;
    }

    // 批量处理切片
    vectorizeBatch(chunks);
  }

  @Override
  public void vectorizeAllUnvectorized() {
    log.info("开始批量向量化所有未向量化的切片");

    List<Chunk> chunks = chunkMapper.findUnvectorized();
    log.info("找到 {} 个未向量化的切片", chunks.size());

    // 分批处理
    for (int i = 0; i < chunks.size(); i += batchSize) {
      int end = Math.min(i + batchSize, chunks.size());
      List<Chunk> batch = chunks.subList(i, end);
      vectorizeBatch(batch);
    }
  }

  /**
   * 批量向量化切片。
   *
   * @param chunks 切片列表
   */
  private void vectorizeBatch(List<Chunk> chunks) {
    log.debug("批量向量化 {} 个切片", chunks.size());

    List<Long> vectorizedIds = new ArrayList<>();

    for (Chunk chunk : chunks) {
      try {
        // 调用 EmbeddingGateway 获取向量（stub 返回 dummy vectors）
        float[] vector = modelGateway.embed(chunk.getChunkText());

        // 写入 Redis 向量索引（Phase 5 完整实现）
        writeVectorToRedis(chunk, vector);

        vectorizedIds.add(chunk.getId());
        log.debug("切片向量化成功: chunkId={}", chunk.getId());
      } catch (Exception e) {
        log.error("切片向量化失败: chunkId={}", chunk.getId(), e);
      }
    }

    // 批量更新向量化状态
    if (!vectorizedIds.isEmpty()) {
      chunkMapper.batchUpdateVectorized(vectorizedIds, true);
      log.info("批量更新向量化状态: {} 个切片", vectorizedIds.size());
    }
  }

  /**
   * 将向量写入 Redis。
   *
   * @param chunk 切片
   * @param vector 向量
   */
  private void writeVectorToRedis(Chunk chunk, float[] vector) {
    try {
      // Redis key: chunk:{chunkId}
      String key = "chunk:" + chunk.getId();

      // 准备数据
      Map<String, String> fields = new HashMap<>();
      fields.put("chunkId", chunk.getId().toString());
      fields.put("resourceId", chunk.getResourceId().toString());
      fields.put("chunkIndex", chunk.getChunkIndex().toString());
      fields.put("chunkText", chunk.getChunkText());

      // 将向量转换为字符串（Phase 5 完整实现时使用 Redis Vector 模块）
      fields.put("vector", vectorToString(vector));

      // 写入 Redis Hash
      redisTemplate.opsForHash().putAll(key, fields);

      // 设置过期时间（30天）
      redisTemplate.expire(key, Duration.ofDays(30));

      log.debug("向量已写入 Redis: chunkId={}", chunk.getId());
    } catch (Exception e) {
      log.error("写入 Redis 向量失败: chunkId={}", chunk.getId(), e);
    }
  }

  /**
   * 将向量转换为字符串。
   *
   * @param vector 向量
   * @return 向量字符串（逗号分隔）
   */
  private String vectorToString(float[] vector) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(vector[i]);
    }
    return sb.toString();
  }
}
