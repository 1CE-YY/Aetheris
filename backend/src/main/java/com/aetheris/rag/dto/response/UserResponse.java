package com.aetheris.rag.dto.response;

import com.aetheris.rag.entity.User;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息的响应 DTO。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  /** 用户ID */
  private Long id;

  /** 用户名 */
  private String username;

  /** 邮箱 */
  private String email;

  /** 注册时间 */
  private Instant createdAt;

  /** 最后活跃时间 */
  private Instant lastActiveAt;

  /**
   * 从实体类转换为 DTO
   *
   * <p>提供统一的实体到 DTO 转换方法，避免重复代码
   *
   * @param user 用户实体
   * @return 用户响应 DTO
   */
  public static UserResponse fromEntity(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .createdAt(user.getCreatedAt())
        .lastActiveAt(user.getLastActiveAt())
        .build();
  }
}
