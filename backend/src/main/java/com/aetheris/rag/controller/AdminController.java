/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 系统维护接口控制器。
 *
 * <p>提供数据修复和维护功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-12
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final ProcessingService processingService;

  /**
   * 完全重建所有资源。
   *
   * <p>执行流程：
   * <ol>
   *   <li>删除所有资源的旧切片</li>
   *   <li>重新解析所有原始文档，生成新切片</li>
   *   <li>删除 Redis 向量数据和索引</li>
   *   <li>批量向量化所有新生成的切片</li>
   *   <li>重建向量索引</li>
   * </ol>
   *
   * @param config 重建配置（可选，不传则使用默认配置）
   * @return 重建结果
   */
  @PostMapping("/system/full-rebuild")
  public ResponseEntity<ApiResponse<RebuildResult>> fullRebuild(
      @RequestBody(required = false) RebuildConfig config) {

    log.info("POST /api/admin/system/full-rebuild");

    // 使用默认配置
    if (config == null) {
      config = RebuildConfig.builder()
          .dropIndex(true)
          .rechunk(true)
          .batchSize(50)
          .maxRetries(3)
          .skipOnError(true)
          .enableConcurrency(true)
          .skipVectorization(false)
          .build();
    }

    try {
      // 调用ProcessingService执行完全重建
      RebuildResult finalResult = processingService.fullRebuild(config);

      String message = String.format(
          "完全重建完成: 成功=%d, 失败=%d, 总切片=%d, 耗时=%d ms",
          finalResult.getSuccessCount(),
          finalResult.getFailureCount(),
          finalResult.getTotalChunks(),
          finalResult.getDuration());

      return ResponseEntity.ok(ApiResponse.success(finalResult, message));

    } catch (Exception e) {
      log.error("完全重建失败", e);
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "完全重建失败: " + e.getMessage()));
    }
  }

  /**
   * 取消重建任务。
   *
   * @return 操作结果
   */
  @PostMapping("/system/cancel-rebuild")
  public ResponseEntity<ApiResponse<String>> cancelRebuild() {
    log.info("POST /api/admin/system/cancel-rebuild");

    boolean cancelled = processingService.cancelRebuild();

    if (cancelled) {
      return ResponseEntity.ok(ApiResponse.success("取消请求已提交", "正在取消重建任务"));
    } else {
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "取消重建任务失败"));
    }
  }

  /**
   * 检查是否有正在执行的重建任务。
   *
   * @return 是否正在执行
   */
  @GetMapping("/system/rebuild-status")
  public ResponseEntity<ApiResponse<Boolean>> checkRebuildStatus() {
    log.info("GET /api/admin/system/rebuild-status");

    boolean isRebuilding = processingService.isRebuilding();

    return ResponseEntity.ok(ApiResponse.success(
        isRebuilding,
        isRebuilding ? "系统正在执行重建任务" : "系统空闲"));
  }
}
