/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.gateway.ChatGateway;
import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器。
 *
 * <p>用于手动测试 Gateway 功能，仅在开发环境使用。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-07
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

  private final EmbeddingGateway embeddingGateway;
  private final ChatGateway chatGateway;
  private final VectorService vectorService;

  /**
   * 测试 EmbeddingGateway - 单个文本向量化。
   *
   * @param text 输入文本
   * @return 向量结果
   */
  @PostMapping("/embedding")
  public Map<String, Object> testEmbedding(@RequestBody Map<String, String> request) {
    Map<String, Object> result = new HashMap<>();

    try {
      String text = request.get("text");
      log.info("【测试】EmbeddingGateway - 输入文本: {}", text);

      long startTime = System.currentTimeMillis();
      float[] vector = embeddingGateway.embed(text);
      long elapsed = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("dimension", vector.length);
      result.put("elapsedMs", elapsed);
      result.put("preview", formatVector(vector, 5));
      result.put("message", "向量化成功");

      log.info("【测试】EmbeddingGateway - 成功，维度: {}, 耗时: {}ms", vector.length, elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "向量化失败: " + e.getMessage());
      log.error("【测试】EmbeddingGateway - 失败", e);
    }

    return result;
  }

  /**
   * 测试 ChatGateway - 生成回答。
   *
   * @param request 请求体，包含 prompt 字段
   * @return 生成的回答
   */
  @PostMapping("/chat")
  public Map<String, Object> testChat(@RequestBody Map<String, String> request) {
    Map<String, Object> result = new HashMap<>();

    try {
      String prompt = request.get("prompt");
      log.info("【测试】ChatGateway - 输入提示: {}", prompt);

      long startTime = System.currentTimeMillis();
      String response = chatGateway.chat(prompt);
      long elapsed = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("elapsedMs", elapsed);
      result.put("response", response);
      result.put("message", "生成成功");

      log.info("【测试】ChatGateway - 成功，耗时: {}ms", elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "生成失败: " + e.getMessage());
      log.error("【测试】ChatGateway - 失败", e);
    }

    return result;
  }

  /**
   * 触发向量化 - 向量化所有未向量化的切片。
   *
   * @return 向量化结果
   */
  @PostMapping("/vectorize")
  public Map<String, Object> testVectorize() {
    Map<String, Object> result = new HashMap<>();

    try {
      log.info("【测试】开始批量向量化");
      long startTime = System.currentTimeMillis();

      vectorService.vectorizeAllUnvectorized();

      long elapsed = System.currentTimeMillis() - startTime;
      result.put("success", true);
      result.put("elapsedMs", elapsed);
      result.put("message", "向量化完成");

      log.info("【测试】批量向量化完成，耗时: {}ms", elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "向量化失败: " + e.getMessage());
      log.error("【测试】批量向量化失败", e);
    }

    return result;
  }

  /**
   * 格式化向量预览。
   *
   * @param vector 向量数组
   * @param n 显示前 n 维
   * @return 格式化的字符串
   */
  private String formatVector(float[] vector, int n) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < Math.min(n, vector.length); i++) {
      if (i > 0) sb.append(", ");
      sb.append(String.format("%.4f", vector[i]));
    }
    sb.append("]");
    return sb.toString();
  }
}
