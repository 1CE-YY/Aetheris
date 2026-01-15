/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 分布式锁工具类。
 *
 * <p>提供统一的分布式锁管理，简化分布式锁的使用。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
@Slf4j
public final class DistributedLockUtil {

  private DistributedLockUtil() {
  }

  /**
   * 使用分布式锁执行操作。
   *
   * @param redissonClient Redis客户端
   * @param lockKey 锁键
   * @param waitTime 等待时间（秒）
   * @param leaseTime 持有时间（秒）
   * @param action 要执行的操作
   * @param <T> 返回类型
   * @return 操作结果
   * @throws Exception 如果执行失败
   */
  public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey,
      long waitTime, long leaseTime, LockAction<T> action) throws Exception {

    RLock lock = redissonClient.getLock(lockKey);
    boolean lockAcquired = false;

    try {
      lockAcquired = lock.tryLock(waitTime, leaseTime, java.util.concurrent.TimeUnit.SECONDS);

      if (!lockAcquired) {
        log.warn("获取分布式锁失败: {}", lockKey);
        throw new java.util.concurrent.TimeoutException("获取分布式锁超时: " + lockKey);
      }

      log.debug("获取分布式锁成功: {}", lockKey);
      return action.execute();

    } finally {
      if (lockAcquired) {
        lock.unlock();
        log.debug("释放分布式锁: {}", lockKey);
      }
    }
  }


  /**
   * 使用分布式锁执行操作（使用Supplier）。
   *
   * @param redissonClient Redis客户端
   * @param lockKey 锁键
   * @param waitTime 等待时间（秒）
   * @param leaseTime 持有时间（秒）
   * @param supplier 要执行的操作
   * @param <T> 返回类型
   * @return 操作结果
   */
  public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey,
      long waitTime, long leaseTime, java.util.function.Supplier<T> supplier) {
    try {
      return executeWithLock(redissonClient, lockKey, waitTime, leaseTime,
          (LockAction<T>) () -> supplier.get());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 使用分布式锁执行操作（简化版，使用Supplier）。
   *
   * @param redissonClient Redis客户端
   * @param lockKey 锁键
   * @param supplier 要执行的操作
   * @param <T> 返回类型
   * @return 操作结果
   */
  public static <T> T executeWithLock(RedissonClient redissonClient, String lockKey,
      java.util.function.Supplier<T> supplier) {
    return executeWithLock(redissonClient, lockKey, 10, 60, supplier);
 }

  /**
   * 锁操作函数式接口。
   *
   * @param <T> 返回类型
   */
  @FunctionalInterface
  public interface LockAction<T> {

    /**
     * 执行操作。
     *
     * @return 操作结果
     * @throws Exception 如果执行失败
     */
    T execute() throws Exception;
  }
}
