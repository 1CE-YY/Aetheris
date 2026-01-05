package com.aetheris.rag.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
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
 * <p>支持时钟偏差容忍，避免因客户端和服务器时间不同步导致 token 被拒绝。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

  /** 时钟偏差容忍时间（秒）。 */
  private static final int ALLOWED_CLOCK_SKEW_SECONDS = 300; // 5 分钟

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
   * @throws RuntimeException 如果 token 无效
   */
  public Long getUserIdFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .setSigningKey(key)
              .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
              .build()
              .parseClaimsJws(token)
              .getBody();
      return Long.parseLong(claims.getSubject());
    } catch (ExpiredJwtException e) {
      log.error("Token 已过期: {}", e.getMessage());
      throw new RuntimeException("Token 已过期", e);
    } catch (MalformedJwtException e) {
      log.error("Token 格式错误: {}", e.getMessage());
      throw new RuntimeException("Token 格式错误", e);
    } catch (Exception e) {
      log.error("解析 Token 失败: {}", e.getMessage(), e);
      throw new RuntimeException("无效的 Token", e);
    }
  }

  /**
   * 验证 JWT token。
   *
   * <p>验证内容包括：
   *
   * <ul>
   *   <li>签名是否有效
   *   <li>token 是否过期（允许 5 分钟时钟偏差）
   *   <li>token 格式是否正确
   * </ul>
   *
   * @param token JWT token
   * @return 如果有效返回 true，否则返回 false
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .setSigningKey(key)
          .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.debug("Token 已过期: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.debug("Token 格式错误: {}", e.getMessage());
      return false;
    } catch (UnsupportedJwtException e) {
      log.debug("不支持的 Token 类型: {}", e.getMessage());
      return false;
    } catch (IllegalArgumentException e) {
      log.debug("Token 参数非法: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("验证 Token 时发生未知错误: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * 解析 JWT token 并返回 Claims 对象。
   *
   * <p>此方法只解析一次 token，避免重复解析。用于 SecurityConfig 中优化性能。
   *
   * @param token JWT token
   * @return Claims 对象，包含 token 中的所有声明
   * @throws RuntimeException 如果 token 无效
   */
  public Claims parseToken(String token) {
    try {
      return Jwts.parser()
          .setSigningKey(key)
          .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (ExpiredJwtException e) {
      log.error("Token 已过期: {}", e.getMessage());
      throw new RuntimeException("Token 已过期", e);
    } catch (MalformedJwtException e) {
      log.error("Token 格式错误: {}", e.getMessage());
      throw new RuntimeException("Token 格式错误", e);
    } catch (UnsupportedJwtException e) {
      log.error("不支持的 Token 类型: {}", e.getMessage());
      throw new RuntimeException("不支持的 Token 类型", e);
    } catch (IllegalArgumentException e) {
      log.error("Token 参数非法: {}", e.getMessage());
      throw new RuntimeException("Token 参数非法", e);
    } catch (Exception e) {
      log.error("解析 Token 失败: {}", e.getMessage(), e);
      throw new RuntimeException("无效的 Token", e);
    }
  }
}
