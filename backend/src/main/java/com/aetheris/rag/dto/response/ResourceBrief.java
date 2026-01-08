/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源简略信息 DTO。
 *
 * <p>此类用于降级场景（LLM 不可用时），返回资源的简要信息，不包含详细的切片列表。
 *
 * <p>包含字段：
 *
 * <ul>
 *   <li>id：资源 ID
 *   <li>title：资源标题
 *   <li>tags：标签（逗号分隔）
 *   <li>fileType：文件类型（PDF/MARKDOWN）
 *   <li>description：资源描述
 *   <li>uploadTime：上传时间
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBrief {

  /** 资源 ID（Long 类型转换为 String 以保持一致性） */
  private String id;

  /** 资源标题 */
  private String title;

  /** 标签（逗号分隔，如："机器学习,深度学习"） */
  private String tags;

  /** 文件类型（PDF/MARKDOWN） */
  private String fileType;

  /** 资源描述（可选） */
  private String description;

  /** 上传时间 */
  private Instant uploadTime;
}
