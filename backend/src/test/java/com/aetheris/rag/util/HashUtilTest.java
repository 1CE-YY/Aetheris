package com.aetheris.rag.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HashUtil}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("HashUtil Tests")
class HashUtilTest {

  @Test
  @DisplayName("hashText should return consistent hash for same text")
  void testHashTextConsistency() {
    String text = "Hello, World!";
    String hash1 = HashUtil.hashText(text);
    String hash2 = HashUtil.hashText(text);

    assertNotNull(hash1);
    assertEquals(hash1, hash2, "Hash should be consistent for same text");
  }

  @Test
  @DisplayName("hashText should return null for null input")
  void testHashTextNullInput() {
    String hash = HashUtil.hashText(null);
    assertNull(hash);
  }

  @Test
  @DisplayName("hashText should be different for different texts")
  void testHashTextUniqueness() {
    String text1 = "Hello";
    String text2 = "World";

    String hash1 = HashUtil.hashText(text1);
    String hash2 = HashUtil.hashText(text2);

    assertNotEquals(hash1, hash2, "Hashes should be different for different texts");
  }

  @Test
  @DisplayName("hashText should handle whitespace normalization")
  void testHashTextWhitespaceNormalization() {
    String text1 = "Hello  World";
    String text2 = "Hello World";

    String hash1 = HashUtil.hashText(text1);
    String hash2 = HashUtil.hashText(text2);

    assertEquals(hash1, hash2, "Hashes should be same after whitespace normalization");
  }

  @Test
  @DisplayName("normalizeText should collapse multiple spaces")
  void testNormalizeTextCollapseSpaces() {
    String input = "Hello    World";
    String expected = "Hello World";

    String result = HashUtil.normalizeText(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalizeText should trim leading/trailing spaces")
  void testNormalizeTextTrim() {
    String input = "  Hello World  ";
    String expected = "Hello World";

    String result = HashUtil.normalizeText(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalizeText should return null for null input")
  void testNormalizeTextNullInput() {
    String result = HashUtil.normalizeText(null);
    assertNull(result);
  }

  @Test
  @DisplayName("normalizeText should handle empty string")
  void testNormalizeTextEmptyString() {
    String result = HashUtil.normalizeText("");
    assertEquals("", result);
  }
}
