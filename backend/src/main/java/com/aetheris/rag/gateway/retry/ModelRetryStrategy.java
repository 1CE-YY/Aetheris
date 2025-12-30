package com.aetheris.rag.gateway.retry;

import com.aetheris.rag.gateway.ModelException;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * 模型 API 调用的重试策略，使用指数退避。
 *
 * <p>此类实现了重试机制，用于处理调用外部模型 API（例如智谱 AI）时的瞬态故障。
 * 它使用带抖动的指数退避来避免在故障期间压垮 API。
 *
 * <p>重试条件：
 *
 * <ul>
 *   <li>HTTP 429（请求过多）- 超过速率限制
 *   <li>HTTP 500（内部服务器错误）- 服务器端错误
 *   <li>HTTP 502（网关错误）- 网关错误
 *   <li>HTTP 503（服务不可用）- 服务暂时不可用
 *   <li>IOException - 网络连接问题
 * </ul>
 *
 * <p>不可重试的错误（立即失败）：
 *
 * <ul>
 *   <li>HTTP 401（未授权）- 无效的 API 密钥
 *   <li>HTTP 400（错误请求）- 无效的请求参数
 *   <li>其他客户端错误（4xx，除了 429）
 * </ul>
 *
 * <p>用法示例：
 *
 * <pre>{@code
 * ModelRetryStrategy retryStrategy = new ModelRetryStrategy(3, Duration.ofSeconds(1));
 * String result = retryStrategy.executeWithRetry(() -> modelAPI.generate(prompt));
 * }</pre>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Slf4j
public final class ModelRetryStrategy {

  private final int maxAttempts;
  private final Duration baseBackoff;
  private final double jitterFactor;

  /**
   * 使用自定义参数创建重试策略。
   *
   * @param maxAttempts 最大重试次数（必须 >= 1）
   * @param baseBackoff 重试之间的基本退避持续时间（必须为正数）
   * @throws IllegalArgumentException 如果 maxAttempts < 1 或 baseBackoff 为 null/负数
   */
  public ModelRetryStrategy(int maxAttempts, Duration baseBackoff) {
    this(maxAttempts, baseBackoff, 0.1);
  }

  /**
   * 使用自定义参数和抖动创建重试策略。
   *
   * @param maxAttempts 最大重试次数（必须 >= 1）
   * @param baseBackoff 重试之间的基本退避持续时间（必须为正数）
   * @param jitterFactor 抖动因子以添加随机性（0.0 到 1.0，默认 0.1）
   * @throws IllegalArgumentException 如果参数无效
   */
  public ModelRetryStrategy(int maxAttempts, Duration baseBackoff, double jitterFactor) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >= 1, got: " + maxAttempts);
    }
    if (baseBackoff == null || baseBackoff.isNegative() || baseBackoff.isZero()) {
      throw new IllegalArgumentException(
          "baseBackoff must be positive, got: " + baseBackoff);
    }
    if (jitterFactor < 0.0 || jitterFactor > 1.0) {
      throw new IllegalArgumentException(
          "jitterFactor must be between 0.0 and 1.0, got: " + jitterFactor);
    }

    this.maxAttempts = maxAttempts;
    this.baseBackoff = baseBackoff;
    this.jitterFactor = jitterFactor;
  }

  /**
   * 使用重试逻辑执行操作。
   *
   * <p>此方法将使用指数退避在瞬态故障上重试操作。
   * 不可重试的错误将立即失败而不会重试。
   *
   * @param operation 要执行的操作
   * @param <T> 操作的返回类型
   * @return 操作的结果
   * @throws ModelException 如果所有重试尝试失败或发生不可重试的错误
   */
  public <T> T executeWithRetry(RetryableOperation<T> operation) {
    int attempts = 0;
    Exception lastException = null;

    while (attempts < maxAttempts) {
      attempts++;

      try {
        return operation.execute();
      } catch (Exception e) {
        lastException = e;

        // Check if error is retryable
        if (!shouldRetry(e)) {
          log.error("Non-retryable error occurred, failing immediately: {}", e.getClass().getName());
          throw new ModelException("Non-retryable error: " + e.getMessage(), e);
        }

        // Check if we should stop retrying
        if (attempts >= maxAttempts) {
          break;
        }

        // Calculate backoff with exponential delay and jitter
        Duration backoff = calculateBackoff(attempts);

        log.warn(
            "Retryable error occurred (attempt {}/{}), retrying in {}: {}",
            attempts,
            maxAttempts,
            backoff,
            e.getClass().getSimpleName());

        // Sleep before retry
        try {
          Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new ModelException("Retry interrupted", ie);
        }
      }
    }

    // All attempts failed
    log.error("Operation failed after {} attempts", attempts);
    throw new ModelException(
        "Operation failed after " + attempts + " attempts", lastException);
  }

  /**
   * 确定异常是否可重试。
   *
   * @param e 要检查的异常
   * @return 如果异常可重试返回 true，否则返回 false
   */
  private boolean shouldRetry(Exception e) {
    // Extract status code from exception message if available
    String message = e.getMessage();
    if (message != null) {
      // Check for HTTP status codes in message
      if (message.contains("401")) {
        // 401 Unauthorized - don't retry (invalid API key)
        return false;
      }
      if (message.contains("429")) {
        // 429 Too Many Requests - retry
        return true;
      }
      if (message.contains("500") || message.contains("502") || message.contains("503")) {
        // 5xx server errors - retry
        return true;
      }
      if (message.contains("400") || message.contains("401") || message.contains("403")) {
        // 4xx client errors (except 429) - don't retry
        return false;
      }
    }

    // IOException is typically retryable (network issues)
    if (e instanceof java.io.IOException) {
      return true;
    }

    // Default: don't retry for unknown exceptions
    return false;
  }

  /**
   * 计算具有指数延迟和抖动的退避持续时间。
   *
   * @param attempt 当前尝试次数（从 1 开始）
   * @return 退避持续时间
   */
  private Duration calculateBackoff(int attempt) {
    // Exponential backoff: base * 2^(attempt-1)
    long baseDelayMs = baseBackoff.toMillis();
    long exponentialDelayMs = baseDelayMs * (1L << (attempt - 1));

    // Add jitter to avoid thundering herd
    double jitter = (Math.random() - 0.5) * 2 * jitterFactor;
    long jitterMs = (long) (exponentialDelayMs * jitter);

    long finalDelayMs = exponentialDelayMs + jitterMs;

    // Cap at 60 seconds
    finalDelayMs = Math.min(finalDelayMs, Duration.ofSeconds(60).toMillis());

    return Duration.ofMillis(finalDelayMs);
  }

  /**
   * 可重试操作的函数式接口。
   *
   * @param <T> 操作的返回类型
   */
  @FunctionalInterface
  public interface RetryableOperation<T> {

    /**
     * 执行操作。
     *
     * @return 结果
     * @throws Exception 如果操作失败
     */
    T execute() throws Exception;
  }
}
