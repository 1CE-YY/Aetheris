/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.model.UserBehavior;

import java.util.List;

/**
 * 用户行为服务接口
 *
 * <p>提供用户行为记录的增删改查操作，支持：
 * <ul>
 *   <li>记录查询行为（FR-003）</li>
 *   <li>记录点击/收藏行为（FR-004）</li>
 *   <li>查询用户最近的行为记录</li>
 *   <li>统计用户行为数量</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public interface BehaviorService {

    /**
     * 记录查询行为
     *
     * <p>当用户在问答界面输入问题时，调用此方法记录查询行为。
     * 查询行为将用于构建用户画像和个性化推荐。
     *
     * @param userId 用户ID
     * @param queryText 查询文本
     * @param sessionId 会话ID（可选，用于关联同一会话中的多个行为）
     * @return 创建的行为记录ID
     */
    Long recordQuery(Long userId, String queryText, String sessionId);

    /**
     * 记录点击行为
     *
     * <p>当用户点击某个资源（如查看资源详情、点击引用）时，调用此方法记录点击行为。
     * 点击行为将用于构建用户画像，权重高于查询行为。
     *
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @param sessionId 会话ID（可选）
     * @return 创建的行为记录ID
     */
    Long recordClick(Long userId, Long resourceId, String sessionId);

    /**
     * 记录收藏行为
     *
     * <p>当用户收藏某个资源时，调用此方法记录收藏行为。
     * 收藏行为将用于构建用户画像，权重最高。
     *
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @param sessionId 会话ID（可选）
     * @return 创建的行为记录ID
     */
    Long recordFavorite(Long userId, Long resourceId, String sessionId);

    /**
     * 查询用户最近的行为记录
     *
     * <p>用于个人中心展示用户最近的行为历史
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（默认 10 条）
     * @return 最近的行为列表，按行为时间倒序排列
     */
    List<UserBehavior> getRecentBehaviors(Long userId, int limit);

    /**
     * 查询用户最近的查询行为
     *
     * <p>用于用户画像更新时获取最近 N 次查询记录
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（默认 10 条）
     * @return 最近的查询行为列表，按行为时间倒序排列
     */
    List<UserBehavior> getRecentQueries(Long userId, int limit);

    /**
     * 统计用户指定类型的的行为数量
     *
     * <p>用于统计用户的查询、点击、收藏次数
     *
     * @param userId 用户ID
     * @param behaviorType 行为类型（QUERY、CLICK、FAVORITE）
     * @return 行为数量
     */
    int countByType(Long userId, UserBehavior.BehaviorType behaviorType);

    /**
     * 生成会话ID
     *
     * <p>为新的用户会话生成唯一的会话标识符
     *
     * @return 会话ID（UUID 格式）
     */
    String generateSessionId();
}
