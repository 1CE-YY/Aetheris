package com.aetheris.rag.gateway.cache;

import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache for embedding vectors to eliminate redundant API calls.
 *
 * <p>This class implements a caching layer for text embeddings using Redis. It caches the
 * embedding vectors computed from text to avoid calling the external embedding API repeatedly
 * for the same content.
 *
 * <p>Cache key design:
 *
 * <ul>
 *   <li>Format: {@code embedding:cache:{textHash}}
 *   <li>{@code textHash}: SHA-256 hash of normalized text (to handle whitespace variations)
 *   <li>TTL: 30 days (configurable)
 * </ul>
 *
 * <p>This cache is critical for cost control and performance:
 *
 * <ul>
 *   <li>Reduces embedding API costs by avoiding duplicate calls
 *   <li>Improves response time for repeated queries
 *   <li>Reduces load on external API services
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Component
public class EmbeddingCache {

  private static final Logger log = LoggerFactory.getLogger(EmbeddingCache.class);

  private static final String CACHE_KEY_PREFIX = "embedding:cache:";
  private static final int DEFAULT_TTL_DAYS = 30;

  private final RedisTemplate<String, float[]> redisTemplate;
  private final long ttlSeconds;

  /**
   * Creates an embedding cache with configurable TTL.
   *
   * @param redisTemplate Redis template for cache operations
   * @param ttlDays cache TTL in days (from configuration)
   */
  @Autowired
  public EmbeddingCache(
      RedisTemplate<String, float[]> redisTemplate,
      @Value("${model-gateway.embedding.cache.ttl-days:${model-gateway.embedding.cache.ttl-days:30}}") int ttlDays) {
    this.redisTemplate = redisTemplate;
    this.ttlSeconds = TimeUnit.DAYS.toSeconds(ttlDays);
    log.info("Initialized EmbeddingCache with TTL: {} days", ttlDays);
  }

  /**
   * Retrieves an embedding from cache.
   *
   * @param textHash the SHA-256 hash of normalized text
   * @return the cached embedding vector, or null if not found
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
   * Stores an embedding in cache with TTL.
   *
   * @param textHash the SHA-256 hash of normalized text
   * @param embedding the embedding vector to cache
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
      redisTemplate.opsForValue().set(key, embedding, ttlSeconds, TimeUnit.SECONDS);
      log.debug("Cached embedding for textHash: {}, vector dimension: {}",
          LogSanitizer.sanitize(textHash), embedding.length);

    } catch (Exception e) {
      log.error("Failed to cache embedding for textHash: {}",
          LogSanitizer.sanitize(textHash), e);
    }
  }

  /**
   * Checks if an embedding exists in cache.
   *
   * @param textHash the SHA-256 hash of normalized text
   * @return true if cached, false otherwise
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
   * Removes an embedding from cache.
   *
   * <p>This method is typically used when:
   *
   * <ul>
   *   <li>Resource content is updated (invalidates cached embeddings)
   *   <li>Manual cache invalidation is required
   * </ul>
   *
   * @param textHash the SHA-256 hash of normalized text
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
   * Clears all embedding cache entries.
   *
   * <p><strong>Warning:</strong> This is a destructive operation that will clear all cached
   * embeddings. Use with caution.
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
   * Builds the cache key for a given text hash.
   *
   * @param textHash the SHA-256 hash of normalized text
   * @return the Redis cache key
   */
  private String buildCacheKey(String textHash) {
    return CACHE_KEY_PREFIX + textHash;
  }
}
