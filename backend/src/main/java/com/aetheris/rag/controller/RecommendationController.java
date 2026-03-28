/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.dto.response.RecommendationResponse;
import com.aetheris.rag.exception.BadRequestException;
import com.aetheris.rag.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐的 REST 控制器。
 *
 * <p>提供个性化推荐接口，支持：
 * <ul>
 *   <li>获取推荐列表（基于用户画像或默认推荐）</li>
 *   <li>记录推荐点击行为</li>
 *   <li>记录推荐收藏行为</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Validated
public class RecommendationController {

  private final RecommendationService recommendationService;

  /**
   * 获取个性化推荐列表。
   *
   * <p>推荐策略：
   * <ul>
   *   <li>有画像：基于画像向量检索相关资源，生成推荐理由和学习建议</li>
   *   <li>无画像：返回热门/最近上传的默认推荐</li>
   * </ul>
   *
   * @param topN 推荐数量（1-50，默认 10）
   * @param authentication Spring Security 认证对象
   * @return 推荐响应
   */
  @GetMapping
  public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int topN,
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("GET /api/recommendations - userId={}, topN={}", userId, topN);

    RecommendationResponse response = recommendationService.recommend(userId, topN);

    log.info("推荐生成完成：userId={}, personalized={}, count={}, latencyMs={}ms",
        userId, response.isPersonalized(), response.getCount(), response.getLatencyMs());

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 记录推荐资源的点击行为。
   *
   * <p>用户点击推荐卡片时触发，异步记录行为并更新用户画像。
   *
   * @param resourceId 资源ID
   * @param authentication Spring Security 认证对象
   * @return 操作结果
   */
  @PostMapping("/click")
  public ResponseEntity<ApiResponse<Void>> recordClick(
      @RequestParam Long resourceId,
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/recommendations/click - userId={}, resourceId={}", userId, resourceId);

    recommendationService.recordClickAsync(userId, resourceId);

    return ResponseEntity.ok(ApiResponse.success(null, "点击行为已记录"));
  }

  /**
   * 记录推荐资源的收藏行为。
   *
   * <p>用户收藏推荐资源时触发，异步记录行为并更新用户画像。
   *
   * @param resourceId 资源ID
   * @param authentication Spring Security 认证对象
   * @return 操作结果
   */
  @PostMapping("/favorite")
  public ResponseEntity<ApiResponse<Void>> recordFavorite(
      @RequestParam Long resourceId,
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/recommendations/favorite - userId={}, resourceId={}", userId, resourceId);

    recommendationService.recordFavoriteAsync(userId, resourceId);

    return ResponseEntity.ok(ApiResponse.success(null, "收藏行为已记录"));
  }
}
