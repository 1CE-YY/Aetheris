/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.mapper.UserBehaviorMapper;
import com.aetheris.rag.entity.UserBehavior;
import com.aetheris.rag.service.BehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 用户行为服务实现
 *
 * <p>提供用户行为记录的增删改查操作，支持：
 * <ul>
 *   <li>记录查询行为（FR-003）</li>
 *   <li>记录点击/收藏行为（FR-004）</li>
 *   <li>查询用户最近的行为记录</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorServiceImpl implements BehaviorService {

    private final UserBehaviorMapper userBehaviorMapper;

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
    @Override
    @Transactional
    public Long recordQuery(Long userId, String queryText, String sessionId) {
        log.debug("记录用户查询行为: userId={}, queryText={}", userId, queryText);

        UserBehavior behavior = UserBehavior.builder()
                .userId(userId)
                .behaviorType(UserBehavior.BehaviorType.QUERY)
                .queryText(queryText)
                .behaviorTime(Instant.now())
                .sessionId(sessionId)
                .build();

        userBehaviorMapper.insert(behavior);
        log.info("查询行为已记录: behaviorId={}, userId={}", behavior.getId(), userId);

        return behavior.getId();
    }

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
    @Override
    @Transactional
    public Long recordClick(Long userId, Long resourceId, String sessionId) {
        log.debug("记录用户点击行为: userId={}, resourceId={}", userId, resourceId);

        UserBehavior behavior = UserBehavior.builder()
                .userId(userId)
                .behaviorType(UserBehavior.BehaviorType.CLICK)
                .resourceId(resourceId)
                .behaviorTime(Instant.now())
                .sessionId(sessionId)
                .build();

        userBehaviorMapper.insert(behavior);
        log.info("点击行为已记录: behaviorId={}, userId={}, resourceId={}", behavior.getId(), userId, resourceId);

        return behavior.getId();
    }

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
    @Override
    @Transactional
    public Long recordFavorite(Long userId, Long resourceId, String sessionId) {
        log.debug("记录用户收藏行为: userId={}, resourceId={}", userId, resourceId);

        UserBehavior behavior = UserBehavior.builder()
                .userId(userId)
                .behaviorType(UserBehavior.BehaviorType.FAVORITE)
                .resourceId(resourceId)
                .behaviorTime(Instant.now())
                .sessionId(sessionId)
                .build();

        userBehaviorMapper.insert(behavior);
        log.info("收藏行为已记录: behaviorId={}, userId={}, resourceId={}", behavior.getId(), userId, resourceId);

        return behavior.getId();
    }

    /**
     * 查询用户最近的行为记录
     *
     * <p>用于个人中心展示用户最近的行为历史
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（默认 10 条）
     * @return 最近的行为列表，按行为时间倒序排列
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserBehavior> getRecentBehaviors(Long userId, int limit) {
        log.debug("查询用户最近行为: userId={}, limit={}", userId, limit);

        List<UserBehavior> behaviors = userBehaviorMapper.findRecentBehaviors(userId, limit);
        log.info("查询到 {} 条最近行为记录: userId={}", behaviors.size(), userId);

        return behaviors;
    }

    /**
     * 查询用户最近的查询行为
     *
     * <p>用于用户画像更新时获取最近 N 次查询记录
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（默认 10 条）
     * @return 最近的查询行为列表，按行为时间倒序排列
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserBehavior> getRecentQueries(Long userId, int limit) {
        log.debug("查询用户最近查询: userId={}, limit={}", userId, limit);

        List<UserBehavior> queries = userBehaviorMapper.findRecentQueries(userId, limit);
        log.info("查询到 {} 条最近查询记录: userId={}", queries.size(), userId);

        return queries;
    }

    /**
     * 统计用户指定类型的的行为数量
     *
     * <p>用于统计用户的查询、点击、收藏次数
     *
     * @param userId 用户ID
     * @param behaviorType 行为类型（QUERY、CLICK、FAVORITE）
     * @return 行为数量
     */
    @Override
    @Transactional(readOnly = true)
    public int countByType(Long userId, UserBehavior.BehaviorType behaviorType) {
        log.debug("统计用户行为数量: userId={}, behaviorType={}", userId, behaviorType);

        int count = userBehaviorMapper.countByType(userId, behaviorType);
        log.info("用户行为统计: userId={}, behaviorType={}, count={}", userId, behaviorType, count);

        return count;
    }

    /**
     * 生成会话ID
     *
     * <p>为新的用户会话生成唯一的会话标识符
     *
     * @return 会话ID（UUID 格式）
     */
    @Override
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
