/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类。
 *
 * <p>配置 Redisson 客户端，提供分布式锁、分布式集合等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Slf4j
@Configuration
public class RedissonConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Value("${spring.data.redis.database:0}")
  private int redisDatabase;

  /**
   * 创建 RedissonClient Bean。
   *
   * <p>使用单节点模式连接 Redis，适用于开发和生产环境。
   *
   * @return RedissonClient 实例
   */
  @Bean(destroyMethod = "shutdown")
  public RedissonClient redissonClient() {
    log.info("初始化 Redisson 客户端: {}:{}", redisHost, redisPort);

    Config config = new Config();
    String address = "redis://" + redisHost + ":" + redisPort;

    // 使用单节点配置
    config
        .useSingleServer()
        .setAddress(address)
        .setDatabase(redisDatabase)
        .setConnectionPoolSize(10)
        .setConnectionMinimumIdleSize(2)
        .setTimeout(5000)
        .setRetryAttempts(3)
        .setRetryInterval(1500);

    // 设置密码（如果配置了密码）
    if (redisPassword != null && !redisPassword.isEmpty()) {
      config.useSingleServer().setPassword(redisPassword);
    }

    RedissonClient redissonClient = Redisson.create(config);
    log.info("Redisson 客户端初始化成功");

    return redissonClient;
  }
}
