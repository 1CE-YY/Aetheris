package com.aetheris.rag.util;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于跟踪性能指标的工具类，支持阶段级别的计时。
 *
 * <p>此类提供了一种简单的方法来测量请求处理管道的不同阶段（例如，解析、嵌入、检索、生成）的经过时间。
 *
 * <p>用法示例：
 *
 * <pre>{@code
 * PerformanceTimer timer = new PerformanceTimer();
 *
 * // 阶段 1：解析
 * timer.recordStage("parsing");
 * // ... 执行解析工作 ...
 * timer.endStage();
 *
 * // 阶段 2：嵌入
 * timer.recordStage("embedding");
 * // ... 执行嵌入工作 ...
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
   * 创建新的性能计时器。
   */
  public PerformanceTimer() {
    this.startTime = Instant.now();
    this.stages = new LinkedHashMap<>();
  }

  /**
   * 获取总经过时间（毫秒）。
   *
   * @return 经过的时间（毫秒）
   */
  public long getElapsedMs() {
    return Duration.between(startTime, Instant.now()).toMillis();
  }

  /**
   * 开始记录新阶段。
   *
   * @param stageName 阶段名称
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
   * 结束当前阶段并记录其持续时间。
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
   * 获取记录的阶段及其持续时间。
   *
   * @return 阶段名称到持续时间的映射（毫秒）
   */
  public Map<String, Long> getStages() {
    return new LinkedHashMap<>(stages);
  }

  /**
   * 获取特定阶段的持续时间。
   *
   * @param stageName 阶段名称
   * @return 持续时间（毫秒），如果未找到阶段则返回 null
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
