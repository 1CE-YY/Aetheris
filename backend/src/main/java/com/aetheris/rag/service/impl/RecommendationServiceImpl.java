/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.response.Citation;
import com.aetheris.rag.dto.response.RecommendationItem;
import com.aetheris.rag.dto.response.RecommendationResponse;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.entity.UserBehavior;
import com.aetheris.rag.gateway.ChatGateway;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.service.BehaviorService;
import com.aetheris.rag.service.RecommendationService;
import com.aetheris.rag.service.SearchService;
import com.aetheris.rag.service.UserProfileService;
import com.aetheris.rag.util.PerformanceTimer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 推荐服务实现类。
 *
 * <p>实现个性化推荐和默认推荐两种策略：
 *
 * <ul>
 *   <li><b>个性化推荐</b>：基于用户画像向量进行 KNN 检索，调用 LLM 生成推荐理由和学习建议
 *   <li><b>默认推荐</b>：新用户无画像时，按资源上传时间返回热门资源
 * </ul>
 *
 * <p>推荐流程：
 * <ol>
 *   <li>获取用户画像向量（如有）</li>
 *   <li>使用画像向量 KNN 检索 → 按资源聚合 → 取 Top-N</li>
 *   <li>为每个推荐资源调用 ChatGateway 生成推荐理由和学习建议</li>
 *   <li>降级处理：LLM 失败时返回固定理由</li>
 * </ol>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

  private final UserProfileService userProfileService;
  private final SearchService searchService;
  private final ChatGateway chatGateway;
  private final BehaviorService behaviorService;
  private final ResourceMapper resourceMapper;

  /** 默认推荐数量 */
  @Value("${recommendation.top-n:10}")
  private int defaultTopN;

  /** 是否启用推荐功能 */
  @Value("${recommendation.enabled:true}")
  private boolean recommendationEnabled;

  /** 是否启用画像功能 */
  @Value("${profile.enabled:true}")
  private boolean profileEnabled;

  /** 相似度阈值（低于此值的推荐结果不纳入） */
  @Value("${rag.retrieval.score-threshold:0.4}")
  private double scoreThreshold;

  /** 推荐理由生成使用的系统提示 */
  private static final String REASON_SYSTEM_PROMPT =
      "你是一个学习推荐助手。根据用户的学习兴趣和推荐资源的相关内容，"
          + "生成简洁的推荐理由和学习建议。只输出推荐理由和学习建议，不要输出其他内容。";

  /** 默认推荐理由 */
  private static final String DEFAULT_REASON = "热门学习资源";

  /** 默认学习建议 */
  private static final String DEFAULT_SUGGESTION = "建议浏览资源目录，了解整体内容结构";

  /** LLM 失败时的降级理由 */
  private static final String FALLBACK_REASON = "该资源与您的学习兴趣相关";

  @Override
  public RecommendationResponse recommend(Long userId, int topN) {
    if (!recommendationEnabled) {
      log.warn("推荐功能已禁用：userId={}", userId);
      return RecommendationResponse.builder()
          .items(List.of())
          .personalized(false)
          .latencyMs(0)
          .count(0)
          .build();
    }

    PerformanceTimer timer = new PerformanceTimer();
    log.info("开始生成推荐：userId={}, topN={}", userId, topN);

    // 1. 获取用户画像向量
    timer.recordStage("profile");
    float[] profileVector = profileEnabled ? userProfileService.getProfileVector(userId) : null;

    // 懒加载：无画像时尝试从已有行为构建画像
    if (profileVector == null && profileEnabled) {
      log.info("尝试懒加载画像：userId={}", userId);
      try {
        userProfileService.updateProfile(userId);
        profileVector = userProfileService.getProfileVector(userId);
      } catch (Exception e) {
        log.warn("懒加载画像失败：userId={}, error={}", userId, e.getMessage());
      }
    }

    boolean personalized = profileVector != null;
    timer.endStage();

    List<RecommendationItem> items;

    if (personalized) {
      // 2a. 个性化推荐路径
      items = generatePersonalizedRecommendations(userId, profileVector, topN, timer);
    } else {
      // 2b. 默认推荐路径
      items = generateDefaultRecommendations(topN);
    }

    long totalLatency = timer.getElapsedMs();

    log.info("推荐生成完成：userId={}, personalized={}, count={}, latencyMs={}",
        userId, personalized, items.size(), totalLatency);

    return RecommendationResponse.builder()
        .items(items)
        .personalized(personalized)
        .latencyMs(totalLatency)
        .count(items.size())
        .build();
  }

  @Override
  @Async
  public void recordClickAsync(Long userId, Long resourceId) {
    try {
      behaviorService.recordClick(userId, resourceId, null);
      // 点击后异步更新画像
      userProfileService.updateProfile(userId);
      log.debug("推荐点击行为记录成功：userId={}, resourceId={}", userId, resourceId);
    } catch (Exception e) {
      log.error("推荐点击行为记录失败：userId={}, resourceId={}, error={}",
          userId, resourceId, e.getMessage());
    }
  }

  @Override
  @Async
  public void recordFavoriteAsync(Long userId, Long resourceId) {
    try {
      behaviorService.recordFavorite(userId, resourceId, null);
      // 收藏后异步更新画像
      userProfileService.updateProfile(userId);
      log.debug("推荐收藏行为记录成功：userId={}, resourceId={}", userId, resourceId);
    } catch (Exception e) {
      log.error("推荐收藏行为记录失败：userId={}, resourceId={}, error={}",
          userId, resourceId, e.getMessage());
    }
  }

  /**
   * 生成个性化推荐。
   *
   * <p>使用画像向量进行 KNN 检索，按资源聚合后为每个推荐资源生成理由和建议。
   *
   * @param userId 用户ID
   * @param profileVector 画像向量
   * @param topN 推荐数量
   * @param timer 性能计时器
   * @return 推荐项列表
   */
  private List<RecommendationItem> generatePersonalizedRecommendations(
      Long userId, float[] profileVector, int topN, PerformanceTimer timer) {

    // 1. 使用画像向量进行 KNN 检索并按资源聚合
    timer.recordStage("retrieval");
    List<Citation> citations =
        searchService.searchByVectorAggregated(profileVector, topN * 2);
    timer.endStage();

    log.info("画像 KNN 检索完成：找到 {} 个切片引用", citations.size());

    if (citations.isEmpty()) {
      log.info("画像检索无结果，回退到默认推荐");
      return generateDefaultRecommendations(topN);
    }

    // 2. 按资源ID聚合引用（保留每个资源的 top 3 引用）
    Map<String, List<Citation>> citationsByResource = citations.stream()
        .sorted(Comparator.comparingDouble(Citation::getScore).reversed())
        .collect(Collectors.groupingBy(
            Citation::getResourceId,
            LinkedHashMap::new,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.stream().limit(3).toList())));

    // 3. 获取最近查询文本（用于推荐理由生成）
    timer.recordStage("generation");
    List<String> recentQueries = getRecentQueryTexts(userId, 5);

    // 4. 为每个资源构建推荐项
    List<RecommendationItem> items = new ArrayList<>();
    for (Map.Entry<String, List<Citation>> entry : citationsByResource.entrySet()) {
      String resourceId = entry.getKey();
      List<Citation> resourceCitations = entry.getValue();

      if (resourceCitations.isEmpty()) {
        continue;
      }

      // 获取资源信息
      Resource resource = resourceMapper.findById(Long.parseLong(resourceId));
      if (resource == null) {
        log.warn("推荐资源不存在，跳过：resourceId={}", resourceId);
        continue;
      }

      // 取最高相似度分数
      double bestScore = resourceCitations.stream()
          .mapToDouble(Citation::getScore)
          .max()
          .orElse(0.0);

      // 低于阈值跳过
      if (bestScore < scoreThreshold) {
        log.debug("推荐资源相似度过低，跳过：resourceId={}, score={}", resourceId, bestScore);
        continue;
      }

      // 生成推荐理由和学习建议
      String[] reasonAndSuggestion = generateReasonAndSuggestion(
          recentQueries, resource, resourceCitations);

      RecommendationItem item = RecommendationItem.builder()
          .resourceId(resourceId)
          .title(resource.getTitle())
          .tags(resource.getTags())
          .reason(reasonAndSuggestion[0])
          .suggestion(reasonAndSuggestion[1])
          .citations(resourceCitations)
          .score(bestScore)
          .fileType(resource.getFileType())
          .description(resource.getDescription())
          .build();

      items.add(item);

      // 达到 topN 即停止
      if (items.size() >= topN) {
        break;
      }
    }
    timer.endStage();

    return items;
  }

  /**
   * 生成默认推荐（无画像时的回退策略）。
   *
   * <p>按资源上传时间倒序返回最近的资源。
   *
   * @param topN 推荐数量
   * @return 推荐项列表
   */
  private List<RecommendationItem> generateDefaultRecommendations(int topN) {
    log.info("生成默认推荐（无画像）：topN={}", topN);

    List<Resource> resources = resourceMapper.findPaged(0, topN);

    return resources.stream()
        .map(resource -> RecommendationItem.builder()
            .resourceId(resource.getId().toString())
            .title(resource.getTitle())
            .tags(resource.getTags())
            .reason(DEFAULT_REASON)
            .suggestion(DEFAULT_SUGGESTION)
            .citations(List.of())
            .score(0.0)
            .fileType(resource.getFileType())
            .description(resource.getDescription())
            .build())
        .toList();
  }

  /**
   * 调用 ChatGateway 生成推荐理由和学习建议。
   *
   * <p>Prompt 模板：
   * <pre>
   * 用户最近的查询兴趣：{recentQueries}
   * 推荐资源：{resourceTitle}
   * 相关证据：{evidence_chunks}
   * 请生成：1. 推荐理由 2. 学习建议
   * </pre>
   *
   * <p>LLM 失败时降级为固定理由。
   *
   * @param recentQueries 最近查询文本列表
   * @param resource 推荐资源
   * @param citations 证据引用列表
   * @return [推荐理由, 学习建议]
   */
  private String[] generateReasonAndSuggestion(
      List<String> recentQueries, Resource resource, List<Citation> citations) {

    try {
      // 构建用户 Prompt
      StringBuilder userPrompt = new StringBuilder();

      // 最近查询兴趣
      if (!recentQueries.isEmpty()) {
        userPrompt.append("用户最近的查询兴趣：").append(String.join("、", recentQueries)).append("\n\n");
      }

      // 推荐资源信息
      userPrompt.append("推荐资源：").append(resource.getTitle()).append("\n");
      if (resource.getTags() != null && !resource.getTags().isEmpty()) {
        userPrompt.append("资源标签：").append(resource.getTags()).append("\n");
      }

      // 相关证据
      userPrompt.append("\n相关证据：\n");
      for (int i = 0; i < citations.size(); i++) {
        Citation citation = citations.get(i);
        userPrompt.append(String.format("%d. [位置: %s, 相似度: %.2f] %s\n",
            i + 1, citation.getLocation(), citation.getScore(), citation.getSnippet()));
      }

      userPrompt.append("\n请生成：\n");
      userPrompt.append("1. 推荐理由（1-2 句话，说明为何推荐此资源）\n");
      userPrompt.append("2. 学习建议（建议先学习哪个章节或内容）\n");
      userPrompt.append("请使用简洁的中文回答。");

      // 调用 LLM
      String llmResponse = chatGateway.chat(REASON_SYSTEM_PROMPT, userPrompt.toString());

      // 解析 LLM 响应（简单分割：取前两段作为理由和建议）
      return parseReasonAndSuggestion(llmResponse);

    } catch (Exception e) {
      log.warn("推荐理由生成失败，使用降级理由：resourceId={}, error={}",
          resource.getId(), e.getMessage());
      return new String[]{FALLBACK_REASON, DEFAULT_SUGGESTION};
    }
  }

  /**
   * 解析 LLM 返回的推荐理由和学习建议。
   *
   * <p>尝试按换行符分割，提取推荐理由和学习建议两部分。
   * 如果解析失败，将整个响应作为推荐理由。
   *
   * @param llmResponse LLM 原始响应
   * @return [推荐理由, 学习建议]
   */
  private String[] parseReasonAndSuggestion(String llmResponse) {
    if (llmResponse == null || llmResponse.isBlank()) {
      return new String[]{FALLBACK_REASON, DEFAULT_SUGGESTION};
    }

    String reason = llmResponse.trim();
    String suggestion = DEFAULT_SUGGESTION;

    // 尝试按"学习建议"关键字分割
    String[] parts = reason.split("(?=学习建议|建议.*[:：])", 2);
    if (parts.length == 2) {
      reason = parts[0].trim();
      suggestion = parts[1].trim();
      // 去除可能的前缀
      suggestion = suggestion.replaceFirst("^(学习建议|建议)[:：]?\\s*", "");
    }

    // 清理推荐理由中可能的"推荐理由"前缀
    reason = reason.replaceFirst("^推荐理由[:：]?\\s*", "");
    // 去除编号前缀
    reason = reason.replaceFirst("^\\d+[.、)]\\s*", "");
    suggestion = suggestion.replaceFirst("^\\d+[.、)]\\s*", "");

    // 清理末尾残留的编号（如 "\n2."）
    reason = reason.replaceAll("[\\r\\n]+\\d+[.、)]\\s*$", "");
    suggestion = suggestion.replaceAll("[\\r\\n]+\\d+[.、)]\\s*$", "");

    return new String[]{reason, suggestion};
  }

  /**
   * 获取用户最近查询文本列表。
   *
   * @param userId 用户ID
   * @param limit 数量限制
   * @return 查询文本列表
   */
  private List<String> getRecentQueryTexts(Long userId, int limit) {
    try {
      List<UserBehavior> queries = behaviorService.getRecentQueries(userId, limit);
      return queries.stream()
          .map(UserBehavior::getQueryText)
          .filter(text -> text != null && !text.isEmpty())
          .distinct()
          .limit(limit)
          .toList();
    } catch (Exception e) {
      log.warn("获取最近查询失败：userId={}, error={}", userId, e.getMessage());
      return List.of();
    }
  }
}
