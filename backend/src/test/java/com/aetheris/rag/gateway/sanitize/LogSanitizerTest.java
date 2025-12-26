package com.aetheris.rag.gateway.sanitize;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LogSanitizer}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("LogSanitizer Tests")
class LogSanitizerTest {

  @Test
  @DisplayName("sanitize should truncate long input")
  void testSanitizeTruncate() {
    String longInput = "a".repeat(300);
    String result = LogSanitizer.sanitize(longInput);

    assertTrue(result.length() < 300, "Result should be truncated");
    assertTrue(result.endsWith("...[truncated]"), "Result should end with truncation suffix");
  }

  @Test
  @DisplayName("sanitize should keep short input unchanged")
  void testSanitizeShortInput() {
    String shortInput = "Hello, World!";
    String result = LogSanitizer.sanitize(shortInput);

    assertEquals(shortInput, result);
  }

  @Test
  @DisplayName("sanitize should return null for null input")
  void testSanitizeNullInput() {
    String result = LogSanitizer.sanitize(null);
    assertNull(result);
  }

  @Test
  @DisplayName("maskApiKey should mask API key")
  void testMaskApiKey() {
    String apiKey = "sk-abc123def456";
    String result = LogSanitizer.maskApiKey(apiKey);

    assertTrue(result.contains("****"), "Result should contain mask");
    assertTrue(result.startsWith("sk-abc"), "Result should show first few chars");
    assertFalse(result.contains("def456"), "Result should mask remaining chars");
  }

  @Test
  @DisplayName("maskApiKey should handle short API key")
  void testMaskApiKeyShort() {
    String shortKey = "sk-ab";
    String result = LogSanitizer.maskApiKey(shortKey);

    assertEquals("****", result, "Short keys should be fully masked");
  }

  @Test
  @DisplayName("maskToken should return masked string")
  void testMaskToken() {
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    String result = LogSanitizer.maskToken(token);

    assertEquals("[JWT_TOKEN]", result);
  }

  @Test
  @DisplayName("maskPassword should return masked string")
  void testMaskPassword() {
    String password = "mySecretPassword123";
    String result = LogSanitizer.maskPassword(password);

    assertEquals("[PASSWORD]", result);
  }

  @Test
  @DisplayName("sanitize with custom maxLength should respect custom limit")
  void testSanitizeCustomMaxLength() {
    String input = "a".repeat(100);
    int maxLength = 50;

    String result = LogSanitizer.sanitize(input, maxLength);

    assertEquals(50 + "...[truncated]".length(), result.length());
  }
}
