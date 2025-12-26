package com.aetheris.rag.service.auth.impl;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;
import com.aetheris.rag.dto.response.UserResponse;
import com.aetheris.rag.mapper.UserMapper;
import com.aetheris.rag.model.User;
import com.aetheris.rag.service.auth.AuthService;
import com.aetheris.rag.util.JwtUtil;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  private final UserMapper userMapper;
  private final JwtUtil jwtUtil;
  private final BCryptPasswordEncoder passwordEncoder;

  public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.jwtUtil = jwtUtil;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  @Override
  @Transactional
  public AuthResponse register(@NotNull RegisterRequest request) {
    log.info("Registering new user: {}", request.getUsername());

    // Check if email already exists
    if (userMapper.emailExists(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }

    // Check if username already exists
    if (userMapper.usernameExists(request.getUsername())) {
      throw new IllegalArgumentException("Username already taken");
    }

    // Hash password
    String passwordHash = passwordEncoder.encode(request.getPassword());

    // Create user
    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordHash)
            .createdAt(java.time.Instant.now())
            .updatedAt(java.time.Instant.now())
            .lastActiveAt(java.time.Instant.now())
            .build();

    userMapper.insert(user);

    log.info("User registered successfully: {}", user.getId());

    // Generate token
    String token = jwtUtil.generateToken(user.getId());

    // Build response
    UserResponse userResponse =
        UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .createdAt(user.getCreatedAt())
            .lastActiveAt(user.getLastActiveAt())
            .build();

    return AuthResponse.builder().token(token).user(userResponse).build();
  }

  @Override
  public AuthResponse login(@NotNull LoginRequest request) {
    log.info("User login attempt: {}", request.getEmail());

    // Find user by email
    User user = userMapper.findByEmail(request.getEmail());
    if (user == null) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    // Update last active
    userMapper.updateLastActive(user.getId(), java.time.Instant.now());

    log.info("User logged in successfully: {}", user.getId());

    // Generate token
    String token = jwtUtil.generateToken(user.getId());

    // Build response
    UserResponse userResponse =
        UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .createdAt(user.getCreatedAt())
            .lastActiveAt(user.getLastActiveAt())
            .build();

    return AuthResponse.builder().token(token).user(userResponse).build();
  }

  @Override
  public boolean validateToken(String token) {
    return jwtUtil.validateToken(token);
  }

  @Override
  public Long getUserIdFromToken(String token) {
    return jwtUtil.getUserIdFromToken(token);
  }
}
