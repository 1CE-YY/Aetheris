/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;
import com.aetheris.rag.dto.response.UserResponse;
import com.aetheris.rag.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证操作的 REST 控制器。
 *
 * <p>提供用户注册和登录接口，所有错误由全局异常处理器统一处理。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * 注册新用户。
   *
   * @param request 注册请求
   * @return 包含 token 的认证响应
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("POST /api/auth/register - username: {}", request.getUsername());

    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 用户登录认证。
   *
   * @param request 登录请求
   * @return 包含 token 的认证响应
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("POST /api/auth/login - email: {}", request.getEmail());

    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * 获取当前登录用户的信息。
   *
   * <p>从 SecurityContext 中获取已认证的用户 ID，并返回完整的用户信息。
   *
   * @return 当前用户信息
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("GET /api/auth/me - userId: {}", userId);

    UserResponse response = authService.getCurrentUser(userId);
    return ResponseEntity.ok(response);
  }
}
