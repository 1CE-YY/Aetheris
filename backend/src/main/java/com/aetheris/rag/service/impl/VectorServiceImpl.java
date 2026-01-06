/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.service.VectorService;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.RedisServerCommands;
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
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

  private final ChunkMapper chunkMapper;
  private final EmbeddingGateway embeddingGateway;
  private final StringRedisTemplate redisTemplate;

  /** 向量维度（智谱 embedding-v2） */
  @Value("${rag.vector.dimension:1024}")
  private int vectorSize;

  /** Redis 向量索引名称 */
  private static final String INDEX_NAME = "chunk_vector_index";

  /** 批量处理大小 */
  @Value("${rag.vectorization.batchSize:10}")
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
      // 检查索引是否已存在
      log.debug("检查向量索引是否存在: {}", INDEX_NAME);

      // 使用 execute 方法执行 FT.INFO 命令
      Boolean indexExists = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
        try {
          // execute(String command, byte[]... args)
          Object result = connection.execute("FT.INFO", INDEX_NAME.getBytes());
          return result != null;
        } catch (Exception e) {
          // 索引不存在会抛出异常
          log.debug("检查索引失败，可能是索引不存在", e);
          return false;
        }
      });

      if (indexExists) {
        log.info("向量索引已存在: {}", INDEX_NAME);
        indexInitialized = true;
        return;
      }

      // 创建向量索引
      log.info("创建向量索引: {}", INDEX_NAME);

      // 执行 FT.CREATE 命令
      String createResult = redisTemplate.execute((RedisCallback<String>) connection -> {
        // FT.CREATE index_name ON HASH PREFIX 1 chunk: SCHEMA ...
        // execute(String command, byte[]... args) - 第一个参数是String命令名
        Object result = connection.execute(
          "FT.CREATE",  // String 命令名
          INDEX_NAME.getBytes(),
          "ON".getBytes(), "HASH".getBytes(),
          "PREFIX".getBytes(), "1".getBytes(), "chunk:".getBytes(),
          "SCHEMA".getBytes(),
          "vector".getBytes(), "VECTOR".getBytes(), "HNSW".getBytes(), "6".getBytes(),
          "TYPE".getBytes(), "FLOAT32".getBytes(),
          "DIM".getBytes(), String.valueOf(vectorSize).getBytes(),
          "DISTANCE_METRIC".getBytes(), "COSINE".getBytes(),
          "initial_size".getBytes(), "1000".getBytes()
        );
        return result != null ? result.toString() : "OK";
      });

      log.info("向量索引创建成功: {}, result: {}", INDEX_NAME, createResult);
      indexInitialized = true;
    } catch (Exception e) {
      log.error("初始化向量索引失败", e);
      throw new RuntimeException("向量索引初始化失败: " + e.getMessage(), e);
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
        // 调用 EmbeddingGateway 获取向量
        float[] vector = embeddingGateway.embed(chunk.getChunkText());

        // 写入 Redis 向量索引
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

      // 将向量转换为字符串（用于 RediSearch Vector）
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
