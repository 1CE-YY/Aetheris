/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.request.AskRequest;
import com.aetheris.rag.dto.response.AnswerResponse;
import com.aetheris.rag.dto.response.Citation;
import com.aetheris.rag.dto.response.ResourceBrief;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.gateway.ChatGateway;
import com.aetheris.rag.service.BehaviorService;
import com.aetheris.rag.service.RagService;
import com.aetheris.rag.service.SearchService;
import com.aetheris.rag.util.PerformanceTimer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 问答服务实现类。
 *
 * <p>实现完整的检索增强生成（RAG）流程，包括：
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
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

  private final SearchService searchService;
  private final ChatGateway chatGateway;
  private final BehaviorService behaviorService;

  /** 相似度阈值（低于此值认为证据不足） */
  private static final double SCORE_THRESHOLD = 0.5;

  /** 最少检索结果数量（少于此值认为证据不足） */
  private static final int MIN_CITATIONS = 2;

  @Override
  public AnswerResponse ask(Long userId, AskRequest request) {
    PerformanceTimer timer = new PerformanceTimer();
    timer.recordStage("retrieval");

    int topK = request.getTopK() != null ? request.getTopK() : 5;

    // 1. 语义检索
    log.info("执行 RAG 问答：userId={}, question='{}', topK={}", userId, request.getQuestion(), topK);
    List<Citation> citations = searchService.searchAggregated(request.getQuestion(), topK);

    timer.endStage();
    timer.recordStage("generation");

    // 2. 检查证据是否充足
    boolean evidenceInsufficient = isEvidenceInsufficient(citations);

    // 3. 构建 Prompt 并生成答案
    String answer;
    List<ResourceBrief> fallbackResources = null;

    if (!evidenceInsufficient) {
      // 正常场景：调用 LLM 生成答案
      try {
        String prompt = buildPrompt(request.getQuestion(), citations);
        answer = chatGateway.chat(buildSystemPrompt(), prompt);
        log.info("LLM 生成答案成功：userId={}, answerLength={}", userId, answer.length());
      } catch (Exception e) {
        // LLM 调用失败，降级为检索结果摘要
        log.warn("LLM 调用失败，降级为检索结果摘要：userId={}, error={}", userId, e.getMessage());
        answer = buildFallbackAnswer(citations);
        fallbackResources = buildFallbackResources(citations);
      }
    } else {
      // 证据不足场景：返回提示信息
      log.warn("证据不足：userId={}, citationsCount={}, avgScore={}",
          userId, citations.size(), getAverageScore(citations));
      answer = "根据现有资料无法完整回答您的问题。建议您：\n"
          + "1. 尝试更具体的问题\n"
          + "2. 查阅以下相关资源获取更多信息";
      fallbackResources = buildFallbackResources(citations);
    }

    timer.endStage();

    // 4. 异步记录查询行为（不阻塞主流程）
    recordQueryBehaviorAsync(userId, request.getQuestion());

    // 5. 构建响应
    long totalLatency = timer.getElapsedMs();
    AnswerResponse response = AnswerResponse.builder()
        .answer(answer)
        .citations(citations)
        .evidenceInsufficient(evidenceInsufficient)
        .fallbackResources(fallbackResources)
        .latencyMs(totalLatency)
        .build();

    log.info("RAG 问答完成：userId={}, latencyMs={}ms, retrieval={}ms, generation={}ms, citationsCount={}",
        userId, totalLatency, timer.getStageDuration("retrieval"), timer.getStageDuration("generation"),
        citations.size());

    return response;
  }

  /**
   * 检查证据是否充足。
   *
   * <p>满足以下任一条件则认为证据不足：
   * <ul>
   *   <li>检索结果少于 MIN_CITATIONS（2 个）
   *   <li>所有检索结果的相似度分数都低于 SCORE_THRESHOLD（0.5）
   * </ul>
   *
   * @param citations 检索结果列表
   * @return true 如果证据不足，否则返回 false
   */
  private boolean isEvidenceInsufficient(List<Citation> citations) {
    if (citations.size() < MIN_CITATIONS) {
      log.debug("证据不足：检索结果数量 {} 少于阈值 {}", citations.size(), MIN_CITATIONS);
      return true;
    }

    double avgScore = getAverageScore(citations);
    if (avgScore < SCORE_THRESHOLD) {
      log.debug("证据不足：平均相似度 {} 低于阈值 {}", avgScore, SCORE_THRESHOLD);
      return true;
    }

    return false;
  }

  /**
   * 计算检索结果的平均相似度分数。
   *
   * @param citations 检索结果列表
   * @return 平均相似度分数
   */
  private double getAverageScore(List<Citation> citations) {
    if (citations.isEmpty()) {
      return 0.0;
    }
    return citations.stream()
        .mapToDouble(Citation::getScore)
        .average()
        .orElse(0.0);
  }

  /**
   * 构建系统 Prompt。
   *
   * @return 系统 Prompt
   */
  private String buildSystemPrompt() {
    return "你是一个专业的学习助手，擅长基于提供的学习资源回答问题。\n"
        + "请严格遵循以下要求：\n"
        + "1. 答案必须严格基于上述学习资源片段\n"
        + "2. 每个关键论断标注引用来源，格式为 [资源标题, 位置]\n"
        + "3. 如果资料不足，明确说明'根据现有资料无法完整回答'\n"
        + "4. 使用简洁清晰的语言回答问题\n"
        + "5. 避免添加资料外的主观推断";
  }

  /**
   * 构建用户 Prompt（包含问题和检索结果）。
   *
   * @param question 用户问题
   * @param citations 检索结果列表
   * @return 用户 Prompt
   */
  private String buildPrompt(String question, List<Citation> citations) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("问题：").append(question).append("\n\n");
    prompt.append("相关学习资源片段：\n");

    for (int i = 0; i < citations.size(); i++) {
      Citation citation = citations.get(i);
      prompt.append(String.format("\n[资源 %d]\n", i + 1));
      prompt.append(String.format("标题：%s\n", citation.getResourceTitle()));
      prompt.append(String.format("位置：%s\n", citation.getLocation()));
      prompt.append(String.format("内容：%s\n", citation.getSnippet()));
      prompt.append(String.format("相似度：%.2f\n", citation.getScore()));
    }

    prompt.append("\n请基于上述资源片段回答问题，并在每个关键论断后标注引用来源。");
    return prompt.toString();
  }

  /**
   * 构建降级答案（LLM 不可用时使用）。
   *
   * <p>返回检索结果的摘要信息，帮助用户快速了解相关资源。
   *
   * @param citations 检索结果列表
   * @return 降级答案文本
   */
  private String buildFallbackAnswer(List<Citation> citations) {
    StringBuilder answer = new StringBuilder();
    answer.append("抱歉，AI 答案生成服务暂时不可用。以下是为您找到的相关学习资源：\n\n");

    for (int i = 0; i < Math.min(citations.size(), 5); i++) {
      Citation citation = citations.get(i);
      answer.append(String.format("%d. %s (相似度：%.2f)\n", i + 1,
          citation.getResourceTitle(), citation.getScore()));
      answer.append(String.format("   位置：%s\n", citation.getLocation()));
      answer.append(String.format("   摘要：%s\n\n", citation.getSnippet()));
    }

    answer.append("建议您查阅以上资源获取更多信息。");
    return answer.toString();
  }

  /**
   * 构建降级资源列表（LLM 不可用时使用）。
   *
   * @param citations 检索结果列表
   * @return 资源简略信息列表
   */
  private List<ResourceBrief> buildFallbackResources(List<Citation> citations) {
    return citations.stream()
        .map(citation -> ResourceBrief.builder()
            .id(citation.getResourceId())
            .title(citation.getResourceTitle())
            .build())
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * 异步记录查询行为。
   *
   * <p>此方法不阻塞主流程，在后台线程中执行。
   *
   * @param userId 用户 ID
   * @param queryText 查询文本
   */
  @Async
  public void recordQueryBehaviorAsync(Long userId, String queryText) {
    try {
      behaviorService.recordQuery(userId, queryText, null);
      log.debug("查询行为记录成功：userId={}, query='{}'", userId, queryText);
    } catch (Exception e) {
      log.error("查询行为记录失败：userId={}, query='{}', error={}", userId, queryText, e.getMessage());
    }
  }
}
