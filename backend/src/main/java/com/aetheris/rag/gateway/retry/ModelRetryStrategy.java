package com.aetheris.rag.gateway.retry;

import dev.langchain4j.exception.ModelException;
import dev.langchain4j.exception.ModelUnauthorizedException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry strategy for model API calls with exponential backoff.
 *
 * <p>This class implements a retry mechanism for handling transient failures when calling
 * external model APIs (e.g., Zhipu AI). It uses exponential backoff with jitter to avoid
 * overwhelming the API during outages.
 *
 * <p>Retry conditions:
 *
 * <ul>
 *   <li>HTTP 429 (Too Many Requests) - rate limit exceeded
 *   <li>HTTP 500 (Internal Server Error) - server-side error
 *   <li>HTTP 502 (Bad Gateway) - gateway error
 *   <li>HTTP 503 (Service Unavailable) - service temporarily unavailable
 *   <li>IOException - network connectivity issues
 * </ul>
 *
 * <p>Non-retryable errors (fail immediately):
 *
 * <ul>
 *   <li>HTTP 401 (Unauthorized) - invalid API key
 *   <li>HTTP 400 (Bad Request) - invalid request parameters
 *   <li>Other client errors (4xx except 429)
 * </ul>
 *
 * <p>Usage example:
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
public final class ModelRetryStrategy {

  private static final Logger log = LoggerFactory.getLogger(ModelRetryStrategy.class);

  private final int maxAttempts;
  private final Duration baseBackoff;
  private final double jitterFactor;

  /**
   * Creates a retry strategy with custom parameters.
   *
   * @param maxAttempts maximum number of retry attempts (must be >= 1)
   * @param baseBackoff base backoff duration between retries (must be positive)
   * @throws IllegalArgumentException if maxAttempts < 1 or baseBackoff is null/negative
   */
  public ModelRetryStrategy(int maxAttempts, Duration baseBackoff) {
    this(maxAttempts, baseBackoff, 0.1);
  }

  /**
   * Creates a retry strategy with custom parameters and jitter.
   *
   * @param maxAttempts maximum number of retry attempts (must be >= 1)
   * @param baseBackoff base backoff duration between retries (must be positive)
   * @param jitterFactor jitter factor to add randomness (0.0 to 1.0, default 0.1)
   * @throws IllegalArgumentException if parameters are invalid
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
   * Executes an operation with retry logic.
   *
   * <p>This method will retry the operation on transient failures using exponential backoff.
   * Non-retryable errors will fail immediately without retry.
   *
   * @param operation the operation to execute
   * @param <T> the return type of the operation
   * @return the result of the operation
   * @throws ModelException if all retry attempts fail or a non-retryable error occurs
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
   * Determines if an exception is retryable.
   *
   * @param e the exception to check
   * @return true if the exception is retryable, false otherwise
   */
  private boolean shouldRetry(Exception e) {
    // Check for specific status codes
    if (e instanceof ModelUnauthorizedException) {
      // 401 Unauthorized - don't retry (invalid API key)
      return false;
    }

    // Extract status code from exception message if available
    String message = e.getMessage();
    if (message != null) {
      // Check for HTTP status codes in message
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
   * Calculates backoff duration with exponential delay and jitter.
   *
   * @param attempt the current attempt number (1-based)
   * @return the backoff duration
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
   * Functional interface for retryable operations.
   *
   * @param <T> the return type of the operation
   */
  @FunctionalInterface
  public interface RetryableOperation<T> {

    /**
     * Executes the operation.
     *
     * @return the result
     * @throws Exception if the operation fails
     */
    T execute() throws Exception;
  }
}
