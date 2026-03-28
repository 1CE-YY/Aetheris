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
 * 单条推荐资源项 DTO。
 *
 * <p>包含推荐资源的完整信息，包括：
 * <ul>
 *   <li>资源基本信息（ID、标题、标签、类型、描述）</li>
 *   <li>AI 生成的推荐理由和学习建议</li>
 *   <li>证据引用列表（支持可追溯性）</li>
 *   <li>相似度分数</li>
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
public class RecommendationItem {

  /** 资源ID */
  private String resourceId;

  /** 资源标题 */
  private String title;

  /** 标签（逗号分隔） */
  private String tags;

  /**
   * AI 生成的推荐理由（1-2 句话）。
   *
   * <p>个性化推荐时基于用户画像和资源内容生成；
   * 默认推荐时为固定理由"热门学习资源"。
   */
  private String reason;

  /**
   * AI 生成的学习建议。
   *
   * <p>建议用户先学习哪个章节或内容，
   * 基于资源内容和引用证据生成。
   */
  private String suggestion;

  /** 推荐理由的证据引用列表 */
  private List<Citation> citations;

  /** 相似度分数（0.0-1.0，个性化推荐时使用画像向量检索分数） */
  private double score;

  /** 文件类型（PDF/MARKDOWN） */
  private String fileType;

  /** 资源描述 */
  private String description;
}
