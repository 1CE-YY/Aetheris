/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

/**
 * 向量转换工具类。
 * <p>向量与字节数组转换，适配Redis FLOAT32格式。
 */
public final class VectorUtils {

  private VectorUtils() {
  }

  /**
   * 将向量转换为Redis Vector索引所需的二进制格式（FLOAT32）。
   * <p>每个float占4字节，使用小端序（Little-Endian）。
   */
  public static byte[] toBytes(float[] vector) {
    byte[] bytes = new byte[vector.length * 4];
    for (int i = 0; i < vector.length; i++) {
      int intBits = Float.floatToIntBits(vector[i]);
      bytes[i * 4] = (byte) intBits;
      bytes[i * 4 + 1] = (byte) (intBits >> 8);
      bytes[i * 4 + 2] = (byte) (intBits >> 16);
      bytes[i * 4 + 3] = (byte) (intBits >> 24);
    }
    return bytes;
  }

  /**
   * 将字节数组转换为向量（FLOAT32格式，小端序）。
   */
  public static float[] fromBytes(byte[] bytes) {
    float[] vector = new float[bytes.length / 4];
    for (int i = 0; i < vector.length; i++) {
      int intBits = ((bytes[i * 4 + 3] & 0xFF) << 24) |
          ((bytes[i * 4 + 2] & 0xFF) << 16) |
          ((bytes[i * 4 + 1] & 0xFF) << 8) |
          (bytes[i * 4] & 0xFF);
      vector[i] = Float.intBitsToFloat(intBits);
    }
    return vector;
  }
}
