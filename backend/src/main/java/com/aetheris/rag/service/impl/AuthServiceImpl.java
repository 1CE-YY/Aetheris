package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;
import com.aetheris.rag.dto.response.UserResponse;
import com.aetheris.rag.exception.ConflictException;
import com.aetheris.rag.exception.UnauthorizedException;
import com.aetheris.rag.mapper.UserMapper;
import com.aetheris.rag.entity.User;
import com.aetheris.rag.service.AuthService;
import com.aetheris.rag.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AuthService} 的实现类。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserMapper userMapper;
  private final JwtUtil jwtUtil;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    log.info("注册新用户: {}", request.getUsername());

    // 检查邮箱是否已存在
    if (userMapper.emailExists(request.getEmail())) {
      throw new ConflictException("邮箱已被注册");
    }

    // 检查用户名是否已存在
    if (userMapper.usernameExists(request.getUsername())) {
      throw new ConflictException("用户名已被占用");
    }

    // 对密码进行哈希
    String passwordHash = passwordEncoder.encode(request.getPassword());

    // 创建用户
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

    // 生成 token
    String token = jwtUtil.generateToken(user.getId());

    // 构建响应
    UserResponse userResponse = UserResponse.fromEntity(user);

    return AuthResponse.builder().token(token).user(userResponse).build();
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    log.info("用户登录尝试: {}", request.getEmail());

    // 根据邮箱查找用户
    User user = userMapper.findByEmail(request.getEmail());
    if (user == null) {
      throw new UnauthorizedException("邮箱或密码错误");
    }

    // 验证密码
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("邮箱或密码错误");
    }

    // 更新最后活跃时间
    userMapper.updateLastActive(user.getId(), java.time.Instant.now());

    log.info("User logged in successfully: {}", user.getId());

    // 生成 token
    String token = jwtUtil.generateToken(user.getId());

    // 构建响应
    UserResponse userResponse = UserResponse.fromEntity(user);

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

  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUser(Long userId) {
    log.debug("获取当前用户信息: userId={}", userId);

    User user = userMapper.findById(userId);
    if (user == null) {
      throw new UnauthorizedException("用户不存在");
    }

    return UserResponse.fromEntity(user);
  }
}
