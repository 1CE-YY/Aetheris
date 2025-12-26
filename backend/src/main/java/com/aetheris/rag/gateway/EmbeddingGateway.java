package com.aetheris.rag.gateway;

import com.aetheris.rag.gateway.cache.EmbeddingCache;
import com.aetheris.rag.gateway.retry.ModelRetryStrategy;
import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import com.aetheris.rag.util.HashUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.zhipuai.ZhipuAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gateway for Zhipu AI embedding API operations.
 *
 * <p>This class encapsulates all interactions with the Zhipu AI embedding service, providing:
 *
 * <ul>
 *   <li>Caching to eliminate redundant API calls (based on text hash)
 *   <li>Retry logic with exponential backoff for transient failures
 *   <li>Rate limiting to prevent API quota exhaustion
 *   <li>Log sanitization to protect sensitive data
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Component
public class EmbeddingGateway {

  private static final Logger log = LoggerFactory.getLogger(EmbeddingGateway.class);

  private final EmbeddingModel embeddingModel;
  private final EmbeddingCache cache;
  private final ModelRetryStrategy retryStrategy;
  private final String modelName;

  /**
   * Creates an embedding gateway with required dependencies.
   *
   * @param cache the embedding cache
   * @param modelName the embedding model name (from configuration)
   * @param apiKey the Zhipu AI API key
   * @param timeout the API timeout
   * @param maxRetries maximum retry attempts
   * @param retryBackoff base backoff duration
   */
  public EmbeddingGateway(
      EmbeddingCache cache,
      @Value("${model-gateway.embedding.model-name}") String modelName,
      @Value("${model-gateway.chat.api-key}") String apiKey,
      @Value("${model-gateway.embedding.timeout}") Duration timeout,
      @Value("${model-gateway.embedding.retry.max-attempts}") int maxRetries,
      @Value("${model-gateway.embedding.retry.backoff}") Duration retryBackoff) {

    this.cache = cache;
    this.modelName = modelName;
    this.retryStrategy = new ModelRetryStrategy(maxRetries, retryBackoff);

    // Initialize Zhipu AI embedding model
    this.embeddingModel =
        ZhipuAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .timeout(timeout)
            .build();

    log.info(
        "Initialized EmbeddingGateway with model: {}, timeout: {}, maxRetries: {}",
        modelName,
        timeout,
        maxRetries);
  }

  /**
   * Generates an embedding vector for the given text.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Normalizes the text and computes SHA-256 hash
   *   <li>Checks cache for existing embedding
   *   <li>If cache miss, calls API with retry logic
   *   <li>Stores result in cache
   *   <li>Returns the embedding vector
   * </ul>
   *
   * @param text the input text to embed
   * @return the embedding vector (float array)
   * @throws ModelException if the operation fails after all retries
   */
  public float[] embed(String text) {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("Text cannot be null or empty");
    }

    // Normalize text and compute hash
    String normalizedText = HashUtil.normalizeText(text);
    String textHash = HashUtil.hashText(normalizedText);

    // Check cache
    float[] cached = cache.get(textHash);
    if (cached != null) {
      log.debug("Embedding cache hit for textHash: {}", LogSanitizer.sanitize(textHash));
      return cached;
    }

    // Cache miss - call API with retry
    log.debug("Embedding cache miss for textHash: {}, calling API", LogSanitizer.sanitize(textHash));

    float[] embedding = retryStrategy.executeWithRetry(() -> {
      try {
        Response<Embedding> response = embeddingModel.embed(normalizedText);
        Embedding emb = response.content();

        if (emb == null || emb.vector() == null) {
          throw new ModelException("Embedding API returned null result");
        }

        return emb.vector();

      } catch (Exception e) {
        log.error("Embedding API call failed for textHash: {}",
            LogSanitizer.sanitize(textHash), e);
        throw e;
      }
    });

    // Store in cache
    cache.put(textHash, embedding);

    log.debug(
        "Generated embedding for textHash: {}, vector dimension: {}",
        LogSanitizer.sanitize(textHash),
        embedding.length);

    return embedding;
  }

  /**
   * Gets the model name being used.
   *
   * @return the model name
   */
  public String getModelName() {
    return modelName;
  }
}
