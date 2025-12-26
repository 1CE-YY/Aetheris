package com.aetheris.rag.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity representing a user account in the system.
 *
 * <p>This class maps to the {@code users} table and contains user authentication and profile
 * information.
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  /** User ID (primary key, auto-generated) */
  private Long id;

  /** Username (unique, not null) */
  private String username;

  /** Email address (unique, not null) */
  private String email;

  /** Password hash (BCrypt, not null) */
  private String passwordHash;

  /** Account creation timestamp */
  private Instant createdAt;

  /** Last update timestamp */
  private Instant updatedAt;

  /** Last activity timestamp (nullable) */
  private Instant lastActiveAt;
}
