/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.service.ProcessingService;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.service.VectorService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 向量化管理 REST 控制器。
 *
 * <p>提供向量化和向量索引管理的API端点。
 *
 * <h3>端点分类：</h3>
 * <ul>
 *   <li><b>向量化操作</b>：单个资源向量化、批量向量化、全部向量化</li>
 *   <li><b>资源重新处理</b>：重新处理资源（切片+向量化）</li>
 *   <li><b>进度查询</b>：查询向量化进度</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/vectors")
@RequiredArgsConstructor
@Validated
public class VectorController {

  private final VectorService vectorService;
  private final ProcessingService processingService;
  private final ResourceService resourceService;

  /**
   * 向量化指定资源的所有切片。
   *
   * <p>仅进行向量化，不重新切片。如果资源没有切片，会返回错误。
   *
   * @param id 资源ID
   * @param authentication 认证信息
   * @return 操作结果
   */
  @PostMapping("/resources/{id}/vectorize")
  public ResponseEntity<ApiResponse<String>> vectorizeResource(
      @PathVariable @Min(1) Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/vectors/resources/{}/vectorize - userId={}", id, userId);

    // 检查资源是否存在
    Resource resource = resourceService.getResourceById(id);
    if (resource == null) {
      return ResponseEntity.notFound().build();
    }

    // 检查资源是否有切片
    if (resource.getChunkCount() == 0) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(400, "资源没有切片，请使用重新处理接口"));
    }

    // 异步向量化
    CompletableFuture.runAsync(() -> {
      try {
        vectorService.vectorizeChunks(id);
      } catch (Exception e) {
        log.error("向量化失败: resourceId={}", id, e);
      }
    });

    return ResponseEntity.ok(ApiResponse.success("向量化任务已触发", "向量化任务已触发"));
  }

  /**
   * 重新处理指定资源（切片+向量化）。
   *
   * <p>删除旧切片，重新解析文档，生成新切片，并向量化。
   *
   * @param id 资源ID
   * @param authentication 认证信息
   * @return 操作结果
   */
  @PostMapping("/resources/{id}/reprocess")
  public ResponseEntity<ApiResponse<String>> reprocessResource(
      @PathVariable @Min(1) Long id,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/vectors/resources/{}/reprocess - userId={}", id, userId);

    // 检查资源是否存在
    Resource resource = resourceService.getResourceById(id);
    if (resource == null) {
      return ResponseEntity.notFound().build();
    }

    try {
      int chunkCount = processingService.reprocessResource(id);
      String message = String.format("重新处理完成，已生成 %d 个切片并向量化", chunkCount);
      return ResponseEntity.ok(ApiResponse.success(message, message));
    } catch (Exception e) {
      log.error("重新处理失败: resourceId={}", id, e);
      return ResponseEntity.status(500)
          .body(ApiResponse.error(500, "重新处理失败: " + e.getMessage()));
    }
  }

  /**
   * 批量向量化多个资源。
   *
   * @param request 批量向量化请求
   * @param authentication 认证信息
   * @return 操作结果
   */
  @PostMapping("/batch-vectorize")
  public ResponseEntity<ApiResponse<String>> batchVectorize(
      @RequestBody com.aetheris.rag.dto.request.BatchDeleteRequest request,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/vectors/batch-vectorize - userId={}, count={}",
        userId, request.getIds().size());

    // 异步批量向量化（循环调用vectorizeChunks）
    CompletableFuture.runAsync(() -> {
      for (Long resourceId : request.getIds()) {
        try {
          vectorService.vectorizeChunks(resourceId);
        } catch (Exception e) {
          log.error("向量化失败: resourceId={}", resourceId, e);
        }
      }
    });

    String message = String.format("批量向量化任务已触发，共 %d 个资源", request.getIds().size());
    return ResponseEntity.ok(ApiResponse.success(message, message));
  }

  /**
   * 向量化所有未向量化的切片。
   *
   * @return 操作结果
   */
  @PostMapping("/vectorize-all")
  public ResponseEntity<ApiResponse<String>> vectorizeAllUnvectorized() {
    log.info("POST /api/vectors/vectorize-all");

    CompletableFuture.runAsync(() -> {
      try {
        vectorService.vectorizeAllUnvectorized();
      } catch (Exception e) {
        log.error("批量向量化失败", e);
      }
    });

    return ResponseEntity.ok(ApiResponse.success("向量化任务已触发", "正在向量化所有未向量化的切片"));
  }

  /**
   * 查询向量化进度。
   *
   * @return 进度信息
   */
  @GetMapping("/progress")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getVectorizationProgress() {
    log.info("GET /api/vectors/progress");

    // 查询统计信息
    int totalChunks = vectorService.getTotalChunksCount();
    int vectorizedCount = vectorService.getVectorizedChunksCount();
    int unvectorizedCount = totalChunks - vectorizedCount;
    double progress = totalChunks > 0 ? (vectorizedCount * 100.0 / totalChunks) : 0;

    Map<String, Object> progressInfo = Map.of(
        "totalChunks", totalChunks,
        "vectorizedCount", vectorizedCount,
        "unvectorizedCount", unvectorizedCount,
        "progress", progress
    );

    return ResponseEntity.ok(ApiResponse.success(progressInfo));
  }
}
