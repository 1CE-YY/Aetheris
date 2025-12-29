package com.aetheris.rag.gateway.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.aetheris.rag.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for {@link EmbeddingCache} using Testcontainers.
 *
 * <p>These tests use a real Redis container to verify caching behavior.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Testcontainers
@DisplayName("EmbeddingCache Integration Tests")
class EmbeddingCacheIntegrationTest {

  @Container
  private static final GenericContainer<?> redisContainer =
      new GenericContainer<>(DockerImageName.parse("redis/redis-stack-server:latest"))
          .withExposedPorts(6379)
          .withCommand("redis-server", "--save", "");

  private RedisTemplate<String, float[]> redisTemplate;
  private EmbeddingCache embeddingCache;

  @BeforeEach
  void setUp() {
    // Create Redis connection factory
    RedisConfig redisConfig = new RedisConfig();
    RedisConnectionFactory factory =
        redisConfig.redisConnectionFactory(
            redisContainer.getHost(), redisContainer.getFirstMappedPort(), "");

    // Create Redis template
    redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(factory);
    redisTemplate.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
    redisTemplate.setValueSerializer(
        new com.aetheris.rag.config.FloatArrayRedisSerializer());
    redisTemplate.setHashKeySerializer(
        new org.springframework.data.redis.serializer.StringRedisSerializer());
    redisTemplate.setHashValueSerializer(
        new com.aetheris.rag.config.FloatArrayRedisSerializer());
    redisTemplate.afterPropertiesSet();

    // Create EmbeddingCache
    embeddingCache = new EmbeddingCache(redisTemplate);
  }

  @AfterEach
  void tearDown() {
    // Clear all keys after each test
    redisTemplate.getConnectionFactory().getConnection().flushDb();
  }

  @Test
  @DisplayName("get and put should store and retrieve embedding")
  void testGetAndPut() {
    // Given
    String textHash = "test-hash-123";
    float[] embedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

    // When
    embeddingCache.put(textHash, embedding);
    float[] retrieved = embeddingCache.get(textHash);

    // Then
    assertNotNull(retrieved);
    assertArrayEquals(embedding, retrieved, 0.001f);
  }

  @Test
  @DisplayName("get should return null for non-existent key")
  void testGetNonExistent() {
    // Given
    String textHash = "non-existent-hash";

    // When
    float[] retrieved = embeddingCache.get(textHash);

    // Then
    assertNull(retrieved);
  }

  @Test
  @DisplayName("invalidate should remove existing embedding")
  void testInvalidate() {
    // Given
    String textHash = "test-hash-456";
    float[] embedding = {0.6f, 0.7f, 0.8f};
    embeddingCache.put(textHash, embedding);

    // When
    embeddingCache.invalidate(textHash);
    float[] retrieved = embeddingCache.get(textHash);

    // Then
    assertNull(retrieved);
  }

  @Test
  @DisplayName("exists should return true for existing key")
  void testExists() {
    // Given
    String textHash = "test-hash-789";
    float[] embedding = {0.9f, 1.0f};
    embeddingCache.put(textHash, embedding);

    // When
    boolean exists = embeddingCache.exists(textHash);

    // Then
    assertTrue(exists);
  }

  @Test
  @DisplayName("exists should return false for non-existent key")
  void testNotExists() {
    // Given
    String textHash = "non-existent-hash-2";

    // When
    boolean exists = embeddingCache.exists(textHash);

    // Then
    assertFalse(exists);
  }

  @Test
  @DisplayName("multiple puts should not interfere with each other")
  void testMultiplePuts() {
    // Given
    String hash1 = "hash-1";
    String hash2 = "hash-2";
    String hash3 = "hash-3";
    float[] embedding1 = {0.1f, 0.2f};
    float[] embedding2 = {0.3f, 0.4f};
    float[] embedding3 = {0.5f, 0.6f};

    // When
    embeddingCache.put(hash1, embedding1);
    embeddingCache.put(hash2, embedding2);
    embeddingCache.put(hash3, embedding3);

    // Then
    assertArrayEquals(embedding1, embeddingCache.get(hash1), 0.001f);
    assertArrayEquals(embedding2, embeddingCache.get(hash2), 0.001f);
    assertArrayEquals(embedding3, embeddingCache.get(hash3), 0.001f);
  }

  @Test
  @DisplayName("put should overwrite existing value")
  void testPutOverwrite() {
    // Given
    String textHash = "test-hash-999";
    float[] embedding1 = {0.1f, 0.2f};
    float[] embedding2 = {0.9f, 0.8f};

    // When
    embeddingCache.put(textHash, embedding1);
    embeddingCache.put(textHash, embedding2);
    float[] retrieved = embeddingCache.get(textHash);

    // Then
    assertArrayEquals(embedding2, retrieved, 0.001f);
  }
}
