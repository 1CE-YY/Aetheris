package com.aetheris.rag.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Custom Redis serializer for float arrays.
 *
 * <p>This serializer handles float[] arrays by converting them to/from byte arrays using Java
 * serialization. This ensures proper serialization and deserialization of embedding vectors.
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public class FloatArrayRedisSerializer implements RedisSerializer<float[]> {

  @Override
  public byte[] serialize(float[] floats) {
    if (floats == null) {
      return new byte[0];
    }
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(floats);
      oos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize float array", e);
    }
  }

  @Override
  public float[] deserialize(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis)) {
      Object obj = ois.readObject();
      if (obj instanceof float[]) {
        return (float[]) obj;
      }
      throw new RuntimeException("Deserialized object is not a float array");
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to deserialize float array", e);
    }
  }
}
