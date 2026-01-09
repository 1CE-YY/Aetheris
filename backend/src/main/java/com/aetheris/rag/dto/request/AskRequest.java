/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答请求 DTO。
 *
 * <p>此类表示用户提交的问答请求，包含：
 *
 * <ul>
 *   <li>question：用户的问题文本（必填）
 *   <li>topK：检索返回的切片数量（可选，默认为 5）
 *   <li>useRag：是否使用 RAG 流程（可选，默认为 true）
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
public class AskRequest {

  /**
   * 用户的问题文本。
   *
   * <p>必填，不能为空或空白字符。
   */
  @NotBlank(message = "问题不能为空")
  private String question;

  /**
   * 检索返回的切片数量。
   *
   * <p>可选，默认值为 5。
   * <p>范围：1-20（避免检索过多切片导致性能问题）
   */
  @Min(value = 1, message = "topK 至少为 1")
  @Max(value = 20, message = "topK 最多为 20")
  private Integer topK;

  /**
   * 是否使用 RAG 流程。
   *
   * <p>默认为 true，表示执行完整的检索增强生成流程。
   * <p>设置为 false 时，跳过检索步骤，直接调用 LLM 生成答案（不包含引用来源）。
   */
  @Builder.Default
  private Boolean useRag = true;
}
