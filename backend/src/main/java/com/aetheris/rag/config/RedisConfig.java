package com.aetheris.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for caching and vector storage.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Configuration
public class RedisConfig {

  @Bean
  public RedisConnectionFactory redisConnectionFactory(
      @Value("${spring.data.redis.host}") String host,
      @Value("${spring.data.redis.port}") int port,
      @Value("${spring.data.redis.password:}") String password) {

    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(host);
    config.setPort(port);

    if (password != null && !password.isEmpty()) {
      config.setPassword(password);
    }

    return new LettuceConnectionFactory(config);
  }

  /**
   * General-purpose RedisTemplate for caching objects.
   *
   * <p>Uses Jackson JSON serializer for values, suitable for most caching scenarios.
   *
   * @param factory Redis connection factory
   * @return RedisTemplate configured for general use
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }

  /**
   * Specialized RedisTemplate for embedding vectors.
   *
   * <p>Uses custom serializer for float[] arrays to ensure proper serialization.
   *
   * @param factory Redis connection factory
   * @return RedisTemplate configured for embedding vectors
   */
  @Bean
  public RedisTemplate<String, float[]> embeddingRedisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, float[]> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new FloatArrayRedisSerializer());
    template.setHashValueSerializer(new FloatArrayRedisSerializer());
    return template;
  }
}
