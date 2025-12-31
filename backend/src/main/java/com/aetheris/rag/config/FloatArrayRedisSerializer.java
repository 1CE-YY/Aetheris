package com.aetheris.rag.config;

import com.aetheris.rag.exception.InternalServerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/**
 * 用于浮点数组的自定义 Redis 序列化器。
 *
 * <p>此序列化器通过使用 Java 序列化将 float[] 数组转换为字节数组来处理它们。
 * 这确保了嵌入向量的正确序列化和反序列化。
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
      throw new InternalServerException("浮点数组序列化失败", e);
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
      throw new InternalServerException("反序列化对象不是浮点数组类型");
    } catch (IOException | ClassNotFoundException e) {
      throw new InternalServerException("浮点数组反序列化失败", e);
    }
  }
}
