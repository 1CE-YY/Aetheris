package com.aetheris.rag.gateway.sanitize;

/**
 * Utility class for sanitizing log messages to prevent sensitive information leakage.
 *
 * <p>This class provides methods to mask or truncate sensitive data in logs, ensuring that:
 *
 * <ul>
 *   <li>API keys and tokens are never logged in plain text
 *   <li>User input is truncated to prevent log flooding
 *   <li>Passwords and credentials are completely masked
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Sanitize user input
 * String sanitized = LogSanitizer.sanitize(userInput); // Truncates to 200 chars
 *
 * // Mask API key
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
   * Sanitizes user input by truncating to maximum length.
   *
   * <p>This method truncates input text to {@code MAX_INPUT_LENGTH} characters to prevent log
   * flooding and reduce log storage costs. If truncated, a suffix is appended.
   *
   * @param input the user input to sanitize
   * @return sanitized input, or null if input is null
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
   * Sanitizes user input with custom maximum length.
   *
   * @param input the user input to sanitize
   * @param maxLength the maximum length to allow
   * @return sanitized input, or null if input is null
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
   * Masks an API key by showing only the first few characters.
   *
   * <p>Example: {@code maskApiKey("sk-abc123def456")} returns {@code "sk-abc****"}
   *
   * @param apiKey the API key to mask
   * @return masked API key, or null if apiKey is null
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
   * Masks a JWT token completely.
   *
   * <p>JWT tokens should never be logged in plain text. This method replaces the entire token
   * with a masked string.
   *
   * @param token the JWT token to mask
   * @return masked token string, or "[null]" if token is null
   */
  public static String maskToken(String token) {
    return token == null ? "[null]" : "[JWT_TOKEN]";
  }

  /**
   * Masks a password completely.
   *
   * <p>Passwords should never be logged in any form. This method replaces the password with a
   * masked string.
   *
   * @param password the password to mask
   * @return masked password string, or "[null]" if password is null
   */
  public static String maskPassword(String password) {
    return password == null ? "[null]" : "[PASSWORD]";
  }

  /**
   * Sanitizes an exception message for logging.
   *
   * <p>Some exceptions may contain sensitive information in their messages. This method
   * truncates the message if necessary.
   *
   * @param throwable the exception to sanitize
   * @return sanitized exception message
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
