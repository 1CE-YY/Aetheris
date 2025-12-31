/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import com.aetheris.rag.entity.UserBehavior;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户行为响应 DTO
 *
 * <p>用于返回用户行为记录的详细信息，不直接暴露实体类
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorResponse {

    /**
     * 行为ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 行为类型（QUERY/CLICK/FAVORITE）
     */
    private String behaviorType;

    /**
     * 资源ID（仅 CLICK 和 FAVORITE 行为有值）
     */
    private Long resourceId;

    /**
     * 查询文本（仅 QUERY 行为有值）
     */
    private String queryText;

    /**
     * 行为发生时间
     */
    private Instant behaviorTime;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 行为权重
     */
    private Double weight;

    /**
     * 从实体类转换为 DTO
     *
     * @param behavior 用户行为实体
     * @return 行为响应 DTO
     */
    public static BehaviorResponse fromEntity(UserBehavior behavior) {
        return BehaviorResponse.builder()
                .id(behavior.getId())
                .userId(behavior.getUserId())
                .behaviorType(behavior.getBehaviorType() != null ? behavior.getBehaviorType().name() : null)
                .resourceId(behavior.getResourceId())
                .queryText(behavior.getQueryText())
                .behaviorTime(behavior.getBehaviorTime())
                .sessionId(behavior.getSessionId())
                .weight(behavior.getWeight())
                .build();
    }
}
