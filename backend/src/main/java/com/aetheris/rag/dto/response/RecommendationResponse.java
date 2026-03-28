/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个性化推荐响应 DTO。
 *
 * <p>包含推荐资源列表和推荐元信息：
 * <ul>
 *   <li>items：推荐资源项列表（每项包含理由、建议、引用）</li>
 *   <li>personalized：是否为个性化推荐（true 基于画像，false 默认推荐）</li>
 *   <li>latencyMs：推荐生成总耗时（毫秒）</li>
 *   <li>count：推荐结果数量</li>
 * </ul>
 *
 * <p>两种推荐场景：
 * <ul>
 *   <li><b>个性化推荐</b>（personalized=true）：基于用户画像向量检索，推荐理由由 LLM 生成</li>
 *   <li><b>默认推荐</b>（personalized=false）：新用户无画像时，按热度/时间排序返回资源</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

  /** 推荐资源项列表 */
  private List<RecommendationItem> items;

  /**
   * 是否为个性化推荐。
   *
   * <p>true 表示基于用户画像向量检索的个性化推荐；
   * false 表示无画像时的默认推荐（按热度/时间排序）。
   */
  private boolean personalized;

  /** 推荐生成总耗时（毫秒） */
  private long latencyMs;

  /** 推荐结果数量 */
  private int count;
}
