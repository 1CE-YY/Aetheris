/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

import com.aetheris.rag.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 *
 * <p>统一处理应用中抛出的所有异常，返回标准化的错误响应。
 *
 * <p>处理顺序：
 * <ol>
 *   <li>自定义业务异常（BaseException 及其子类）</li>
 *   <li>参数校验异常（MethodArgumentNotValidException）</li>
 *   <li>参数类型不匹配异常（MethodArgumentTypeMismatchException）</li>
 *   <li>其他未捕获异常</li>
 * </ol>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有自定义业务异常。
     *
     * @param ex 业务异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
        BaseException ex, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());

        ApiResponse<Void> response =
            ApiResponse.error(Integer.valueOf(ex.getHttpStatus()), ex.getMessage());

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * 处理 400 错误请求异常。
     *
     * @param ex 错误请求异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
        BadRequestException ex, HttpServletRequest request) {
        log.warn("请求参数错误: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(400, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理 401 未授权异常。
     *
     * @param ex 未授权异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
        UnauthorizedException ex, HttpServletRequest request) {
        log.warn("未授权访问: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(401, ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理 403 禁止访问异常。
     *
     * @param ex 禁止访问异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
        ForbiddenException ex, HttpServletRequest request) {
        log.warn("禁止访问: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(403, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理 404 资源未找到异常。
     *
     * @param ex 资源未找到异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(
        NotFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(404, ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理 500 内部服务器错误异常。
     *
     * @param ex 内部服务器错误异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalServerException(
        InternalServerException ex, HttpServletRequest request) {
        log.error("内部服务器错误: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(500, ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理参数校验异常（@Valid 触发）。
     *
     * @param ex 参数校验异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
        MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("参数校验失败: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String detailMessage =
            errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        ApiResponse<Void> response = ApiResponse.error(400, "参数校验失败: " + detailMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数类型不匹配异常。
     *
     * @param ex 参数类型不匹配异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("参数类型不匹配: parameter={}, value={}", ex.getName(), ex.getValue());

        String message =
            String.format("参数 '%s' 的值 '%s' 类型不正确", ex.getName(), ex.getValue());

        ApiResponse<Void> response = ApiResponse.error(400, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
        IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("非法参数: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(400, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理所有未捕获的异常。
     *
     * @param ex 异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(
        Exception ex, HttpServletRequest request) {
        log.error("未处理的异常: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(500, "服务器内部错误");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
