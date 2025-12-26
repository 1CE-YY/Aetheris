package com.aetheris.rag.mapper;

import com.aetheris.rag.model.User;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for User entity.
 *
 * <p>This interface defines database operations for user accounts.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Mapper
public interface UserMapper {

  /**
   * Inserts a new user.
   *
   * @param user the user to insert
   * @return the number of affected rows
   */
  int insert(User user);

  /**
   * Finds a user by ID.
   *
   * @param id the user ID
   * @return the user, or null if not found
   */
  User findById(@Param("id") Long id);

  /**
   * Finds a user by email.
   *
   * @param email the email address
   * @return the user, or null if not found
   */
  User findByEmail(@Param("email") String email);

  /**
   * Finds a user by username.
   *
   * @param username the username
   * @return the user, or null if not found
   */
  User findByUsername(@Param("username") String username);

  /**
   * Updates the last active timestamp.
   *
   * @param id the user ID
   * @param lastActiveAt the last active timestamp
   * @return the number of affected rows
   */
  int updateLastActive(@Param("id") Long id, @Param("lastActiveAt") Instant lastActiveAt);

  /**
   * Checks if an email exists.
   *
   * @param email the email address
   * @return true if exists, false otherwise
   */
  boolean emailExists(@Param("email") String email);

  /**
   * Checks if a username exists.
   *
   * @param username the username
   * @return true if exists, false otherwise
   */
  boolean usernameExists(@Param("username") String username);
}
