/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行为统计响应 DTO
 *
 * <p>用于返回用户行为统计信息
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorCountResponse {

    /**
     * 行为类型（QUERY/CLICK/FAVORITE）
     */
    private String behaviorType;

    /**
     * 行为数量
     */
    private Integer count;
}
