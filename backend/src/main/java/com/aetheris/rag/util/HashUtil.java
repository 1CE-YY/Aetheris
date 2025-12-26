package com.aetheris.rag.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for computing hash values.
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
   * Computes SHA-256 hash of normalized text.
   *
   * @param text the text to hash
   * @return hexadecimal hash string
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
   * Normalizes text for consistent hashing.
   *
   * @param text the text to normalize
   * @return normalized text
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
