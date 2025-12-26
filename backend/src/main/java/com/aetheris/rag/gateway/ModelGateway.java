package com.aetheris.rag.gateway;

/**
 * Gateway interface for AI model operations.
 *
 * <p>This interface defines the contract for all AI model interactions, providing a single
 * exit point for embedding and chat operations. This centralized approach ensures:
 *
 * <ul>
 *   <li>Consistent error handling and retry logic
 *   <li>Unified rate limiting and timeout management
 *   <li>Caching for cost optimization
 *   <li>Security and logging policies
 *   <li>Fallback strategies for service degradation
 * </ul>
 *
 * <p><strong>Important:</strong> All business code MUST use this gateway instead of calling
 * model APIs directly. This is enforced by the project constitution (Principle 4: Model Access).
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public interface ModelGateway {

  /**
   * Generates an embedding vector for the given text.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Checks cache first to avoid redundant API calls
   *   <li>Implements retry logic for transient failures
   *   <li>Enforces rate limiting
   *   <li>Sanitizes logs to protect sensitive data
   * </ul>
   *
   * @param text the input text to embed
   * @return the embedding vector (float array)
   * @throws ModelException if the embedding operation fails after all retries
   */
  float[] embed(String text);

  /**
   * Generates a chat response using the LLM.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Implements retry logic for transient failures
   *   <li>Enforces rate limiting
   *   <li>Provides fallback strategies for service degradation
   *   <li>Sanitizes logs to protect sensitive data
   * </ul>
   *
   * @param prompt the input prompt
   * @return the generated response text
   * @throws ModelException if the chat operation fails after all retries (unless fallback is
   *     enabled)
   */
  String chat(String prompt);

  /**
   * Generates a chat response with a system message.
   *
   * <p>Same as {@link #chat(String)}, but includes a system message to guide the model's
   * behavior.
   *
   * @param systemMessage the system message (e.g., instructions)
   * @param userMessage the user message
   * @return the generated response text
   * @throws ModelException if the chat operation fails after all retries
   */
  String chat(String systemMessage, String userMessage);
}
