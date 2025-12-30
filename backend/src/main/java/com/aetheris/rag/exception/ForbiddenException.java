/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 403 禁止访问异常
 *
 * <p>用于权限不足的情况
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message, 403);
    }

    public ForbiddenException(String message, Throwable cause) {
        super("FORBIDDEN", message, 403, cause);
    }
}
