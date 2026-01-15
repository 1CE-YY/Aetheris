/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.service.impl.VectorServiceImpl;
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

  private final VectorService vectorService;
  private final ResourceService resourceService;

  /**
   * 修复指定资源的向量化状态。
   *
   * @param id 资源ID
   * @return 修复结果
   */
  @PostMapping("/resources/{id}/repair-vectorization")
  public ResponseEntity<ApiResponse<Boolean>> repairVectorization(@PathVariable Long id) {
    log.info("POST /api/admin/resources/{}/repair-vectorization", id);

    boolean repaired = vectorService.recalculateVectorizationStatus(id);
    String message = repaired ? "状态已修复" : "状态正常，无需修复";

    return ResponseEntity.ok(ApiResponse.success(repaired, message));
  }

  /**
   * 批量修复所有资源的向量化状态。
   *
   * @return 修复结果
   */
  @PostMapping("/resources/repair-all-vectorization")
  public ResponseEntity<ApiResponse<String>> repairAllVectorization() {
    log.info("POST /api/admin/resources/repair-all-vectorization");

    int repairedCount = vectorService.repairAllVectorizationStatus();
    String message = String.format("批量修复完成，共修复 %d 个资源", repairedCount);

    return ResponseEntity.ok(ApiResponse.success(message, message));
  }

  /**
   * 重建向量索引。
   *
   * <p>删除旧索引，重置所有切片的向量化状态，创建新索引，并重新向量化所有切片。
   *
   * @return 操作结果
   */
  @PostMapping("/vector-index/rebuild")
  public ResponseEntity<ApiResponse<String>> rebuildVectorIndex() {
    log.info("POST /api/admin/vector-index/rebuild");

    try {
      vectorService.rebuildVectorIndex();
      return ResponseEntity.ok(ApiResponse.success("向量索引重建完成", "向量索引重建完成"));
    } catch (Exception e) {
      log.error("重建向量索引失败", e);
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "重建向量索引失败: " + e.getMessage()));
    }
  }

  /**
   * 手动触发全量重建。
   *
   * <p>删除所有切片，重新解析所有原始文件，生成新切片并触发向量化。
   * 最后会重建向量索引，确保所有切片都被正确向量化。
   *
   * @return 重建结果
   */
  @PostMapping("/resources/rebuild-all")
  public ResponseEntity<ApiResponse<RebuildResult>> rebuildAllResources() {
    log.info("POST /api/admin/resources/rebuild-all");

    try {
      // ✅ 调用 rebuildAllResources() 即可，内部会处理切片重建和向量化
      RebuildResult result = resourceService.rebuildAllResources();

      String message =
          String.format(
              "全量重建完成: 成功=%d, 失败=%d, 总切片=%d, 耗时=%d ms",
              result.getSuccessCount(),
              result.getFailureCount(),
              result.getTotalChunks(),
              result.getDuration());

      return ResponseEntity.ok(ApiResponse.success(result, message));
    } catch (Exception e) {
      log.error("全量重建失败", e);
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "全量重建失败: " + e.getMessage()));
    }
  }

  /**
   * 完全重建索引和资源（统一的重建接口）。
   *
   * <p>执行流程：
   * <ol>
   *   <li>删除所有资源的旧切片</li>
   *   <li>重新解析所有原始文档，生成新切片</li>
   *   <li>删除 Redis 缓存、向量、索引</li>
   *   <li>创建新的向量索引</li>
   *   <li>批量向量化所有新生成的切片</li>
   * </ol>
   *
   * @param config 重建配置（可选）
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
          .skipVectorization(false)  // 修改：不跳过向量化，保持与其他接口一致
          .build();
    }

    try {
      // ✅ 步骤 1: 重建所有资源（重新切片）
      RebuildResult resourceResult = ((com.aetheris.rag.service.impl.ResourceServiceImpl) resourceService)
          .rebuildAllResources(config);

      // ✅ 步骤 2: 重建向量索引（删除索引和向量数据，统一批量向量化）
      RebuildConfig indexConfig = RebuildConfig.builder()
          .dropIndex(true)
          .batchSize(config.getBatchSize())
          .maxRetries(config.getMaxRetries())
          .skipOnError(config.isSkipOnError())
          .enableConcurrency(config.isEnableConcurrency())
          .build();
      RebuildResult indexResult = ((VectorServiceImpl) vectorService)
          .rebuildVectorIndex(indexConfig);

      // 合并结果
      RebuildResult finalResult = RebuildResult.builder()
          .successCount(resourceResult.getSuccessCount())
          .failureCount(resourceResult.getFailureCount())
          .totalChunks(resourceResult.getTotalChunks())
          .duration(resourceResult.getDuration() + indexResult.getDuration())
          .failedResourceIds(resourceResult.getFailedResourceIds())
          .build();

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

    boolean cancelled = ((VectorServiceImpl) vectorService).cancelRebuild();

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

    // 简化实现：检查锁是否存在
    boolean isRebuilding = false; // TODO: 实现状态检查逻辑

    return ResponseEntity.ok(ApiResponse.success(
        isRebuilding,
        isRebuilding ? "系统正在执行重建任务" : "系统空闲"));
  }

  /**
   * 🔧 临时测试接口：重新处理指定资源（无需权限，仅用于调试）。
   *
   * @param id 资源ID
   * @return 重建结果
   */
  @PostMapping("/debug/reprocess/{id}")
  public ResponseEntity<ApiResponse<String>> reprocessResourceDebug(@PathVariable Long id) {
    log.info("🔧 [DEBUG] POST /api/admin/debug/reprocess/{}", id);

    try {
      int chunkCount = resourceService.reprocessResource(id);
      String message = String.format("重新处理完成，已生成 %d 个切片", chunkCount);
      return ResponseEntity.ok(ApiResponse.success(message, message));
    } catch (Exception e) {
      log.error("重新处理失败: resourceId={}", id, e);
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "重新处理失败: " + e.getMessage()));
    }
  }
}
