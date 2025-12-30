/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户行为实体类
 *
 * <p>用于记录用户在系统中的各种行为，包括查询、点击、收藏等。
 * 这些行为数据用于构建用户画像和个性化推荐。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior {

    /**
     * 行为ID（主键）
     */
    private Long id;

    /**
     * 用户ID（外键，关联 users 表）
     */
    private Long userId;

    /**
     * 行为类型枚举
     */
    public enum BehaviorType {
        /**
         * 查询行为
         */
        QUERY,

        /**
         * 点击行为
         */
        CLICK,

        /**
         * 收藏行为
         */
        FAVORITE
    }

    /**
     * 行为类型（QUERY/CLICK/FAVORITE）
     */
    private BehaviorType behaviorType;

    /**
     * 资源ID（外键，关联 resources 表）
     * <p>仅在 CLICK 和 FAVORITE 行为中有值
     */
    private Long resourceId;

    /**
     * 查询文本
     * <p>仅在 QUERY 行为中有值，记录用户的查询内容
     */
    private String queryText;

    /**
     * 行为发生时间
     */
    private Instant behaviorTime;

    /**
     * 会话ID
     * <p>用于关联同一会话中的多个行为，便于分析用户行为序列
     */
    private String sessionId;

    /**
     * 行为权重（用于用户画像）
     * <p>默认值：查询=1.0, 点击=2.0, 收藏=3.0
     */
    private Double weight;
}
