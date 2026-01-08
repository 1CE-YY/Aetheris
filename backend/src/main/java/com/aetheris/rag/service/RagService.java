/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.dto.request.AskRequest;
import com.aetheris.rag.dto.response.AnswerResponse;

/**
 * RAG 问答服务接口。
 *
 * <p>提供检索增强生成（RAG）的完整问答流程，支持：
 *
 * <ul>
 *   <li>语义检索（调用 SearchService）
 *   <li>Prompt 构建（基于检索结果）
 *   <li>LLM 生成答案（调用 ChatGateway）
 *   <li>提取引用来源（Citations）
 *   <li>降级处理（LLM 不可用或证据不足时）
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-08
 */
public interface RagService {

  /**
   * 执行 RAG 问答，返回答案和引用。
   *
   * <p>完整流程：
   * <ol>
   *   <li>语义检索：根据用户问题检索 Top-K 相关切片
   *   <li>Prompt 构建：基于检索结果构建 Prompt
   *   <li>LLM 生成：调用 ChatGateway 生成答案
   *   <li>提取引用：从检索结果中提取 Citations
   *   <li>降级处理：LLM 不可用时返回检索结果 + 证据摘要
   * </ol>
   *
   * <p>特殊场景：
   * <ul>
   *   <li><b>证据不足</b>：检索结果少于 2 个或相似度分数过低，返回"根据现有资料无法完整回答"
   *   <li><b>LLM 不可用</b>：返回检索结果 + 证据摘要 + 错误提示
   * </ul>
   *
   * @param userId 用户 ID（用于记录行为）
   * @param request 问答请求（包含 question 和 topK）
   * @return 问答响应（包含 answer、citations、evidenceInsufficient、fallbackResources、latencyMs）
   */
  AnswerResponse ask(Long userId, AskRequest request);
}
