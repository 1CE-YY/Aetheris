/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源切片实体类。
 *
 * <p>表示学习资源的文本切片，包含位置信息（页码/章节）和向量化状态。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

  /** 切片ID */
  private Long id;

  /** 资源ID */
  private Long resourceId;

  /**
   * 切片序号（从0开始）。
   *
   * <p>同一资源的切片按文档顺序编号，从0开始递增。
   */
  private Integer chunkIndex;

  /** 切片文本内容 */
  private String chunkText;

  /**
   * 定位信息（PDF页码范围/MD章节路径）。
   *
   * <p>格式示例：
   * <ul>
   *   <li>PDF: "第5-6页"</li>
   *   <li>Markdown: "第一章>1.1节>1.1.1小节"</li>
   * </ul>
   */
  private String locationInfo;

  /**
   * PDF起始页码（可选）。
   *
   * <p>仅对PDF文档有效，表示切片内容在原文档中的起始页码。
   */
  private Integer pageStart;

  /**
   * PDF结束页码（可选）。
   *
   * <p>仅对PDF文档有效，表示切片内容在原文档中的结束页码。
   */
  private Integer pageEnd;

  /**
   * Markdown章节路径（可选）。
   *
   * <p>仅对Markdown文档有效，表示切片所属的章节层级。
   * 格式：使用 ">" 分隔的标题层级，如 "第一章>1.1节>1.1.1小节"。
   */
  private String chapterPath;

  /**
   * 文本哈希（SHA-256，用于Embedding缓存）。
   *
   * <p>相同文本内容的切片会复用缓存的embedding向量。
   */
  private String textHash;

  /** 是否已向量化 */
  private Boolean vectorized;

  /** 创建时间 */
  private Instant createdAt;
}
