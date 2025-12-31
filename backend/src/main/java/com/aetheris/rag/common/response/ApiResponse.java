/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.common.response;

import com.aetheris.rag.exception.BaseException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装类。
 *
 * <p>所有接口响应统一使用此类包装，确保前端处理一致。
 *
 * @param <T> 业务数据类型
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  /** 业务状态码 */
  private Integer code;

  /** 消息描述 */
  private String message;

  /** 业务数据 */
  private T data;

  /**
   * 成功响应（默认消息）。
   *
   * @param data 业务数据
   * @param <T> 数据类型
   * @return 统一响应
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .code(200)
        .message("操作成功")
        .data(data)
        .build();
  }

  /**
   * 成功响应（自定义消息）。
   *
   * @param data 业务数据
   * @param message 消息
   * @param <T> 数据类型
   * @return 统一响应
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .code(200)
        .message(message)
        .data(data)
        .build();
  }

  /**
   * 失败响应。
   *
   * @param code 错误码
   * @param message 错误消息
   * @param <T> 数据类型
   * @return 统一响应
   */
  public static <T> ApiResponse<T> error(Integer code, String message) {
    return ApiResponse.<T>builder()
        .code(code)
        .message(message)
        .data(null)
        .build();
  }

  /**
   * 从异常构建响应。
   *
   * @param ex 业务异常
   * @param <T> 数据类型
   * @return 统一响应
   */
  public static <T> ApiResponse<T> fromException(BaseException ex) {
    return ApiResponse.<T>builder()
        .code(Integer.valueOf(ex.getHttpStatus()))
        .message(ex.getMessage())
        .data(null)
        .build();
  }
}
