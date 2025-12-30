/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.integration;

import com.aetheris.rag.mapper.UserBehaviorMapper;
import com.aetheris.rag.model.UserBehavior;
import com.aetheris.rag.service.BehaviorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户行为集成测试
 *
 * <p>测试用户行为的完整生命周期，包括：
 * <ul>
 *   <li>记录查询、点击、收藏行为</li>
 *   <li>查询最近的行为记录</li>
 *   <li>统计行为数量</li>
 * </ul>
 *
 * <p>注意：这是集成测试，需要真实的数据库环境。
 * 使用 @Transactional 注解确保测试后回滚，不污染数据库。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserBehaviorIntegrationTest {

    @Autowired
    private BehaviorService behaviorService;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // 清理测试数据（可选，因为 @Transactional 会自动回滚）
    }

    @Test
    void testCompleteBehaviorLifecycle() {
        // 1. 记录查询行为
        Long queryBehaviorId = behaviorService.recordQuery(
                testUserId,
                "什么是深度学习？",
                "test-session-1"
        );

        assertNotNull(queryBehaviorId, "查询行为ID不应为空");

        // 2. 验证查询行为已记录
        List<UserBehavior> recentQueries = behaviorService.getRecentQueries(testUserId, 10);
        assertFalse(recentQueries.isEmpty(), "查询行为列表不应为空");
        assertEquals("什么是深度学习？", recentQueries.get(0).getQueryText());

        // 3. 记录点击行为
        Long clickBehaviorId = behaviorService.recordClick(
                testUserId,
                100L,
                "test-session-1"
        );

        assertNotNull(clickBehaviorId, "点击行为ID不应为空");

        // 4. 验证点击行为已记录
        int clickCount = behaviorService.countByType(testUserId, UserBehavior.BehaviorType.CLICK);
        assertEquals(1, clickCount, "点击行为数量应为 1");

        // 5. 记录收藏行为
        Long favoriteBehaviorId = behaviorService.recordFavorite(
                testUserId,
                100L,
                "test-session-1"
        );

        assertNotNull(favoriteBehaviorId, "收藏行为ID不应为空");

        // 6. 验证收藏行为已记录
        int favoriteCount = behaviorService.countByType(testUserId, UserBehavior.BehaviorType.FAVORITE);
        assertEquals(1, favoriteCount, "收藏行为数量应为 1");

        // 7. 查询所有最近的行为
        List<UserBehavior> recentBehaviors = behaviorService.getRecentBehaviors(testUserId, 10);
        assertTrue(recentBehaviors.size() >= 3, "最近行为列表应至少包含 3 条记录");

        // 8. 验证行为时间戳正确
        assertTrue(recentBehaviors.get(0).getBehaviorTime() != null);
    }

    @Test
    void testMultipleUsersIsolation() {
        // 用户1记录查询行为
        behaviorService.recordQuery(1L, "用户1的查询", "session-1");

        // 用户2记录查询行为
        behaviorService.recordQuery(2L, "用户2的查询", "session-2");

        // 验证用户1的查询记录
        List<UserBehavior> user1Queries = behaviorService.getRecentQueries(1L, 10);
        assertEquals(1, user1Queries.size());
        assertEquals("用户1的查询", user1Queries.get(0).getQueryText());

        // 验证用户2的查询记录
        List<UserBehavior> user2Queries = behaviorService.getRecentQueries(2L, 10);
        assertEquals(1, user2Queries.size());
        assertEquals("用户2的查询", user2Queries.get(0).getQueryText());
    }

    @Test
    void testBehaviorOrdering() {
        // 记录多个查询行为
        behaviorService.recordQuery(testUserId, "第一次查询", "session-1");
        behaviorService.recordQuery(testUserId, "第二次查询", "session-1");
        behaviorService.recordQuery(testUserId, "第三次查询", "session-1");

        // 验证查询行为按时间倒序排列
        List<UserBehavior> recentQueries = behaviorService.getRecentQueries(testUserId, 10);
        assertEquals(3, recentQueries.size());
        assertEquals("第三次查询", recentQueries.get(0).getQueryText());
        assertEquals("第二次查询", recentQueries.get(1).getQueryText());
        assertEquals("第一次查询", recentQueries.get(2).getQueryText());
    }

    @Test
    void testSessionIdGeneration() {
        // 生成多个会话ID
        String sessionId1 = behaviorService.generateSessionId();
        String sessionId2 = behaviorService.generateSessionId();
        String sessionId3 = behaviorService.generateSessionId();

        // 验证会话ID唯一
        assertNotEquals(sessionId1, sessionId2);
        assertNotEquals(sessionId2, sessionId3);
        assertNotEquals(sessionId1, sessionId3);

        // 验证会话ID格式（UUID）
        assertTrue(sessionId1.matches("^[a-f0-9\\-]{36}$"));
    }
}
