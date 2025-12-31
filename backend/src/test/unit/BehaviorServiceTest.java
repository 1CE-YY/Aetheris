/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.unit;

import com.aetheris.rag.mapper.UserBehaviorMapper;
import com.aetheris.rag.entity.UserBehavior;
import com.aetheris.rag.service.BehaviorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BehaviorService 单元测试
 *
 * <p>测试用户行为服务的核心功能，包括：
 * <ul>
 *   <li>记录查询行为</li>
 *   <li>记录点击行为</li>
 *   <li>记录收藏行为</li>
 *   <li>查询最近的行为记录</li>
 *   <li>统计行为数量</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@ExtendWith(MockitoExtension.class)
class BehaviorServiceTest {

    @Mock
    private UserBehaviorMapper userBehaviorMapper;

    @InjectMocks
    private BehaviorService behaviorService;

    private UserBehavior mockBehavior;

    @BeforeEach
    void setUp() {
        // 初始化模拟的行为记录
        mockBehavior = UserBehavior.builder()
                .id(1L)
                .userId(1L)
                .behaviorType(UserBehavior.BehaviorType.QUERY)
                .queryText("什么是 RAG？")
                .behaviorTime(Instant.now())
                .sessionId("test-session-id")
                .build();
    }

    @Test
    void testRecordQuery_Success() {
        // Given
        Long userId = 1L;
        String queryText = "什么是 RAG？";
        String sessionId = "test-session-id";

        when(userBehaviorMapper.insert(any(UserBehavior.class))).thenAnswer(invocation -> {
            UserBehavior behavior = invocation.getArgument(0);
            behavior.setId(1L);
            return 1;
        });

        // When
        Long behaviorId = behaviorService.recordQuery(userId, queryText, sessionId);

        // Then
        assertNotNull(behaviorId);
        assertEquals(1L, behaviorId);

        verify(userBehaviorMapper, times(1)).insert(argThat(behavior ->
                behavior.getUserId().equals(userId) &&
                behavior.getBehaviorType() == UserBehavior.BehaviorType.QUERY &&
                behavior.getQueryText().equals(queryText) &&
                behavior.getSessionId().equals(sessionId)
        ));
    }

    @Test
    void testRecordClick_Success() {
        // Given
        Long userId = 1L;
        Long resourceId = 100L;
        String sessionId = "test-session-id";

        when(userBehaviorMapper.insert(any(UserBehavior.class))).thenAnswer(invocation -> {
            UserBehavior behavior = invocation.getArgument(0);
            behavior.setId(2L);
            return 1;
        });

        // When
        Long behaviorId = behaviorService.recordClick(userId, resourceId, sessionId);

        // Then
        assertNotNull(behaviorId);
        assertEquals(2L, behaviorId);

        verify(userBehaviorMapper, times(1)).insert(argThat(behavior ->
                behavior.getUserId().equals(userId) &&
                behavior.getBehaviorType() == UserBehavior.BehaviorType.CLICK &&
                behavior.getResourceId().equals(resourceId) &&
                behavior.getSessionId().equals(sessionId)
        ));
    }

    @Test
    void testRecordFavorite_Success() {
        // Given
        Long userId = 1L;
        Long resourceId = 100L;
        String sessionId = "test-session-id";

        when(userBehaviorMapper.insert(any(UserBehavior.class))).thenAnswer(invocation -> {
            UserBehavior behavior = invocation.getArgument(0);
            behavior.setId(3L);
            return 1;
        });

        // When
        Long behaviorId = behaviorService.recordFavorite(userId, resourceId, sessionId);

        // Then
        assertNotNull(behaviorId);
        assertEquals(3L, behaviorId);

        verify(userBehaviorMapper, times(1)).insert(argThat(behavior ->
                behavior.getUserId().equals(userId) &&
                behavior.getBehaviorType() == UserBehavior.BehaviorType.FAVORITE &&
                behavior.getResourceId().equals(resourceId) &&
                behavior.getSessionId().equals(sessionId)
        ));
    }

    @Test
    void testGetRecentBehaviors_Success() {
        // Given
        Long userId = 1L;
        int limit = 10;
        List<UserBehavior> mockBehaviors = List.of(mockBehavior);

        when(userBehaviorMapper.findRecentBehaviors(eq(userId), eq(limit)))
                .thenReturn(mockBehaviors);

        // When
        List<UserBehavior> behaviors = behaviorService.getRecentBehaviors(userId, limit);

        // Then
        assertNotNull(behaviors);
        assertEquals(1, behaviors.size());
        assertEquals(mockBehavior, behaviors.get(0));

        verify(userBehaviorMapper, times(1)).findRecentBehaviors(userId, limit);
    }

    @Test
    void testGetRecentQueries_Success() {
        // Given
        Long userId = 1L;
        int limit = 10;
        List<UserBehavior> mockQueries = List.of(mockBehavior);

        when(userBehaviorMapper.findRecentQueries(eq(userId), eq(limit)))
                .thenReturn(mockQueries);

        // When
        List<UserBehavior> queries = behaviorService.getRecentQueries(userId, limit);

        // Then
        assertNotNull(queries);
        assertEquals(1, queries.size());
        assertEquals(mockBehavior, queries.get(0));

        verify(userBehaviorMapper, times(1)).findRecentQueries(userId, limit);
    }

    @Test
    void testCountByType_Query() {
        // Given
        Long userId = 1L;
        UserBehavior.BehaviorType behaviorType = UserBehavior.BehaviorType.QUERY;
        int expectedCount = 5;

        when(userBehaviorMapper.countByType(eq(userId), eq(behaviorType)))
                .thenReturn(expectedCount);

        // When
        int count = behaviorService.countByType(userId, behaviorType);

        // Then
        assertEquals(expectedCount, count);

        verify(userBehaviorMapper, times(1)).countByType(userId, behaviorType);
    }

    @Test
    void testCountByType_Click() {
        // Given
        Long userId = 1L;
        UserBehavior.BehaviorType behaviorType = UserBehavior.BehaviorType.CLICK;
        int expectedCount = 3;

        when(userBehaviorMapper.countByType(eq(userId), eq(behaviorType)))
                .thenReturn(expectedCount);

        // When
        int count = behaviorService.countByType(userId, behaviorType);

        // Then
        assertEquals(expectedCount, count);

        verify(userBehaviorMapper, times(1)).countByType(userId, behaviorType);
    }

    @Test
    void testGenerateSessionId() {
        // When
        String sessionId1 = behaviorService.generateSessionId();
        String sessionId2 = behaviorService.generateSessionId();

        // Then
        assertNotNull(sessionId1);
        assertNotNull(sessionId2);
        assertNotEquals(sessionId1, sessionId2);
    }
}
