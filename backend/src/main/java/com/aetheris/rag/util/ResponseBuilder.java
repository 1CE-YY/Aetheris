/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 统一的响应构建工具类。
 *
 * <p>提供统一的API响应构建方法，简化Controller层的响应构建逻辑。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
public final class ResponseBuilder {

  private ResponseBuilder() {
  }

  /**
   * 构建成功响应。
   *
   * @param data 响应数据
   * @param message 成功消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
    return ResponseEntity.ok(ApiResponse.success(data, message));
  }

  /**
   * 构建成功响应（无数据）。
   *
   * @param message 成功消息
   * @return 响应实体
   */
  public static ResponseEntity<ApiResponse<String>> success(String message) {
    return ResponseEntity.ok(ApiResponse.success(message));
  }

  /**
   * 构建成功响应（使用默认消息）。
   *
   * @param data 响应数据
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  /**
   * 构建成功响应（使用CREATED状态码）。
   *
   * @param data 响应数据
   * @param message 成功消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(data, message));
  }

  /**
   * 构建错误响应。
   *
   * @param code HTTP状态码
   * @param message 错误消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> error(int code, String message) {
    return ResponseEntity.status(code)
        .body(ApiResponse.error(code, message));
  }

  /**
   * 构建未找到响应。
   *
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(404, "资源不存在"));
  }

  /**
   * 构建禁止访问响应。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(403, message));
  }

  /**
   * 构建错误请求响应。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(400, message));
  }

  /**
   * 构建服务器错误响应。
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return 响应实体
   */
  public static <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(500, message));
  }
}
