/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.dto.response.RecommendationResponse;

/**
 * 推荐服务接口。
 *
 * <p>提供基于用户画像的个性化推荐功能，支持：
 * <ul>
 *   <li>个性化推荐：基于用户画像向量检索相关资源</li>
 *   <li>默认推荐：无画像时按热度/时间返回资源</li>
 *   <li>推荐理由和学习建议生成（通过 LLM）</li>
 *   <li>证据引用（Citations）</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
public interface RecommendationService {

  /**
   * 为用户生成个性化推荐。
   *
   * <p>推荐流程：
   * <ol>
   *   <li>获取用户画像向量（如果有）</li>
   *   <li>使用画像向量进行 KNN 检索（或使用默认策略）</li>
   *   <li>按资源聚合检索结果</li>
   *   <li>调用 LLM 生成推荐理由和学习建议</li>
   *   <li>构建推荐响应（包含引用证据）</li>
   * </ol>
   *
   * @param userId 用户ID
   * @param topN 推荐数量（1-50）
   * @return 推荐响应
   */
  RecommendationResponse recommend(Long userId, int topN);

  /**
   * 异步记录推荐点击行为并更新画像。
   *
   * @param userId 用户ID
   * @param resourceId 资源ID
   */
  void recordClickAsync(Long userId, Long resourceId);

  /**
   * 异步记录推荐收藏行为并更新画像。
   *
   * @param userId 用户ID
   * @param resourceId 资源ID
   */
  void recordFavoriteAsync(Long userId, Long resourceId);
}
