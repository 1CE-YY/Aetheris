package com.aetheris.rag.util;

/**
 * 文本规范化的工具类。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public final class TextNormalizer {

  private TextNormalizer() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * 通过删除冗余空格和统一换行符来规范化文本。
   *
   * @param text 要规范化的文本
   * @return 规范化的文本
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
