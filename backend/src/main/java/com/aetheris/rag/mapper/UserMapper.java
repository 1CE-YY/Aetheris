package com.aetheris.rag.mapper;

import com.aetheris.rag.model.User;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户实体的 MyBatis Mapper。
 *
 * <p>此接口定义用户帐户的数据库操作。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Mapper
public interface UserMapper {

  /**
   * 插入新用户。
   *
   * @param user 要插入的用户
   * @return 受影响的行数
   */
  int insert(User user);

  /**
   * 根据 ID 查找用户。
   *
   * @param id 用户 ID
   * @return 用户，如果未找到则返回 null
   */
  User findById(@Param("id") Long id);

  /**
   * 根据电子邮件查找用户。
   *
   * @param email 电子邮件地址
   * @return 用户，如果未找到则返回 null
   */
  User findByEmail(@Param("email") String email);

  /**
   * 根据用户名查找用户。
   *
   * @param username 用户名
   * @return 用户，如果未找到则返回 null
   */
  User findByUsername(@Param("username") String username);

  /**
   * 更新最后活动时间戳。
   *
   * @param id 用户 ID
   * @param lastActiveAt 最后活动时间戳
   * @return 受影响的行数
   */
  int updateLastActive(@Param("id") Long id, @Param("lastActiveAt") Instant lastActiveAt);

  /**
   * 检查电子邮件是否存在。
   *
   * @param email 电子邮件地址
   * @return 如果存在返回 true，否则返回 false
   */
  boolean emailExists(@Param("email") String email);

  /**
   * 检查用户名是否存在。
   *
   * @param username 用户名
   * @return 如果存在返回 true，否则返回 false
   */
  boolean usernameExists(@Param("username") String username);
}
