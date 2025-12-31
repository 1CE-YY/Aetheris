package com.aetheris.rag.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示系统中用户帐户的用户实体。
 *
 * <p>此类映射到 {@code users} 表，包含用户认证和个人资料信息。
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

  /** 用户 ID（主键，自动生成） */
  private Long id;

  /** 用户名（唯一，非空） */
  private String username;

  /** 电子邮件地址（唯一，非空） */
  private String email;

  /** 密码哈希（BCrypt，非空） */
  private String passwordHash;

  /** 帐户创建时间戳 */
  private Instant createdAt;

  /** 最后更新时间戳 */
  private Instant updatedAt;

  /** 最后活动时间戳（可空） */
  private Instant lastActiveAt;
}
