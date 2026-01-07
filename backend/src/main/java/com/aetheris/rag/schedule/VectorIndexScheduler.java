/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.schedule;

import com.aetheris.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 向量索引定时任务。
 *
 * <p>定期重建向量索引，清理 HNSW 索引中的悬空节点。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorIndexScheduler {

  private final VectorService vectorService;

  /**
   * 定时重建向量索引。
   *
   * <p>每天凌晨 3 点执行一次。
   * <p>Cron 表达式：秒 分 时 日 月 周
   */
  @Scheduled(cron = "0 0 3 * * ?")
  public void rebuildVectorIndexDaily() {
    log.info("========== 定时任务：开始重建向量索引 ==========");
    long startTime = System.currentTimeMillis();

    try {
      vectorService.rebuildVectorIndex();
      long duration = System.currentTimeMillis() - startTime;
      log.info("========== 定时任务：向量索引重建完成，耗时 {} ms ==========", duration);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("========== 定时任务：向量索引重建失败，耗时 {} ms ==========", duration, e);
    }
  }
}
