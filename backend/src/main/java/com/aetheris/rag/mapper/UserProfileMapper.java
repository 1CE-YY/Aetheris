/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.mapper;

import com.aetheris.rag.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户画像数据访问接口。
 *
 * <p>提供用户画像的增删改查操作，用于支持：
 * <ul>
 *   <li>画像向量的创建和更新（UPSERT）</li>
 *   <li>根据用户ID查询画像</li>
 *   <li>更新行为计数</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@Mapper
public interface UserProfileMapper {

  /**
   * 插入或更新用户画像（UPSERT）。
   *
   * <p>如果 user_id 已存在，则更新 profile_vector、window_size 和 updated_at；
   * 否则插入新记录。
   *
   * @param userProfile 用户画像实体
   * @return 影响行数（1 表示成功）
   */
  int upsert(UserProfile userProfile);

  /**
   * 根据用户ID查询画像。
   *
   * @param userId 用户ID
   * @return 用户画像实体，不存在则返回 null
   */
  UserProfile findByUserId(@Param("userId") Long userId);

  /**
   * 更新用户的行为计数。
   *
   * @param userId 用户ID
   * @param queryCount 查询次数
   * @param clickCount 点击次数
   * @param favoriteCount 收藏次数
   * @return 影响行数
   */
  int updateCounts(@Param("userId") Long userId,
                   @Param("queryCount") int queryCount,
                   @Param("clickCount") int clickCount,
                   @Param("favoriteCount") int favoriteCount);

  /**
   * 仅更新画像向量。
   *
   * @param userId 用户ID
   * @param profileVector 画像向量（JSON 数组字符串）
   * @return 影响行数
   */
  int updateProfileVector(@Param("userId") Long userId,
                          @Param("profileVector") String profileVector);
}
