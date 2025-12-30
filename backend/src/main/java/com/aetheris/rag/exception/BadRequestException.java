/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 400 错误请求异常
 *
 * <p>用于客户端请求参数错误的情况
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message, 400);
    }

    public BadRequestException(String message, Throwable cause) {
        super("BAD_REQUEST", message, 400, cause);
    }
}
