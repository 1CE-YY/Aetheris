/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 404 资源未找到异常
 *
 * <p>用于请求的资源不存在的情况
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message, 404);
    }

    public NotFoundException(String message, Throwable cause) {
        super("NOT_FOUND", message, 404, cause);
    }
}
