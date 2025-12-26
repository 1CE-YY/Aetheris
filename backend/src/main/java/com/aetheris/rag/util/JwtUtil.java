package com.aetheris.rag.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for JWT token generation and validation.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Component
public class JwtUtil {

  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

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
   * Generates a JWT token for the given user ID.
   *
   * @param userId the user ID
   * @return the JWT token
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
   * Extracts user ID from a JWT token.
   *
   * @param token the JWT token
   * @return the user ID
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
    return Long.parseLong(claims.getSubject());
  }

  /**
   * Validates a JWT token.
   *
   * @param token the JWT token
   * @return true if valid, false otherwise
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
