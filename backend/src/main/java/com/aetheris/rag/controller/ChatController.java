/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.dto.request.AskRequest;
import com.aetheris.rag.dto.response.AnswerResponse;
import com.aetheris.rag.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 问答的 REST 控制器。
 *
 * <p>提供基于学习资源的问答接口，支持：
 *
 * <ul>
 *   <li>语义检索 + RAG 生成
 *   <li>引用来源返回（Citations）
 *   <li>证据不足提示
 *   <li>LLM 不可用时的降级处理
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-08
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final RagService ragService;

  /**
   * 执行 RAG 问答。
   *
   * <p>完整流程：
   * <ol>
   *   <li>语义检索：根据用户问题检索 Top-K 相关切片
   *   <li>Prompt 构建：基于检索结果构建 Prompt
   *   <li>LLM 生成：调用 ChatGateway 生成答案
   *   <li>提取引用：从检索结果中提取 Citations
   * </ol>
   *
   * <p>特殊场景：
   * <ul>
   *   <li><b>证据不足</b>：检索结果少于 2 个或相似度分数过低，返回提示信息
   *   <li><b>LLM 不可用</b>：返回检索结果 + 证据摘要 + 错误提示
   * </ul>
   *
   * @param request 问答请求（包含 question 和 topK）
   * @param authentication Spring Security 认证对象（用于获取当前用户 ID）
   * @return 问答响应（包含 answer、citations、evidenceInsufficient、fallbackResources、latencyMs）
   */
  @PostMapping("/ask")
  public ResponseEntity<ApiResponse<AnswerResponse>> ask(
      @Valid @RequestBody AskRequest request,
      Authentication authentication) {

    // 从认证对象中获取用户 ID
    Long userId = (Long) authentication.getPrincipal();
    log.info("POST /api/chat/ask - userId={}, question='{}', topK={}",
        userId, request.getQuestion(), request.getTopK());

    // 调用 RAG 服务
    AnswerResponse answerResponse = ragService.ask(userId, request);

    // 构建响应
    ApiResponse<AnswerResponse> response = ApiResponse.success(answerResponse);

    log.info("RAG 问答完成：userId={}, latencyMs={}ms, evidenceInsufficient={}",
        userId, answerResponse.getLatencyMs(), answerResponse.isEvidenceInsufficient());

    return ResponseEntity.ok(response);
  }
}
