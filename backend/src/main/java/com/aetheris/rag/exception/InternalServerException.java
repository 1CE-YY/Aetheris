/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 500 内部服务器错误异常
 *
 * <p>用于服务器内部错误的情况
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class InternalServerException extends BaseException {

    public InternalServerException(String message) {
        super("INTERNAL_ERROR", message, 500);
    }

    public InternalServerException(String message, Throwable cause) {
        super("INTERNAL_ERROR", message, 500, cause);
    }
}
