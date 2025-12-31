package com.aetheris.rag.util;

import com.aetheris.rag.exception.InternalServerException;
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
      throw new InternalServerException("SHA-256 算法不可用", e);
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
   * 计算文本的 SHA-256 哈希（别名方法）。
   *
   * @param text 要哈希的文本
   * @return 十六进制哈希字符串
   */
  public static String sha256(String text) {
    return hashText(text);
  }

  /**
   * 计算字节数组的 SHA-256 哈希。
   *
   * @param data 要哈希的字节数组
   * @return 十六进制哈希字符串
   */
  public static String sha256(byte[] data) {
    if (data == null) {
      return null;
    }
    byte[] hash = SHA_256_DIGEST.digest(data);
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
