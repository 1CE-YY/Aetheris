/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 401 未授权异常
 *
 * <p>用于认证失败的情况
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message, 401);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super("UNAUTHORIZED", message, 401, cause);
    }
}
