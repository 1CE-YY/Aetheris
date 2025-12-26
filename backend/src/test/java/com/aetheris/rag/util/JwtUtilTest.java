package com.aetheris.rag.util;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JwtUtil}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

  private JwtUtil jwtUtil;
  private String testSecret;

  @BeforeEach
  void setUp() {
    // Generate a 256-bit secret key for testing
    SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    testSecret = Base64.getEncoder().encodeToString(key.getEncoded());

    // JwtUtil requires (secret, expiration) - use 1 hour (3600000 ms) for tests
    jwtUtil = new JwtUtil(testSecret, 3600000L);
  }

  @Test
  @DisplayName("generateToken should create valid JWT token")
  void testGenerateToken() {
    // Given
    Long userId = 123L;

    // When
    String token = jwtUtil.generateToken(userId);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(token.length() > 50);
  }

  @Test
  @DisplayName("validateToken should accept valid token")
  void testValidateValidToken() {
    // Given
    Long userId = 456L;
    String token = jwtUtil.generateToken(userId);

    // When
    boolean isValid = jwtUtil.validateToken(token);

    // Then
    assertTrue(isValid);
  }

  @Test
  @DisplayName("validateToken should reject invalid token")
  void testValidateInvalidToken() {
    // Given
    String invalidToken = "invalid.token.string";

    // When
    boolean isValid = jwtUtil.validateToken(invalidToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("validateToken should reject expired token")
  void testValidateExpiredToken() {
    // Given - Create an expired token (expiration set to past)
    Long userId = 789L;
    SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(testSecret));

    String expiredToken =
        Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 10000))
            .setExpiration(new java.util.Date(System.currentTimeMillis() - 5000))
            .signWith(key)
            .compact();

    // When
    boolean isValid = jwtUtil.validateToken(expiredToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("getUserIdFromToken should extract correct user ID")
  void testGetUserIdFromToken() {
    // Given
    Long userId = 999L;
    String token = jwtUtil.generateToken(userId);

    // When
    Long extractedUserId = jwtUtil.getUserIdFromToken(token);

    // Then
    assertEquals(userId, extractedUserId);
  }

  @Test
  @DisplayName("getUserIdFromToken should throw exception for invalid token")
  void testGetUserIdFromInvalidToken() {
    // Given
    String invalidToken = "invalid.token.string";

    // When & Then
    assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken(invalidToken));
  }

  @Test
  @DisplayName("getUserIdFromToken should throw exception for malformed token")
  void testGetUserIdFromMalformedToken() {
    // Given
    String malformedToken = "eyJhbGciOiJIUzI1NiJ9.malformed.payload";

    // When & Then
    assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken(malformedToken));
  }
}
