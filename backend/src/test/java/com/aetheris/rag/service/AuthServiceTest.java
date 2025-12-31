package com.aetheris.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.aetheris.rag.dto.request.LoginRequest;
import com.aetheris.rag.dto.request.RegisterRequest;
import com.aetheris.rag.dto.response.AuthResponse;
import com.aetheris.rag.entity.User;
import com.aetheris.rag.mapper.UserMapper;
import com.aetheris.rag.service.impl.AuthServiceImpl;
import com.aetheris.rag.util.JwtUtil;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Unit tests for {@link AuthService}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

  @Mock private UserMapper userMapper;

  @Mock private JwtUtil jwtUtil;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthServiceImpl(userMapper, jwtUtil);
  }

  @Test
  @DisplayName("register should create user and return token")
  void testRegisterSuccess() {
    // Given
    RegisterRequest request =
        RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

    when(userMapper.emailExists(anyString())).thenReturn(false);
    when(userMapper.usernameExists(anyString())).thenReturn(false);
    when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(1L);
      return 1;
    });
    when(jwtUtil.generateToken(any(Long.class))).thenReturn("test-jwt-token");

    // When
    AuthResponse response = authService.register(request);

    // Then
    assertNotNull(response);
    assertEquals("test-jwt-token", response.getToken());
    assertNotNull(response.getUser());
    assertEquals("testuser", response.getUser().getUsername());
    assertEquals("test@example.com", response.getUser().getEmail());

    verify(userMapper).insert(any(User.class));
    verify(jwtUtil).generateToken(1L);
  }

  @Test
  @DisplayName("register should throw exception for existing email")
  void testRegisterEmailExists() {
    // Given
    RegisterRequest request =
        RegisterRequest.builder()
            .username("testuser")
            .email("existing@example.com")
            .password("password123")
            .build();

    when(userMapper.emailExists(anyString())).thenReturn(true);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.register(request));

    verify(userMapper, never()).insert(any(User.class));
  }

  @Test
  @DisplayName("register should throw exception for existing username")
  void testRegisterUsernameExists() {
    // Given
    RegisterRequest request =
        RegisterRequest.builder()
            .username("existinguser")
            .email("test@example.com")
            .password("password123")
            .build();

    when(userMapper.emailExists(anyString())).thenReturn(false);
    when(userMapper.usernameExists(anyString())).thenReturn(true);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.register(request));

    verify(userMapper, never()).insert(any(User.class));
  }

  @Test
  @DisplayName("login should authenticate user and return token")
  void testLoginSuccess() {
    // Given
    LoginRequest request =
        LoginRequest.builder().email("test@example.com").password("password123").build();

    // Generate real BCrypt hash for "password123"
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String passwordHash = encoder.encode("password123");

    User user =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .passwordHash(passwordHash)
            .createdAt(Instant.now())
            .lastActiveAt(Instant.now())
            .build();

    when(userMapper.findByEmail(anyString())).thenReturn(user);
    when(jwtUtil.generateToken(any(Long.class))).thenReturn("test-jwt-token");

    // When
    AuthResponse response = authService.login(request);

    // Then
    assertNotNull(response);
    assertEquals("test-jwt-token", response.getToken());
    assertNotNull(response.getUser());
    assertEquals("testuser", response.getUser().getUsername());
    assertEquals("test@example.com", response.getUser().getEmail());

    verify(userMapper).updateLastActive(eq(1L), any(Instant.class));
    verify(jwtUtil).generateToken(1L);
  }

  @Test
  @DisplayName("login should throw exception for non-existent user")
  void testLoginUserNotFound() {
    // Given
    LoginRequest request =
        LoginRequest.builder().email("nonexistent@example.com").password("password123").build();

    when(userMapper.findByEmail(anyString())).thenReturn(null);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> authService.login(request));

    verify(jwtUtil, never()).generateToken(any(Long.class));
  }
}
