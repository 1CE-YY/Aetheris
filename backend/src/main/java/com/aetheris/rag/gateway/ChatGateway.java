package com.aetheris.rag.gateway;

import com.aetheris.rag.gateway.retry.ModelRetryStrategy;
import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.zhipuai.ZhipuAiChatModel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gateway for Zhipu AI chat API operations.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@Component
public class ChatGateway {

  private static final Logger log = LoggerFactory.getLogger(ChatGateway.class);

  private final ChatLanguageModel chatModel;
  private final ModelRetryStrategy retryStrategy;
  private final String modelName;

  public ChatGateway(
      @Value("${model-gateway.chat.model-name}") String modelName,
      @Value("${model-gateway.chat.api-key}") String apiKey,
      @Value("${model-gateway.chat.temperature}") Double temperature,
      @Value("${model-gateway.chat.top-p}") Double topP,
      @Value("${model-gateway.chat.max-tokens}") Integer maxTokens,
      @Value("${model-gateway.chat.timeout}") Duration timeout,
      @Value("${model-gateway.chat.retry.max-attempts}") int maxRetries,
      @Value("${model-gateway.chat.retry.backoff}") Duration retryBackoff) {

    this.modelName = modelName;
    this.retryStrategy = new ModelRetryStrategy(maxRetries, retryBackoff);

    this.chatModel =
        ZhipuAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(temperature)
            .topP(topP)
            .maxTokens(maxTokens)
            .timeout(timeout)
            .build();

    log.info("Initialized ChatGateway with model: {}", modelName);
  }

  /**
   * Generates a chat response.
   *
   * @param prompt the input prompt
   * @return the generated response
   */
  public String chat(String prompt) {
    if (prompt == null || prompt.trim().isEmpty()) {
      throw new IllegalArgumentException("Prompt cannot be null or empty");
    }

    log.debug("Calling chat API with prompt length: {}", prompt.length());

    return retryStrategy.executeWithRetry(() -> {
      try {
        Response<AiMessage> response = chatModel.generate(prompt);
        AiMessage aiMessage = response.content();

        if (aiMessage == null || aiMessage.text() == null) {
          throw new ModelException("Chat API returned null result");
        }

        String result = aiMessage.text();
        log.debug("Chat API returned response length: {}", result.length());
        return result;

      } catch (Exception e) {
        log.error("Chat API call failed for prompt: {}", LogSanitizer.sanitize(prompt, 50), e);
        throw e;
      }
    });
  }

  public String getModelName() {
    return modelName;
  }
}
