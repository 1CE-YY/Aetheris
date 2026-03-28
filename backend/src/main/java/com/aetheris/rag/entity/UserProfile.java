/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户画像实体类。
 *
 * <p>存储用户的兴趣向量画像，基于最近 N 次行为的 embedding 加权平均计算得出。
 * 画像向量用于个性化推荐，通过 Redis KNN 搜索召回相关资源。
 *
 * <p>画像更新策略：
 * <ul>
 *   <li>每次查询后异步更新画像向量（滑动窗口）</li>
 *   <li>点击/收藏行为权重更高（可配置）</li>
 *   <li>资源上传后可触发画像预热</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

  /** 画像ID（主键） */
  private Long id;

  /** 用户ID（唯一，外键关联 users 表） */
  private Long userId;

  /**
   * 画像向量（JSON 数组格式存储）。
   *
   * <p>存储为 JSON 字符串，例如 "[0.123, -0.456, 0.789, ...]"。
   * 向量维度与 Redis 向量索引一致（默认 2048）。
   */
  private String profileVector;

  /** 行为窗口大小（最近 N 次行为参与画像计算） */
  @Builder.Default
  private Integer windowSize = 10;

  /** 查询行为次数 */
  @Builder.Default
  private Integer queryCount = 0;

  /** 点击行为次数 */
  @Builder.Default
  private Integer clickCount = 0;

  /** 收藏行为次数 */
  @Builder.Default
  private Integer favoriteCount = 0;

  /** 画像更新时间 */
  private Instant updatedAt;
}
