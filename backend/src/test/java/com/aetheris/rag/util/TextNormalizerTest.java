package com.aetheris.rag.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TextNormalizer}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("TextNormalizer Tests")
class TextNormalizerTest {

  @Test
  @DisplayName("normalize should collapse multiple spaces")
  void testNormalizeCollapseSpaces() {
    String input = "Hello    World";
    String expected = "Hello World";

    String result = TextNormalizer.normalize(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalize should unify line breaks")
  void testNormalizeUnifyLineBreaks() {
    String input = "Line1\r\nLine2\rLine3\nLine4";
    String expected = "Line1\nLine2\nLine3\nLine4";

    String result = TextNormalizer.normalize(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalize should trim lines and remove empty lines")
  void testNormalizeTrimLines() {
    String input = "  Line1  \n\n  Line2  \n\n  Line3  ";
    String expected = "Line1\nLine2\nLine3";

    String result = TextNormalizer.normalize(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalize should handle null input")
  void testNormalizeNullInput() {
    String result = TextNormalizer.normalize(null);
    assertNull(result);
  }

  @Test
  @DisplayName("normalize should handle empty string")
  void testNormalizeEmptyString() {
    String result = TextNormalizer.normalize("");
    assertEquals("", result);
  }

  @Test
  @DisplayName("normalize should handle whitespace only")
  void testNormalizeWhitespaceOnly() {
    String result = TextNormalizer.normalize("   \n\n   ");
    assertEquals("", result);
  }

  @Test
  @DisplayName("normalize should collapse multiple line breaks")
  void testNormalizeCollapseLineBreaks() {
    String input = "Line1\n\n\n\nLine2";
    String expected = "Line1\nLine2";

    String result = TextNormalizer.normalize(input);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("normalize should trim leading and trailing whitespace")
  void testNormalizeTrimLeadingTrailing() {
    String input = "   \n  Hello World  \n   ";
    String expected = "Hello World";

    String result = TextNormalizer.normalize(input);

    assertEquals(expected, result);
  }
}
