/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一错误响应 DTO
 *
 * <p>用于返回统一的错误信息给前端
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误详细信息（可选）
     */
    private String detail;

    /**
     * 错误发生时间戳
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 创建标准错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建带详细信息的错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @param detail 详细信息
     * @return 错误响应
     */
    public static ErrorResponse withDetail(String code, String message, String detail) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 创建带路径的错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @param path 请求路径
     * @return 错误响应
     */
    public static ErrorResponse withPath(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * 创建带详细信息和路径的错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @param detail 详细信息
     * @param path 请求路径
     * @return 错误响应
     */
    public static ErrorResponse of(String code, String message, String detail, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    /**
     * 创建 400 错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.of("BAD_REQUEST", message);
    }

    /**
     * 创建 401 错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse unauthorized(String message) {
        return ErrorResponse.of("UNAUTHORIZED", message);
    }

    /**
     * 创建 403 错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse forbidden(String message) {
        return ErrorResponse.of("FORBIDDEN", message);
    }

    /**
     * 创建 404 错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse notFound(String message) {
        return ErrorResponse.of("NOT_FOUND", message);
    }

    /**
     * 创建 500 错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    public static ErrorResponse internalError(String message) {
        return ErrorResponse.of("INTERNAL_ERROR", message);
    }
}
