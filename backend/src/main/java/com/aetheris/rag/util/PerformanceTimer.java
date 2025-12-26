package com.aetheris.rag.util;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for tracking performance metrics with stage-level timing.
 *
 * <p>This class provides a simple way to measure elapsed time for different stages of a
 * request processing pipeline (e.g., parsing, embedding, retrieval, generation).
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * PerformanceTimer timer = new PerformanceTimer();
 *
 * // Stage 1: Parsing
 * timer.recordStage("parsing");
 * // ... do parsing work ...
 * timer.endStage();
 *
 * // Stage 2: Embedding
 * timer.recordStage("embedding");
 * // ... do embedding work ...
 * timer.endStage();
 *
 * log.info("Total time: {} ms, Stages: {}", timer.getElapsedMs(), timer.getStages());
 * }</pre>
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public class PerformanceTimer {

  private static final Logger log = LoggerFactory.getLogger(PerformanceTimer.class);

  private final Instant startTime;
  private final Map<String, Long> stages;
  private String currentStage;
  private Instant stageStartTime;

  /**
   * Creates a new performance timer.
   */
  public PerformanceTimer() {
    this.startTime = Instant.now();
    this.stages = new LinkedHashMap<>();
  }

  /**
   * Gets the total elapsed time in milliseconds.
   *
   * @return the elapsed time in milliseconds
   */
  public long getElapsedMs() {
    return Duration.between(startTime, Instant.now()).toMillis();
  }

  /**
   * Starts recording a new stage.
   *
   * @param stageName the name of the stage
   */
  public void recordStage(String stageName) {
    if (currentStage != null) {
      log.warn("Previous stage '{}' was not ended before starting new stage '{}'", currentStage, stageName);
      endStage();
    }

    this.currentStage = stageName;
    this.stageStartTime = Instant.now();
  }

  /**
   * Ends the current stage and records its duration.
   */
  public void endStage() {
    if (currentStage == null) {
      log.warn("No stage is currently being recorded");
      return;
    }

    Instant stageEndTime = Instant.now();
    long duration = Duration.between(stageStartTime, stageEndTime).toMillis();
    stages.put(currentStage, duration);

    log.debug("Stage '{}' completed in {} ms", currentStage, duration);

    currentStage = null;
    stageStartTime = null;
  }

  /**
   * Gets the recorded stages and their durations.
   *
   * @return a map of stage names to durations in milliseconds
   */
  public Map<String, Long> getStages() {
    return new LinkedHashMap<>(stages);
  }

  /**
   * Gets the duration of a specific stage.
   *
   * @param stageName the stage name
   * @return the duration in milliseconds, or null if stage not found
   */
  public Long getStageDuration(String stageName) {
    return stages.get(stageName);
  }

  @Override
  public String toString() {
    return "PerformanceTimer{"
        + "totalTime="
        + getElapsedMs()
        + "ms"
        + ", stages="
        + stages
        + '}';
  }
}
