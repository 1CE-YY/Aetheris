package com.aetheris.rag.gateway.sanitize;

/**
 * 用于清理日志消息以防止敏感信息泄露的工具类。
 *
 * <p>此类提供了在日志中屏蔽或截断敏感数据的方法，确保：
 *
 * <ul>
 *   <li>API 密钥和 token 永远不会以明文记录
 *   <li>用户输入被截断以防止日志泛滥
 *   <li>密码和凭据被完全屏蔽
 * </ul>
 *
 * <p>用法示例：
 *
 * <pre>{@code
 * // 清理用户输入
 * String sanitized = LogSanitizer.sanitize(userInput); // 截断为 200 个字符
 *
 * // 屏蔽 API 密钥
 * String masked = LogSanitizer.maskApiKey("sk-abc123def456"); // "sk-abc****"
 * }</pre>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public final class LogSanitizer {

  private static final int MAX_INPUT_LENGTH = 200;
  private static final String TRUNCATION_SUFFIX = "...[truncated]";
  private static final int API_KEY_VISIBLE_CHARS = 8;
  private static final String MASK = "****";

  private LogSanitizer() {
    // Prevent instantiation
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * 通过截断到最大长度来清理用户输入。
   *
   * <p>此方法将输入文本截断为 {@code MAX_INPUT_LENGTH} 个字符，以防止日志泛滥并降低日志存储成本。
   * 如果被截断，则附加后缀。
   *
   * @param input 要清理的用户输入
   * @return 清理后的输入，如果输入为 null 则返回 null
   */
  public static String sanitize(String input) {
    if (input == null) {
      return null;
    }

    if (input.length() <= MAX_INPUT_LENGTH) {
      return input;
    }

    return input.substring(0, MAX_INPUT_LENGTH) + TRUNCATION_SUFFIX;
  }

  /**
   * 使用自定义最大长度清理用户输入。
   *
   * @param input 要清理的用户输入
   * @param maxLength 允许的最大长度
   * @return 清理后的输入，如果输入为 null 则返回 null
   */
  public static String sanitize(String input, int maxLength) {
    if (input == null) {
      return null;
    }

    if (input.length() <= maxLength) {
      return input;
    }

    return input.substring(0, maxLength) + TRUNCATION_SUFFIX;
  }

  /**
   * 通过仅显示前几个字符来屏蔽 API 密钥。
   *
   * <p>示例：{@code maskApiKey("sk-abc123def456")} 返回 {@code "sk-abc****"}
   *
   * @param apiKey 要屏蔽的 API 密钥
   * @return 屏蔽后的 API 密钥，如果 apiKey 为 null 则返回 null
   */
  public static String maskApiKey(String apiKey) {
    if (apiKey == null) {
      return null;
    }

    if (apiKey.length() <= API_KEY_VISIBLE_CHARS) {
      return MASK;
    }

    return apiKey.substring(0, API_KEY_VISIBLE_CHARS) + MASK;
  }

  /**
   * 完全屏蔽 JWT token。
   *
   * <p>JWT token 永远不应以明文记录。此方法将整个 token 替换为屏蔽字符串。
   *
   * @param token 要屏蔽的 JWT token
   * @return 屏蔽后的 token 字符串，如果 token 为 null 则返回 "[null]"
   */
  public static String maskToken(String token) {
    return token == null ? "[null]" : "[JWT_TOKEN]";
  }

  /**
   * 完全屏蔽密码。
   *
   * <p>密码不应以任何形式记录。此方法将密码替换为屏蔽字符串。
   *
   * @param password 要屏蔽的密码
   * @return 屏蔽后的密码字符串，如果密码为 null 则返回 "[null]"
   */
  public static String maskPassword(String password) {
    return password == null ? "[null]" : "[PASSWORD]";
  }

  /**
   * 清理异常消息以进行记录。
   *
   * <p>某些异常可能在消息中包含敏感信息。如有必要，此方法会截断消息。
   *
   * @param throwable 要清理的异常
   * @return 清理后的异常消息
   */
  public static String sanitizeException(Throwable throwable) {
    if (throwable == null) {
      return "[null]";
    }

    String message = throwable.getMessage();
    String className = throwable.getClass().getSimpleName();

    if (message == null) {
      return className;
    }

    return className + ": " + sanitize(message);
  }
}
