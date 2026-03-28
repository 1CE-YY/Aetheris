/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.entity.UserBehavior;
import com.aetheris.rag.entity.UserProfile;
import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.mapper.UserProfileMapper;
import com.aetheris.rag.service.BehaviorService;
import com.aetheris.rag.service.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户画像服务实现类。
 *
 * <p>基于用户最近 N 次行为的 embedding 向量计算加权平均，生成用户画像向量。
 * 画像向量用于个性化推荐服务，通过 Redis KNN 搜索召回与用户兴趣最匹配的资源。
 *
 * <p>画像更新策略：
 * <ul>
 *   <li>每次查询后异步更新画像（滑动窗口）</li>
 *   <li>点击行为权重 2.0，收藏行为权重 3.0（可配置）</li>
 *   <li>资源上传后可触发画像预热</li>
 * </ul>
 *
 * <p>画像计算算法（加权滑动平均）：
 * <ol>
 *   <li>从 BehaviorService 获取最近 windowSize 次行为</li>
 *   <li>对 QUERY 行为：embed(queryText)，权重 = queryWeight</li>
 *   <li>对 CLICK 行为：embed(resourceTitle)，权重 = clickWeight</li>
 *   <li>对 FAVORITE 行为：embed(resourceTitle)，权重 = favoriteWeight</li>
 *   <li>对每个维度 i：profileVector[i] = sum(weight_j * embedding_j[i]) / sum(weight_j)</li>
 *   <li>序列化为 JSON 数组存入 user_profiles 表</li>
 * </ol>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

  private final UserProfileMapper userProfileMapper;
  private final BehaviorService behaviorService;
  private final EmbeddingGateway embeddingGateway;
  private final ResourceMapper resourceMapper;
  private final ObjectMapper objectMapper;

  /** 行为窗口大小（最近 N 次行为参与画像计算） */
  @Value("${profile.window-size:10}")
  private int windowSize;

  /** 查询行为权重 */
  @Value("${profile.query-weight:1.0}")
  private double queryWeight;

  /** 点击行为权重 */
  @Value("${profile.click-weight:2.0}")
  private double clickWeight;

  /** 收藏行为权重 */
  @Value("${profile.favorite-weight:3.0}")
  private double favoriteWeight;

  /** 是否启用画像功能 */
  @Value("${profile.enabled:true}")
  private boolean profileEnabled;

  @Override
  public UserProfile getProfile(Long userId) {
    log.debug("获取用户画像：userId={}", userId);
    return userProfileMapper.findByUserId(userId);
  }

  @Override
  @Transactional
  public void updateProfile(Long userId) {
    if (!profileEnabled) {
      log.debug("画像功能已禁用，跳过更新：userId={}", userId);
      return;
    }

    log.info("开始更新用户画像：userId={}, windowSize={}", userId, windowSize);

    // 1. 获取最近 N 次行为（混合类型）
    List<UserBehavior> recentBehaviors =
        behaviorService.getRecentBehaviors(userId, windowSize);

    if (recentBehaviors.isEmpty()) {
      log.info("用户无行为记录，跳过画像更新：userId={}", userId);
      return;
    }

    // 2. 计算加权平均画像向量
    float[] profileVector = computeWeightedAverageVector(recentBehaviors);

    if (profileVector == null) {
      log.warn("画像向量计算失败（可能所有行为的文本都为空）：userId={}", userId);
      return;
    }

    // 3. 序列化为 JSON 数组
    String profileVectorJson = serializeVector(profileVector);

    // 4. 保存或更新画像
    UserProfile profile = UserProfile.builder()
        .userId(userId)
        .profileVector(profileVectorJson)
        .windowSize(windowSize)
        .build();

    userProfileMapper.upsert(profile);
    log.info("用户画像已更新：userId={}, vectorDimension={}, behaviorCount={}",
        userId, profileVector.length, recentBehaviors.size());

    // 5. 更新行为计数
    updateCounts(userId);
  }

  @Override
  public float[] getProfileVector(Long userId) {
    if (!profileEnabled) {
      log.debug("画像功能已禁用：userId={}", userId);
      return null;
    }

    UserProfile profile = userProfileMapper.findByUserId(userId);
    if (profile == null || profile.getProfileVector() == null || profile.getProfileVector().isEmpty()) {
      log.debug("用户无画像向量：userId={}", userId);
      return null;
    }

    return deserializeVector(profile.getProfileVector());
  }

  @Override
  public void prewarmProfile(Long userId) {
    if (!profileEnabled) {
      log.debug("画像功能已禁用，跳过预热：userId={}", userId);
      return;
    }

    // 仅在用户没有画像时预热
    UserProfile existingProfile = userProfileMapper.findByUserId(userId);
    if (existingProfile != null && existingProfile.getProfileVector() != null) {
      log.debug("用户已有画像，跳过预热：userId={}", userId);
      return;
    }

    log.info("预热用户画像：userId={}", userId);
    updateProfile(userId);
  }

  @Override
  public void updateCounts(Long userId) {
    int queryCount = behaviorService.countByType(userId, UserBehavior.BehaviorType.QUERY);
    int clickCount = behaviorService.countByType(userId, UserBehavior.BehaviorType.CLICK);
    int favoriteCount = behaviorService.countByType(userId, UserBehavior.BehaviorType.FAVORITE);

    userProfileMapper.updateCounts(userId, queryCount, clickCount, favoriteCount);
    log.debug("行为计数已更新：userId={}, queries={}, clicks={}, favorites={}",
        userId, queryCount, clickCount, favoriteCount);
  }

  /**
   * 计算行为的加权平均向量。
   *
   * <p>对每种行为类型：
   * <ul>
   *   <li>QUERY：使用 queryText 进行 embedding</li>
   *   <li>CLICK/FAVORITE：使用关联资源的标题进行 embedding</li>
   * </ul>
   *
   * <p>加权平均公式：profileVector[i] = sum(weight_j * embedding_j[i]) / sum(weight_j)
   *
   * @param behaviors 用户行为列表
   * @return 加权平均向量，如果无法计算则返回 null
   */
  private float[] computeWeightedAverageVector(List<UserBehavior> behaviors) {
    float[] weightedSum = null;
    double totalWeight = 0.0;

    for (UserBehavior behavior : behaviors) {
      double weight = getWeight(behavior);
      String textToEmbed = getEmbeddingText(behavior);

      if (textToEmbed == null || textToEmbed.isEmpty()) {
        log.debug("跳过无文本的行为：behaviorId={}, type={}", behavior.getId(), behavior.getBehaviorType());
        continue;
      }

      try {
        float[] embedding = embeddingGateway.embed(textToEmbed);

        if (weightedSum == null) {
          weightedSum = new float[embedding.length];
        }

        // 累加加权向量
        for (int i = 0; i < embedding.length; i++) {
          weightedSum[i] += (float) weight * embedding[i];
        }
        totalWeight += weight;

      } catch (Exception e) {
        log.warn("行为文本向量化失败，跳过：behaviorId={}, error={}",
            behavior.getId(), e.getMessage());
      }
    }

    if (weightedSum == null || totalWeight == 0.0) {
      return null;
    }

    // 计算加权平均
    float[] profileVector = new float[weightedSum.length];
    for (int i = 0; i < weightedSum.length; i++) {
      profileVector[i] = weightedSum[i] / (float) totalWeight;
    }

    return profileVector;
  }

  /**
   * 获取行为的权重。
   *
   * @param behavior 用户行为
   * @return 权重值
   */
  private double getWeight(UserBehavior behavior) {
    return switch (behavior.getBehaviorType()) {
      case QUERY -> queryWeight;
      case CLICK -> clickWeight;
      case FAVORITE -> favoriteWeight;
    };
  }

  /**
   * 获取用于 embedding 的文本。
   *
   * <p>QUERY 行为使用 queryText；CLICK/FAVORITE 行为使用关联资源的标题和描述。
   *
   * @param behavior 用户行为
   * @return 用于 embedding 的文本，无法获取时返回 null
   */
  private String getEmbeddingText(UserBehavior behavior) {
    return switch (behavior.getBehaviorType()) {
      case QUERY -> behavior.getQueryText();
      case CLICK, FAVORITE -> getResourceText(behavior.getResourceId());
    };
  }

  /**
   * 获取资源的文本表示（标题 + 描述），用于 embedding。
   *
   * @param resourceId 资源ID
   * @return 资源文本（标题 + 描述），资源不存在时返回 null
   */
  private String getResourceText(Long resourceId) {
    if (resourceId == null) {
      return null;
    }
    Resource resource = resourceMapper.findById(resourceId);
    if (resource == null) {
      return null;
    }

    StringBuilder text = new StringBuilder();
    if (resource.getTitle() != null) {
      text.append(resource.getTitle());
    }
    if (resource.getDescription() != null && !resource.getDescription().isEmpty()) {
      if (text.length() > 0) {
        text.append(" ");
      }
      text.append(resource.getDescription());
    }

    return text.length() > 0 ? text.toString() : null;
  }

  /**
   * 将向量序列化为 JSON 数组字符串。
   *
   * @param vector 浮点数组
   * @return JSON 数组字符串（如 "[0.123, -0.456, ...]"）
   */
  private String serializeVector(float[] vector) {
    try {
      return objectMapper.writeValueAsString(vector);
    } catch (JsonProcessingException e) {
      log.error("向量序列化失败", e);
      throw new RuntimeException("向量序列化失败", e);
    }
  }

  /**
   * 从 JSON 数组字符串反序列化向量。
   *
   * @param json JSON 数组字符串
   * @return 浮点数组
   */
  private float[] deserializeVector(String json) {
    try {
      return objectMapper.readValue(json, float[].class);
    } catch (JsonProcessingException e) {
      log.error("向量反序列化失败：json长度={}", json.length(), e);
      return null;
    }
  }
}
