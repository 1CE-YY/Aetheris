/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.mapper;

import com.aetheris.rag.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户行为数据访问接口
 *
 * <p>提供用户行为记录的增删改查操作，用于支持：
 * <ul>
 *   <li>记录用户查询行为</li>
 *   <li>记录用户点击行为</li>
 *   <li>记录用户收藏行为</li>
 *   <li>查询用户最近的行为记录</li>
 * </ul>
 *
 * <p>符合 Google Java Style Guide：
 * <ul>
 *   <li>方法名使用 camelCase</li>
 *   <li>参数使用 @Param 注解</li>
 *   <li>返回类型明确</li>
 *   <li>SQL 语句定义在对应的 UserBehaviorMapper.xml 文件中</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Mapper
public interface UserBehaviorMapper {

    /**
     * 插入用户行为记录
     *
     * @param behavior 用户行为实体
     * @return 影响行数（1 表示插入成功，0 表示插入失败）
     */
    int insert(UserBehavior behavior);

    /**
     * 查询用户最近的查询行为（按时间倒序）
     *
     * <p>用于用户画像更新时获取最近 N 次查询记录
     *
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 查询行为列表，按行为时间倒序排列
     */
    java.util.List<UserBehavior> findRecentQueries(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户在指定时间之后的所有行为
     *
     * <p>用于分析用户在某个时间范围内的行为序列
     *
     * @param userId 用户ID
     * @param startTime 起始时间
     * @return 行为列表，按行为时间倒序排列
     */
    java.util.List<UserBehavior> findByUserIdAndTimeRange(@Param("userId") Long userId,
                                                           @Param("startTime") java.time.Instant startTime);

    /**
     * 统计用户指定类型的的行为数量
     *
     * <p>用于统计用户的查询、点击、收藏次数
     *
     * @param userId 用户ID
     * @param type 行为类型（QUERY、CLICK、FAVORITE）
     * @return 行为数量
     */
    int countByType(@Param("userId") Long userId, @Param("type") UserBehavior.BehaviorType type);

    /**
     * 查询用户最近的所有行为（按时间倒序）
     *
     * <p>用于个人中心展示用户最近的行为历史
     *
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 所有类型的行为列表，按行为时间倒序排列
     */
    java.util.List<UserBehavior> findRecentBehaviors(@Param("userId") Long userId, @Param("limit") int limit);
}
