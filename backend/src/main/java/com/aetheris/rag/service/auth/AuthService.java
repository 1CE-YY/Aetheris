package com.aetheris.rag.service.auth;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;

/**
 * Service interface for authentication operations.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public interface AuthService {

  /**
   * Registers a new user.
   *
   * @param request the registration request
   * @return the authentication response with token
   */
  AuthResponse register(RegisterRequest request);

  /**
   * Authenticates a user.
   *
   * @param request the login request
   * @return the authentication response with token
   */
  AuthResponse login(LoginRequest request);

  /**
   * Validates a JWT token.
   *
   * @param token the JWT token
   * @return true if valid, false otherwise
   */
  boolean validateToken(String token);

  /**
   * Extracts user ID from a JWT token.
   *
   * @param token the JWT token
   * @return the user ID
   */
  Long getUserIdFromToken(String token);
}
