package com.aetheris.rag.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

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

  private final Converter<float[], byte[]> serializer;
  private final Converter<byte[], float[]> deserializer;

  /** Creates a new FloatArrayRedisSerializer. */
  public FloatArrayRedisSerializer() {
    this.serializer = new SerializingConverter();
    this.deserializer = new DeserializingConverter();
  }

  @Override
  public byte[] serialize(float[] floats) throws SerializationException {
    if (floats == null) {
      return new byte[0];
    }
    try {
      return serializer.convert(floats);
    } catch (Exception e) {
      throw new SerializationException("Failed to serialize float array", e);
    }
  }

  @Override
  public float[] deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try {
      return deserializer.convert(bytes);
    } catch (Exception e) {
      throw new SerializationException("Failed to deserialize float array", e);
    }
  }
}
