/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 模型网关的统一实现类。
 *
 * <p>此类作为 {@link ModelGateway} 接口的唯一实现，委托给具体的 Gateway：
 * <ul>
 *   <li>{@link EmbeddingGateway} 处理嵌入向量生成</li>
 *   <li>{@link ChatGateway} 处理聊天对话</li>
 * </ul>
 *
 * <p><strong>注意：</strong>当前为 stub 实现，返回 dummy 值。
 * 完整的模型调用将在 Phase 5 实现。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelGatewayImpl implements ModelGateway {

  private final EmbeddingGateway embeddingGateway;
  private final ChatGateway chatGateway;

  @Override
  public float[] embed(String text) {
    log.debug("委托给 EmbeddingGateway 生成嵌入向量");
    return embeddingGateway.embed(text);
  }

  @Override
  public String chat(String prompt) {
    log.debug("委托给 ChatGateway 生成聊天响应");
    return chatGateway.chat(prompt);
  }

  @Override
  public String chat(String systemMessage, String userMessage) {
    log.debug("委托给 ChatGateway 生成聊天响应（带系统消息）");
    return chatGateway.chat(systemMessage, userMessage);
  }
}
