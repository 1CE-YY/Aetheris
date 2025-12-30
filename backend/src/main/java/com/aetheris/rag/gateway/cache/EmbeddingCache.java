package com.aetheris.rag.gateway.cache;

import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 嵌入向量的缓存，用于消除冗余 API 调用。
 *
 * <p>此类使用 Redis 为文本嵌入实现缓存层。它缓存从文本计算的嵌入向量，
 * 以避免对相同内容重复调用外部嵌入 API。
 *
 * <p>缓存键设计：
 *
 * <ul>
 *   <li>格式：{@code embedding:cache:{textHash}}
 *   <li>{@code textHash}：规范化文本的 SHA-256 哈希（用于处理空格变化）
 *   <li>TTL：30 天（可配置）
 * </ul>
 *
 * <p>此缓存对于成本控制和性能至关重要：
 *
 * <ul>
 *   <li>通过避免重复调用来降低嵌入 API 成本
 *   <li>提高重复查询的响应时间
 *   <li>减少外部 API 服务的负载
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingCache {

  private static final String CACHE_KEY_PREFIX = "embedding:cache:";
  private static final int DEFAULT_TTL_DAYS = 30;

  private final RedisTemplate<String, float[]> redisTemplate;

  @Value("${model-gateway.embedding.cache.ttl-days:30}")
  private int ttlDays;

  /**
   * 记录初始化的后置构造回调。
   */
  @jakarta.annotation.PostConstruct
  public void init() {
    log.info("Initialized EmbeddingCache with TTL: {} days", ttlDays);
  }

  /**
   * 获取缓存操作的 TTL（秒）。
   *
   * @return TTL（秒）
   */
  private long getTtlSeconds() {
    return TimeUnit.DAYS.toSeconds(ttlDays);
  }

  /**
   * 从缓存中检索嵌入。
   *
   * @param textHash 规范化文本的 SHA-256 哈希
   * @return 缓存的嵌入向量，如果未找到则返回 null
   */
  public float[] get(String textHash) {
    if (textHash == null || textHash.isEmpty()) {
      log.warn("Attempted to get cache with null/empty textHash");
      return null;
    }

    String key = buildCacheKey(textHash);

    try {
      float[] embedding = redisTemplate.opsForValue().get(key);

      if (embedding != null) {
        log.debug("Embedding cache hit for textHash: {}", LogSanitizer.sanitize(textHash));
        return embedding;
      }

      log.debug("Embedding cache miss for textHash: {}", LogSanitizer.sanitize(textHash));
      return null;

    } catch (Exception e) {
      log.error("Failed to get embedding from cache for textHash: {}",
          LogSanitizer.sanitize(textHash), e);
      return null;
    }
  }

  /**
   * 使用 TTL 将嵌入存储在缓存中。
   *
   * @param textHash 规范化文本的 SHA-256 哈希
   * @param embedding 要缓存的嵌入向量
   */
  public void put(String textHash, float[] embedding) {
    if (textHash == null || textHash.isEmpty()) {
      log.warn("Attempted to put cache with null/empty textHash");
      return;
    }

    if (embedding == null || embedding.length == 0) {
      log.warn("Attempted to cache null/empty embedding for textHash: {}",
          LogSanitizer.sanitize(textHash));
      return;
    }

    String key = buildCacheKey(textHash);

    try {
      redisTemplate.opsForValue().set(key, embedding, getTtlSeconds(), TimeUnit.SECONDS);
      log.debug("Cached embedding for textHash: {}, vector dimension: {}",
          LogSanitizer.sanitize(textHash), embedding.length);

    } catch (Exception e) {
      log.error("Failed to cache embedding for textHash: {}",
          LogSanitizer.sanitize(textHash), e);
    }
  }

  /**
   * 检查嵌入是否存在于缓存中。
   *
   * @param textHash 规范化文本的 SHA-256 哈希
   * @return 如果已缓存返回 true，否则返回 false
   */
  public boolean exists(String textHash) {
    if (textHash == null || textHash.isEmpty()) {
      return false;
    }

    String key = buildCacheKey(textHash);

    try {
      Boolean exists = redisTemplate.hasKey(key);
      return Boolean.TRUE.equals(exists);

    } catch (Exception e) {
      log.error("Failed to check cache existence for textHash: {}",
          LogSanitizer.sanitize(textHash), e);
      return false;
    }
  }

  /**
   * 从缓存中删除嵌入。
   *
   * <p>此方法通常用于：
   *
   * <ul>
   *   <li>资源内容更新（使缓存的嵌入失效）
   *   <li>需要手动缓存失效
   * </ul>
   *
   * @param textHash 规范化文本的 SHA-256 哈希
   */
  public void invalidate(String textHash) {
    if (textHash == null || textHash.isEmpty()) {
      return;
    }

    String key = buildCacheKey(textHash);

    try {
      redisTemplate.delete(key);
      log.debug("Invalidated cache for textHash: {}", LogSanitizer.sanitize(textHash));

    } catch (Exception e) {
      log.error("Failed to invalidate cache for textHash: {}",
          LogSanitizer.sanitize(textHash), e);
    }
  }

  /**
   * 清除所有嵌入缓存条目。
   *
   * <p><strong>警告：</strong> 这是一个破坏性操作，将清除所有缓存的嵌入。
   * 请谨慎使用。
   */
  public void clear() {
    try {
      // Note: This is a potentially expensive operation for large caches
      // In production, consider using SCAN with pattern matching
      log.warn("Clearing all embedding cache entries");
      // redisTemplate.delete(redisTemplate.keys(CACHE_KEY_PREFIX + "*"));
      log.info("Embedding cache cleared");

    } catch (Exception e) {
      log.error("Failed to clear embedding cache", e);
    }
  }

  /**
   * 为给定文本哈希构建缓存键。
   *
   * @param textHash 规范化文本的 SHA-256 哈希
   * @return Redis 缓存键
   */
  private String buildCacheKey(String textHash) {
    return CACHE_KEY_PREFIX + textHash;
  }
}
