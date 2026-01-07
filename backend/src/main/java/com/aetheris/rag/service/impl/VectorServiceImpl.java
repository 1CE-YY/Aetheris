/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
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
  private final ResourceMapper resourceMapper;
  private final EmbeddingGateway embeddingGateway;
  private final StringRedisTemplate redisTemplate;

  /** 向量维度（智谱 embedding-3，默认 2048 维） */
  @Value("${rag.vector.dimension:2048}")
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
      // 检查索引是否已存在（使用 FT._LIST 命令，避免 FT.INFO 的浮点数解析问题）
      log.debug("检查向量索引是否存在: {}", INDEX_NAME);

      Boolean indexExists = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
        try {
          // 使用 FT._LIST 命令列出所有索引，避免解析浮点数问题
          Object result = connection.execute("FT._LIST");
          if (result != null) {
            String indexList = result.toString();
            // 检查索引名是否在列表中
            return indexList.contains(INDEX_NAME);
          }
          return false;
        } catch (Exception e) {
          // 任何异常都认为索引不存在
          log.debug("检查索引失败，可能是索引不存在: {}", e.getMessage());
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
        // HNSW 参数：TYPE、DIM、DISTANCE_METRIC（共 6 个参数值，即 3 个键值对）
        Object result = connection.execute(
          "FT.CREATE",  // String 命令名
          INDEX_NAME.getBytes(),
          "ON".getBytes(), "HASH".getBytes(),
          "PREFIX".getBytes(), "1".getBytes(), "chunk:".getBytes(),
          "SCHEMA".getBytes(),
          "vector".getBytes(), "VECTOR".getBytes(), "HNSW".getBytes(), "6".getBytes(),
          "TYPE".getBytes(), "FLOAT32".getBytes(),
          "DIM".getBytes(), String.valueOf(vectorSize).getBytes(),
          "DISTANCE_METRIC".getBytes(), "COSINE".getBytes()
        );
        return result != null ? result.toString() : "OK";
      });

      log.info("向量索引创建成功: {}, result: {}", INDEX_NAME, createResult);
      indexInitialized = true;
    } catch (Exception e) {
      // 检查是否是索引已存在的错误（检查整个异常链）
      boolean indexAlreadyExists = false;
      Throwable cause = e;
      while (cause != null) {
        if (cause.getMessage() != null && cause.getMessage().contains("Index already exists")) {
          indexAlreadyExists = true;
          break;
        }
        cause = cause.getCause();
      }

      if (indexAlreadyExists) {
        log.info("向量索引已存在: {}", INDEX_NAME);
        indexInitialized = true;
      } else {
        log.error("初始化向量索引失败", e);
        throw new RuntimeException("向量索引初始化失败: " + e.getMessage(), e);
      }
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

    // 检查资源的所有切片是否都已向量化
    List<Chunk> allChunks = chunkMapper.findByResourceId(resourceId);
    boolean allVectorized = allChunks.stream().allMatch(Chunk::getVectorized);

    if (allVectorized) {
      resourceMapper.updateChunkStatus(resourceId, allChunks.size(), true);
      log.info("资源向量化完成: resourceId={}, chunkCount={}", resourceId, allChunks.size());
    } else {
      long vectorizedCount = allChunks.stream().filter(Chunk::getVectorized).count();
      log.warn("资源部分切片向量化失败: resourceId={}, 已向量化={}/{}", resourceId, vectorizedCount, allChunks.size());
    }
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

  @Override
  public void rebuildVectorIndex() {
    log.info("开始重建向量索引...");

    try {
      // 1. 删除旧索引
      log.info("删除旧索引...");
      redisTemplate.execute((RedisCallback<Object>) connection ->
          connection.execute("FT.DROP", INDEX_NAME.getBytes()));
      log.info("旧索引已删除");

      // 2. 重置初始化状态并创建新索引
      indexInitialized = false;
      log.info("创建新索引...");
      initializeVectorIndex();
      log.info("新索引已创建");

      // 3. 重新向量化所有资源
      log.info("开始重新向量化所有资源...");
      vectorizeAllUnvectorized();
      log.info("向量索引重建完成");

    } catch (Exception e) {
      log.error("重建向量索引失败", e);
      throw new RuntimeException("重建向量索引失败: " + e.getMessage(), e);
    }
  }
}
