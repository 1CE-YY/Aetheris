/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.dto.request.AskRequest;
import com.aetheris.rag.dto.response.AnswerResponse;

/**
 * RAG 问答服务接口。
 *
 * <p>提供检索增强生成（RAG）和纯 LLM 两种问答模式，支持：
 *
 * <ul>
 *   <li>RAG 模式：语义检索 → Prompt 构建 → LLM 生成 → 提取引用
 *   <li>纯 LLM 模式：跳过检索，直接调用 LLM 生成答案
 *   <li>降级处理：LLM 不可用时返回检索结果或错误提示
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-08
 */
public interface RagService {

  /**
   * 执行 RAG 问答或纯 LLM 问答，返回答案和引用。
   *
   * <p>根据 useRag 参数选择执行模式：
   *
   * <ul>
   *   <li><b>RAG 模式（useRag=true）</b>：
   *     <ol>
   *       <li>语义检索：根据用户问题检索 Top-K 相关切片
   *       <li>Prompt 构建：基于检索结果构建 Prompt
   *       <li>LLM 生成：调用 ChatGateway 生成答案
   *       <li>提取引用：从检索结果中提取 Citations
   *     </ol>
   *   <li><b>纯 LLM 模式（useRag=false）</b>：
   *     <ol>
   *       <li>跳过检索步骤
   *       <li>直接调用 LLM 生成答案（无引用来源）
   *     </ol>
   * </ul>
   *
   * <p>特殊场景：
   * <ul>
   *   <li><b>证据不足</b>：检索结果少于 2 个或相似度分数过低，返回提示信息
   *   <li><b>LLM 不可用</b>：RAG 模式下返回检索结果，纯 LLM 模式下返回错误提示
   * </ul>
   *
   * @param userId 用户 ID（用于记录行为）
   * @param request 问答请求（包含 question、topK、useRag）
   * @return 问答响应（包含 answer、citations、evidenceInsufficient、fallbackResources、latencyMs）
   */
  AnswerResponse ask(Long userId, AskRequest request);
}
