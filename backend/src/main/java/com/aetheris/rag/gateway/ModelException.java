package com.aetheris.rag.gateway;

/**
 * 模型操作失败时抛出的异常。
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
public class ModelException extends RuntimeException {

  public ModelException(String message) {
    super(message);
  }

  public ModelException(String message, Throwable cause) {
    super(message, cause);
  }
}
