package com.aetheris.rag.service;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;

/**
 * 认证操作的服务接口。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public interface AuthService {

  /**
   * 注册新用户。
   *
   * @param request 注册请求
   * @return 包含 token 的认证响应
   */
  AuthResponse register(RegisterRequest request);

  /**
   * 用户登录认证。
   *
   * @param request 登录请求
   * @return 包含 token 的认证响应
   */
  AuthResponse login(LoginRequest request);

  /**
   * 验证 JWT token。
   *
   * @param token JWT token
   * @return 如果有效返回 true，否则返回 false
   */
  boolean validateToken(String token);

  /**
   * 从 JWT token 中提取用户 ID。
   *
   * @param token JWT token
   * @return 用户 ID
   */
  Long getUserIdFromToken(String token);
}
