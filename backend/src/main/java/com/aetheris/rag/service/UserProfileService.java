/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.entity.UserProfile;

/**
 * 用户画像服务接口。
 *
 * <p>提供用户画像的管理和计算功能，支持：
 * <ul>
 *   <li>获取用户画像信息</li>
 *   <li>基于最近行为计算画像向量（滑动平均）</li>
 *   <li>获取画像向量用于推荐服务</li>
 *   <li>画像预热（资源上传后触发）</li>
 *   <li>更新行为计数</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
public interface UserProfileService {

  /**
   * 获取用户画像信息。
   *
   * @param userId 用户ID
   * @return 用户画像实体，不存在则返回 null
   */
  UserProfile getProfile(Long userId);

  /**
   * 计算并更新用户画像向量。
   *
   * <p>基于用户最近 N 次行为的 embedding 加权平均计算画像向量。
   * 行为类型包括查询（QUERY）、点击（CLICK）、收藏（FAVORITE），
   * 各类型的权重可通过配置文件调整。
   *
   * <p>如果用户没有行为记录，不会创建画像。
   *
   * @param userId 用户ID
   */
  void updateProfile(Long userId);

  /**
   * 获取用户的画像向量（用于推荐服务检索）。
   *
   * <p>如果用户没有画像或画像向量为空，返回 null。
   *
   * @param userId 用户ID
   * @return 画像向量数组，无画像时返回 null
   */
  float[] getProfileVector(Long userId);

  /**
   * 预热用户画像（异步调用）。
   *
   * <p>在资源上传后触发，如果用户尚无画像，则基于已有行为创建初始画像。
   * 如果用户已有画像，此方法不执行任何操作。
   *
   * @param userId 用户ID
   */
  void prewarmProfile(Long userId);

  /**
   * 更新用户的行为计数（查询、点击、收藏次数）。
   *
   * @param userId 用户ID
   */
  void updateCounts(Long userId);
}
