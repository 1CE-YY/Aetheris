package com.aetheris.rag.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 计算哈希值的工具类。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public final class HashUtil {

  private static final MessageDigest SHA_256_DIGEST;

  static {
    try {
      SHA_256_DIGEST = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private HashUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * 计算规范化文本的 SHA-256 哈希。
   *
   * @param text 要哈希的文本
   * @return 十六进制哈希字符串
   */
  public static String hashText(String text) {
    if (text == null) {
      return null;
    }

    String normalized = normalizeText(text);
    byte[] hash = SHA_256_DIGEST.digest(normalized.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hash);
  }

  /**
   * 规范化文本以进行一致的哈希。
   *
   * @param text 要规范化的文本
   * @return 规范化的文本
   */
  public static String normalizeText(String text) {
    if (text == null) {
      return null;
    }

    return text
        .replaceAll("\\s+", " ") // Collapse whitespace
        .trim(); // Trim ends
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder();
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}
