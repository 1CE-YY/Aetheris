package com.aetheris.rag.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT token 生成和验证的工具类。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

  private final Key key;
  private final long jwtExpiration;

  public JwtUtil(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration}") long jwtExpiration) {

    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters");
    }

    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.jwtExpiration = jwtExpiration;

    log.info("Initialized JwtUtil with expiration: {} ms", jwtExpiration);
  }

  /**
   * 为给定的用户 ID 生成 JWT token。
   *
   * @param userId 用户 ID
   * @return JWT token
   */
  public String generateToken(Long userId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * 从 JWT token 中提取用户 ID。
   *
   * @param token JWT token
   * @return 用户 ID
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
    return Long.parseLong(claims.getSubject());
  }

  /**
   * 验证 JWT token。
   *
   * @param token JWT token
   * @return 如果有效返回 true，否则返回 false
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }
}
