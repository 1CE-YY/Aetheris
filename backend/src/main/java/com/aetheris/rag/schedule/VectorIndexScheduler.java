/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.schedule;

import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.service.impl.ResourceServiceImpl;
import com.aetheris.rag.service.impl.VectorServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 向量索引定时任务。
 *
 * <p>每周日凌晨 3 点全量重建所有资源。
 *
 * @author Aetheris Team
 * @version 2.0.0
 * @since 2025-01-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorIndexScheduler {

  private final VectorService vectorService;
  private final ResourceService resourceService;

  /**
   * 定时全量重建所有资源。
   *
   * <p>每周日凌晨 3 点执行一次。
   * <p>执行流程：
   * <ol>
   *   <li>删除所有旧切片，重新生成新切片</li>
   *   <li>删除 Redis 缓存、向量、索引</li>
   *   <li>创建新的向量索引</li>
   *   <li>批量向量化所有新切片</li>
   * </ol>
   */
  @Scheduled(cron = "0 0 3 ? * SUN")
  public void rebuildAllResourcesWeekly() {
    log.info("========== 定时任务：开始全量重建 ==========");
    long startTime = System.currentTimeMillis();

    try {
      // 使用安全配置（跳过错误继续执行）
      RebuildConfig config = RebuildConfig.builder()
          .dropIndex(true)
          .rechunk(true)
          .batchSize(50)
          .maxRetries(2)
          .skipOnError(true)  // 定时任务跳过错误继续执行
          .enableConcurrency(true)
          .skipVectorization(false)  // 修改：不跳过向量化
          .build();

      // ✅ 步骤 1: 重建所有资源（重新切片）
      RebuildResult resourceResult = vectorService.rebuildAllResourcesWithConfig(config);

      // ✅ 步骤 2: 重建向量索引（删除索引和向量数据，统一批量向量化）
      VectorServiceImpl vectorServiceImpl = (VectorServiceImpl) vectorService;
      RebuildConfig indexConfig = RebuildConfig.builder()
          .dropIndex(true)
          .batchSize(config.getBatchSize())
          .maxRetries(config.getMaxRetries())
          .skipOnError(config.isSkipOnError())
          .enableConcurrency(config.isEnableConcurrency())
          .build();
      RebuildResult indexResult = vectorServiceImpl.rebuildVectorIndex(indexConfig);

      // 合并结果
      RebuildResult finalResult = RebuildResult.builder()
          .successCount(resourceResult.getSuccessCount())
          .failureCount(resourceResult.getFailureCount())
          .totalChunks(resourceResult.getTotalChunks())
          .duration(resourceResult.getDuration() + indexResult.getDuration())
          .failedResourceIds(resourceResult.getFailedResourceIds())
          .build();

      long duration = System.currentTimeMillis() - startTime;

      log.info("========== 定时任务：全量重建完成，耗时 {} ms ==========", duration);
      log.info("成功: {}, 失败: {}, 总切片: {}",
          finalResult.getSuccessCount(),
          finalResult.getFailureCount(),
          finalResult.getTotalChunks());

      // 如果有失败，记录告警
      if (finalResult.getFailureCount() > 0) {
        log.error("定时任务检测到 {} 个资源重建失败，资源ID: {}",
            finalResult.getFailureCount(),
            finalResult.getFailedResourceIds());
      }

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("========== 定时任务：全量重建失败，耗时 {} ms ==========", duration, e);
    }
  }
}
