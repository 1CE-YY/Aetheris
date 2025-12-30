package com.aetheris.rag.controller;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;
import com.aetheris.rag.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证操作的 REST 控制器。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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

    try {
      AuthResponse response = authService.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      log.warn("Registration failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
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

    try {
      AuthResponse response = authService.login(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("Login failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
