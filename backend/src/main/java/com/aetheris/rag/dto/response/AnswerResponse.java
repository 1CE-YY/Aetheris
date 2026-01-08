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
 * 问答响应 DTO。
 *
 * <p>此类表示 RAG 问答的响应结果，包含：
 *
 * <ul>
 *   <li>answer：AI 生成的答案文本
 *   <li>citations：答案引用的证据列表（每条引用包含 resourceId、chunkId、chunkIndex、location、snippet、score）
 *   <li>evidenceInsufficient：标识证据是否不足（当检索结果少于 2 个或相似度分数过低时为 true）
 *   <li>fallbackResources：降级时返回的候选资源列表（LLM 不可用时）
 *   <li>latencyMs：总响应时间（毫秒）
 * </ul>
 *
 * <p>响应场景：
 *
 * <ul>
 *   <li><b>正常场景</b>：LLM 可用，返回生成的答案 + 引用列表，evidenceInsufficient=false，fallbackResources=null
 *   <li><b>证据不足场景</b>：检索结果少于 2 个或相似度分数过低，返回"根据现有资料无法完整回答" + 检索结果，evidenceInsufficient=true
 *   <li><b>降级场景</b>：LLM 不可用，返回检索结果 + 证据摘要 + 错误提示，fallbackResources 包含候选资源列表
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
public class AnswerResponse {

  /** AI 生成的答案文本（降级时为证据摘要或错误提示） */
  private String answer;

  /** 答案引用的证据列表（每个关键论断至少 1 条引用） */
  private List<Citation> citations;

  /**
   * 标识证据是否不足。
   *
   * <p>当满足以下任一条件时为 true：
   * <ul>
   *   <li>检索结果少于 2 个
   *   <li>所有检索结果的相似度分数都低于阈值（0.5）
   * </ul>
   */
  private boolean evidenceInsufficient;

  /**
   * 降级时返回的候选资源列表（LLM 不可用时）。
   *
   * <p>正常场景下此字段为 null。
   * <p>包含资源的简要信息（id、title、tags、fileType、description、uploadTime）。
   */
  private List<ResourceBrief> fallbackResources;

  /** 总响应时间（毫秒），包括检索、生成等所有阶段 */
  private long latencyMs;
}
