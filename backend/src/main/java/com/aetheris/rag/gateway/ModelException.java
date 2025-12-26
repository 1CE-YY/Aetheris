package com.aetheris.rag.gateway;

/**
 * Exception thrown when model operations fail.
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
