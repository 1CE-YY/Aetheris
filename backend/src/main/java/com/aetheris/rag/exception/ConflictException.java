/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.exception;

/**
 * 409 冲突异常
 *
 * <p>用于资源冲突的情况，例如：
 * <ul>
 *   <li>邮箱已被注册</li>
 *   <li>用户名已被占用</li>
 *   <li>资源版本冲突</li>
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super("CONFLICT", message, 409);
    }

    public ConflictException(String message, Throwable cause) {
        super("CONFLICT", message, 409, cause);
    }
}
