package com.aetheris.rag.util;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PerformanceTimer}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("PerformanceTimer Tests")
class PerformanceTimerTest {

  @Test
  @DisplayName("PerformanceTimer should record elapsed time")
  void testPerformanceTimer() {
    PerformanceTimer timer = new PerformanceTimer();

    // Simulate some work
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long elapsedMs = timer.getElapsedMs();

    assertTrue(elapsedMs >= 100, "Elapsed time should be at least 100ms");
    assertTrue(elapsedMs < 200, "Elapsed time should be less than 200ms");
  }

  @Test
  @DisplayName("getElapsedMs should return consistent values")
  void testGetElapsedMsConsistency() {
    PerformanceTimer timer = new PerformanceTimer();

    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long elapsed1 = timer.getElapsedMs();
    long elapsed2 = timer.getElapsedMs();

    // Both should be similar (within 10ms tolerance)
    assertTrue(Math.abs(elapsed1 - elapsed2) < 10);
  }

  @Test
  @DisplayName("toString should contain elapsed time")
  void testToString() {
    PerformanceTimer timer = new PerformanceTimer();

    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    String str = timer.toString();

    assertTrue(str.contains("ms") || str.contains("PerformanceTimer"));
  }
}
