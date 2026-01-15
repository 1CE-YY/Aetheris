/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.mapper.ChunkMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 向量数据删除工具类。
 * <p>统一处理Redis向量数据和索引的删除。
 */
@Slf4j
public final class VectorDataDeletionUtil {

  private VectorDataDeletionUtil() {
  }

  /**
   * 删除指定资源的向量数据。
   */
  public static int deleteVectorDataByResourceIds(
      ChunkMapper chunkMapper,
      StringRedisTemplate redisTemplate,
      List<Long> resourceIds,
      String indexName) {

    try {
      // 查询所有切片
      List<Chunk> chunks = chunkMapper.findByResourceIds(resourceIds);
      if (chunks.isEmpty()) {
        return 0;
      }

      // 删除Redis键
      List<String> keys = chunks.stream()
          .map(chunk -> "chunk:" + chunk.getId())
          .toList();
      redisTemplate.delete(keys);

      // 从索引中删除记录
      for (Chunk chunk : chunks) {
        String docId = "chunk:" + chunk.getId();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
          connection.execute("FT.DEL", indexName.getBytes(), docId.getBytes());
          return null;
        });
      }

      log.info("已删除资源的向量数据和索引记录，共 {} 个切片", chunks.size());
      return chunks.size();

    } catch (Exception e) {
      log.warn("批量删除向量数据失败: resourceIds={}", resourceIds, e);
      return 0;
    }
  }

  /**
   * 删除指定切片的向量数据。
   */
  public static int deleteVectorDataByChunkIds(
      ChunkMapper chunkMapper,
      StringRedisTemplate redisTemplate,
      List<Long> chunkIds,
      String indexName) {

    try {
      // 查询切片信息
      List<Chunk> chunks = chunkMapper.findByIds(chunkIds);
      if (chunks.isEmpty()) {
        return 0;
      }

      // 删除Redis键
      List<String> keys = chunks.stream()
          .map(chunk -> "chunk:" + chunk.getId())
          .toList();
      redisTemplate.delete(keys);

      // 从索引中删除记录
      for (Chunk chunk : chunks) {
        String docId = "chunk:" + chunk.getId();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
          connection.execute("FT.DEL", indexName.getBytes(), docId.getBytes());
          return null;
        });
      }

      log.info("已删除切片的向量数据和索引记录，共 {} 个切片", chunks.size());
      return chunks.size();

    } catch (Exception e) {
      log.warn("批量删除切片向量数据失败: chunkIds={}", chunkIds, e);
      return 0;
    }
  }
}
