/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.common.response.ApiResponse;
import com.aetheris.rag.dto.response.BehaviorCountResponse;
import com.aetheris.rag.dto.response.BehaviorResponse;
import com.aetheris.rag.exception.BadRequestException;
import com.aetheris.rag.entity.UserBehavior;
import com.aetheris.rag.service.BehaviorService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户行为控制器
 *
 * <p>提供用户行为记录的查询接口，支持：
 * <ul>
 *   <li>查询用户最近的行为记录（用于个人中心）</li>
 *   <li>统计用户行为数量</li>
 * </ul>
 *
 * <p>注意：行为记录的插入是内部调用，
 * 由其他服务（如 ChatService、ResourceService）通过 BehaviorService 完成，
 * 不直接暴露 REST 接口。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@RestController
@RequestMapping("/api/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorService behaviorService;

    /**
     * 查询用户最近的行为记录。
     *
     * <p>用于个人中心展示用户最近的行为历史，包括查询、点击、收藏等行为。
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（可选，默认 10 条）
     * @return 最近的行为列表，按行为时间倒序排列
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<BehaviorResponse>>> getRecentBehaviors(
        @RequestParam Long userId, @RequestParam(defaultValue = "10") int limit) {

        log.info("查询用户最近行为: userId={}, limit={}", userId, limit);

        if (userId == null || userId <= 0) {
            log.warn("无效的用户ID: {}", userId);
            throw new BadRequestException("用户ID不能为空或小于等于0");
        }

        if (limit <= 0 || limit > 100) {
            log.warn("无效的 limit 值: {}", limit);
            throw new BadRequestException("limit 必须在 1-100 之间");
        }

        List<UserBehavior> behaviors = behaviorService.getRecentBehaviors(userId, limit);

        // 转换为 DTO
        List<BehaviorResponse> behaviorResponses =
            behaviors.stream()
                .map(BehaviorResponse::fromEntity)
                .collect(Collectors.toList());

        log.info("查询到 {} 条最近行为记录: userId={}", behaviors.size(), userId);

        return ResponseEntity.ok(ApiResponse.success(behaviorResponses));
    }

    /**
     * 统计用户查询行为数量。
     *
     * @param userId 用户ID
     * @return 查询行为数量
     */
    @GetMapping("/count/query")
    public ResponseEntity<ApiResponse<BehaviorCountResponse>> countQueries(
        @RequestParam Long userId) {
        log.info("统计用户查询行为数量: userId={}", userId);

        if (userId == null || userId <= 0) {
            log.warn("无效的用户ID: {}", userId);
            throw new BadRequestException("用户ID不能为空或小于等于0");
        }

        int count = behaviorService.countByType(userId, UserBehavior.BehaviorType.QUERY);
        log.info("用户查询行为统计: userId={}, count={}", userId, count);

        BehaviorCountResponse countResponse =
            BehaviorCountResponse.builder().behaviorType("QUERY").count(count).build();

        return ResponseEntity.ok(ApiResponse.success(countResponse));
    }

    /**
     * 统计用户点击行为数量。
     *
     * @param userId 用户ID
     * @return 点击行为数量
     */
    @GetMapping("/count/click")
    public ResponseEntity<ApiResponse<BehaviorCountResponse>> countClicks(
        @RequestParam Long userId) {
        log.info("统计用户点击行为数量: userId={}", userId);

        if (userId == null || userId <= 0) {
            log.warn("无效的用户ID: {}", userId);
            throw new BadRequestException("用户ID不能为空或小于等于0");
        }

        int count = behaviorService.countByType(userId, UserBehavior.BehaviorType.CLICK);
        log.info("用户点击行为统计: userId={}, count={}", userId, count);

        BehaviorCountResponse countResponse =
            BehaviorCountResponse.builder().behaviorType("CLICK").count(count).build();

        return ResponseEntity.ok(ApiResponse.success(countResponse));
    }

    /**
     * 统计用户收藏行为数量。
     *
     * @param userId 用户ID
     * @return 收藏行为数量
     */
    @GetMapping("/count/favorite")
    public ResponseEntity<ApiResponse<BehaviorCountResponse>> countFavorites(
        @RequestParam Long userId) {
        log.info("统计用户收藏行为数量: userId={}", userId);

        if (userId == null || userId <= 0) {
            log.warn("无效的用户ID: {}", userId);
            throw new BadRequestException("用户ID不能为空或小于等于0");
        }

        int count = behaviorService.countByType(userId, UserBehavior.BehaviorType.FAVORITE);
        log.info("用户收藏行为统计: userId={}, count={}", userId, count);

        BehaviorCountResponse countResponse =
            BehaviorCountResponse.builder().behaviorType("FAVORITE").count(count).build();

        return ResponseEntity.ok(ApiResponse.success(countResponse));
    }
}
