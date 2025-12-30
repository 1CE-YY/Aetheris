/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

import lombok.Getter;

/**
 * 基础异常类
 *
 * <p>所有自定义异常的基类，包含错误码和错误信息
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * HTTP 状态码
     */
    private final int httpStatus;

    public BaseException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BaseException(String code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
