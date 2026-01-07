package com.aetheris.rag.gateway;

import com.aetheris.rag.gateway.retry.ModelRetryStrategy;
import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 智谱 AI 聊天 API 操作的网关。
 *
 * <p>此类封装了与智谱 AI 聊天服务的所有交互，提供：
 *
 * <ul>
 *   <li>提示构建和格式化
 *   <li>使用指数退避的重试逻辑
 *   <li>LLM 不可用时的优雅降级
 *   <li>日志清理以保护敏感数据
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.3.0
 * @since 2025-12-26
 */
@Slf4j
@Component
public class ChatGateway {

  private final ChatService chatService;
  private final ModelRetryStrategy retryStrategy;
  private final String modelName;

  /**
   * 聊天服务接口（AiService 方式）。
   */
  @SystemMessage("你是一个专业的AI助手，擅长回答问题并提供有用的建议。")
  interface ChatService {
    @UserMessage("{{userMessage}}")
    String chat(String userMessage);
  }

  /**
   * 构造 ChatGateway。
   */
  public ChatGateway(
      @Value("${model-gateway.chat.model-name}") String modelName,
      @Value("${model-gateway.chat.api-key}") String apiKey,
      @Value("${model-gateway.chat.temperature}") Double temperature,
      @Value("${model-gateway.chat.top-p}") Double topP,
      @Value("${model-gateway.chat.retry.max-attempts}") int maxRetries,
      @Value("${model-gateway.chat.retry.backoff}") java.time.Duration retryBackoff) {

    this.modelName = modelName;
    this.retryStrategy = new ModelRetryStrategy(maxRetries, retryBackoff);

    // LangChain4j 1.9.1: 构建 ZhipuAiChatModel
    ZhipuAiChatModel model =
        ZhipuAiChatModel.builder()
            .apiKey(apiKey)
            .model(modelName)
            .temperature(temperature)
            .topP(topP)
            .maxRetries(maxRetries)
            .logRequests(true)
            .logResponses(true)
            .build();

    // 使用 AiServices 构建聊天服务
    this.chatService = AiServices.builder(ChatService.class)
        .chatModel(model)
        .build();

    log.info("初始化 ChatGateway，模型：{}，temperature：{}", modelName, temperature);
  }

  /**
   * 生成聊天响应。
   *
   * @param prompt 输入提示
   * @return 生成的响应文本
   * @throws RuntimeException 如果 API 调用在所有重试后失败
   */
  public String chat(String prompt) {
    // 记录日志（脱敏）
    String sanitizedPrompt = LogSanitizer.sanitize(prompt);
    log.info("调用 Chat API：model={}, promptPreview={}", modelName, sanitizedPrompt);

    // 使用重试策略调用 API
    String response =
        retryStrategy.executeWithRetry(
            () -> {
              return chatService.chat(prompt);
            });

    return response;
  }

  /**
   * 使用系统提示生成聊天响应。
   *
   * @param systemPrompt 系统提示
   * @param userPrompt 用户提示
   * @return 生成的响应文本
   * @throws RuntimeException 如果 API 调用在所有重试后失败
   */
  public String chat(String systemPrompt, String userPrompt) {
    // 记录日志（脱敏）
    String sanitizedSystemPrompt = LogSanitizer.sanitize(systemPrompt);
    String sanitizedUserPrompt = LogSanitizer.sanitize(userPrompt);
    log.info(
        "调用 Chat API（带系统提示）：model={}, systemPromptPreview={}, userPromptPreview={}",
        modelName,
        sanitizedSystemPrompt,
        sanitizedUserPrompt);

    // 使用重试策略调用 API
    // 注意：AiService 不支持动态系统提示，所以这里使用带系统提示的格式
    String combinedPrompt = String.format("[系统提示]%s\n\n[用户问题]%s", systemPrompt, userPrompt);
    String response =
        retryStrategy.executeWithRetry(
            () -> {
              return chatService.chat(combinedPrompt);
            });

    return response;
  }
}
