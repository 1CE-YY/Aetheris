package com.aetheris.rag.gateway;

import com.aetheris.rag.gateway.cache.EmbeddingCache;
import com.aetheris.rag.gateway.retry.ModelRetryStrategy;
import com.aetheris.rag.gateway.sanitize.LogSanitizer;
import com.aetheris.rag.util.HashUtil;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 智谱 AI 嵌入 API 操作的网关。
 *
 * <p>此类封装了与智谱 AI 嵌入服务的所有交互，提供：
 *
 * <ul>
 *   <li>缓存以消除冗余 API 调用（基于文本哈希）
 *   <li>使用指数退避的重试逻辑以处理瞬态故障
 *   <li>速率限制以防止 API 配额耗尽
 *   <li>日志清理以保护敏感数据
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.1.0
 * @since 2025-12-26
 */
@Slf4j
@Component
public class EmbeddingGateway {

  private final ZhipuAiEmbeddingModel embeddingModel;
  private final EmbeddingCache cache;
  private final ModelRetryStrategy retryStrategy;
  private final String modelName;
  private final boolean cacheEnabled;

  /**
   * 构造 EmbeddingGateway。
   *
   * @param cache Embedding 缓存
   * @param modelName 模型名称（如 embedding-3）
   * @param apiKey 智谱 AI API 密钥
   * @param timeout 超时时间
   * @param maxRetries 最大重试次数
   * @param retryBackoff 重试退避时间
   * @param cacheEnabled 是否启用缓存
   */
  public EmbeddingGateway(
      EmbeddingCache cache,
      @Value("${model-gateway.embedding.model-name}") String modelName,
      @Value("${model-gateway.chat.api-key}") String apiKey,
      @Value("${model-gateway.embedding.timeout}") Duration timeout,
      @Value("${model-gateway.embedding.retry.max-attempts}") int maxRetries,
      @Value("${model-gateway.embedding.retry.backoff}") Duration retryBackoff,
      @Value("${model-gateway.embedding.cache.enabled:true}") boolean cacheEnabled) {

    this.cache = cache;
    this.modelName = modelName;
    this.cacheEnabled = cacheEnabled;
    this.retryStrategy = new ModelRetryStrategy(maxRetries, retryBackoff);

    // LangChain4j 1.9.1: 使用 ZhipuAiEmbeddingModel 直接构建
    this.embeddingModel =
        ZhipuAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .model(modelName)
            .maxRetries(maxRetries)
            .logRequests(true)
            .logResponses(true)
            .build();

    log.info("初始化 EmbeddingGateway，模型：{}，缓存：{}", modelName, cacheEnabled ? "启用" : "禁用");
  }

  /**
   * 为给定文本生成嵌入向量。
   *
   * @param text 要嵌入的输入文本
   * @return 嵌入向量（embedding-3 默认为 2048 维）
   * @throws RuntimeException 如果 API 调用在所有重试后失败
   */
  public float[] embed(String text) {
    // 规范化文本（去除冗余空白、统一换行）
    String normalizedText = text.trim().replaceAll("\\s+", " ");

    // 计算文本哈希
    String textHash = HashUtil.sha256(normalizedText);

    // 查询缓存
    if (cacheEnabled) {
      float[] cached = cache.get(textHash);
      if (cached != null) {
        log.debug("Embedding 缓存命中：textHash={}", textHash.substring(0, 8));
        return cached;
      }
    }

    // 记录日志（脱敏）
    String sanitizedText = LogSanitizer.sanitize(normalizedText);
    log.info("调用 Embedding API：model={}, textPreview={}", modelName, sanitizedText);

    // 使用重试策略调用 API
    float[] embedding =
        retryStrategy.executeWithRetry(
            () -> {
              // LangChain4j 1.9.1: 使用 embedAll 方法，传入 TextSegment 列表
              List<dev.langchain4j.data.embedding.Embedding> embeddings =
                  embeddingModel.embedAll(List.of(TextSegment.from(normalizedText))).content();

              // 转换为 float 数组
              return embeddings.get(0).vector();
            });

    // 缓存结果
    if (cacheEnabled) {
      cache.put(textHash, embedding);
      log.debug("Embedding 已缓存：textHash={}", textHash.substring(0, 8));
    }

    return embedding;
  }

  /**
   * 批量为多个文本生成嵌入向量。
   *
   * @param texts 输入文本列表
   * @return 嵌入向量列表
   * @throws RuntimeException 如果 API 调用在所有重试后失败
   */
  public float[][] embedBatch(List<String> texts) {
    log.info("批量调用 Embedding API：model={}, count={}", modelName, texts.size());

    // 批量处理每个文本
    float[][] embeddings = new float[texts.size()][];
    for (int i = 0; i < texts.size(); i++) {
      embeddings[i] = embed(texts.get(i));
    }

    return embeddings;
  }
}
