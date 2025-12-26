package com.aetheris.rag.gateway.retry;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.exception.ModelException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModelRetryStrategy}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("ModelRetryStrategy Tests")
class ModelRetryStrategyTest {

  @Test
  @DisplayName("executeWithRetry should succeed on first attempt")
  void testExecuteSuccessFirstAttempt() {
    ModelRetryStrategy strategy = new ModelRetryStrategy(3, Duration.ofMillis(100));

    String result = strategy.executeWithRetry(() -> "success");

    assertEquals("success", result);
  }

  @Test
  @DisplayName("executeWithRetry should retry on retryable exception")
  void testExecuteRetrySuccess() {
    ModelRetryStrategy strategy = new ModelRetryStrategy(3, Duration.ofMillis(100));
    AtomicInteger attempts = new AtomicInteger(0);

    String result =
        strategy.executeWithRetry(
            () -> {
              int attempt = attempts.incrementAndGet();
              if (attempt < 2) {
                throw new IOException("Temporary failure");
              }
              return "success";
            });

    assertEquals("success", result);
    assertEquals(2, attempts.get());
  }

  @Test
  @DisplayName("executeWithRetry should fail after max attempts")
  void testExecuteMaxAttemptsExceeded() {
    ModelRetryStrategy strategy = new ModelRetryStrategy(2, Duration.ofMillis(100));

    assertThrows(
        ModelException.class,
        () ->
            strategy.executeWithRetry(
                () -> {
                  throw new IOException("Persistent failure");
                }));
  }

  @Test
  @DisplayName("executeWithRetry should fail immediately for non-retryable error")
  void testExecuteNonRetryableError() {
    ModelRetryStrategy strategy = new ModelRetryStrategy(3, Duration.ofMillis(100));

    assertThrows(
        ModelException.class,
        () ->
            strategy.executeWithRetry(
                () -> {
                  throw new ModelException("Non-retryable error");
                }));
  }

  @Test
  @DisplayName("Constructor should throw exception for invalid maxAttempts")
  void testConstructorInvalidMaxAttempts() {
    assertThrows(IllegalArgumentException.class, () -> new ModelRetryStrategy(0, Duration.ofSeconds(1)));
  }

  @Test
  @DisplayName("Constructor should throw exception for null backoff")
  void testConstructorNullBackoff() {
    assertThrows(IllegalArgumentException.class, () -> new ModelRetryStrategy(3, null));
  }

  @Test
  @DisplayName("Constructor should throw exception for invalid jitter factor")
  void testConstructorInvalidJitter() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ModelRetryStrategy(3, Duration.ofSeconds(1), -0.1));

    assertThrows(
        IllegalArgumentException.class,
        () -> new ModelRetryStrategy(3, Duration.ofSeconds(1), 1.1));
  }
}
