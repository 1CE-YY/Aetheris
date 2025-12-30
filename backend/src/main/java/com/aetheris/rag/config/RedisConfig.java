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
 * 用于缓存和向量存储的 Redis 配置。
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
   * 用于缓存对象的通用 RedisTemplate。
   *
   * <p>使用 Jackson JSON 序列化器处理值，适用于大多数缓存场景。
   *
   * @param factory Redis 连接工厂
   * @return 为通用用途配置的 RedisTemplate
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
   * 用于嵌入向量的专用 RedisTemplate。
   *
   * <p>使用自定义序列化器处理 float[] 数组，以确保正确的序列化。
   *
   * @param factory Redis 连接工厂
   * @return 为嵌入向量配置的 RedisTemplate
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
