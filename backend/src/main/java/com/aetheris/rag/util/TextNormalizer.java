package com.aetheris.rag.util;

/**
 * Utility class for text normalization.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public final class TextNormalizer {

  private TextNormalizer() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Normalizes text by removing redundant whitespace and unifying line breaks.
   *
   * @param text the text to normalize
   * @return normalized text
   */
  public static String normalize(String text) {
    if (text == null) {
      return null;
    }

    // Unify line breaks
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');

    // Trim lines and remove empty lines
    String[] lines = normalized.split("\n");
    StringBuilder sb = new StringBuilder();

    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(trimmed);
      }
    }

    // Collapse multiple spaces
    return sb.toString().replaceAll("\\s+", " ");
  }
}
