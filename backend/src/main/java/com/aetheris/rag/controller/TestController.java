/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.controller;

import com.aetheris.rag.dto.response.Citation;
import com.aetheris.rag.gateway.ChatGateway;
import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.service.SearchService;
import com.aetheris.rag.service.VectorService;
import com.aetheris.rag.service.ResourceService;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.entity.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
  private final SearchService searchService;
  private final ResourceService resourceService;
  private final ResourceMapper resourceMapper;
  private final ChunkMapper chunkMapper;

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
   * 重建向量索引 - 删除并重新创建索引，重新向量化所有切片。
   *
   * @return 重建结果
   */
  @PostMapping("/rebuild-index")
  public Map<String, Object> rebuildIndex() {
    Map<String, Object> result = new HashMap<>();

    try {
      log.info("【测试】开始重建向量索引");
      long startTime = System.currentTimeMillis();

      vectorService.rebuildVectorIndex();

      long elapsed = System.currentTimeMillis() - startTime;
      result.put("success", true);
      result.put("elapsedMs", elapsed);
      result.put("message", "索引重建完成");

      log.info("【测试】向量索引重建完成，耗时: {}ms", elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "索引重建失败: " + e.getMessage());
      log.error("【测试】向量索引重建失败", e);
    }

    return result;
  }

  /**
   * 测试向量搜索。
   *
   * @param request 请求体，包含 query 和 topK 字段
   * @return 搜索结果
   */
  @PostMapping("/search")
  public Map<String, Object> testSearch(@RequestBody Map<String, Object> request) {
    Map<String, Object> result = new HashMap<>();

    try {
      String query = (String) request.get("query");
      int topK = request.containsKey("topK") ? (Integer) request.get("topK") : 3;

      log.info("【测试】开始向量搜索: query='{}', topK={}", query, topK);
      long startTime = System.currentTimeMillis();

      List<Citation> citations = searchService.search(query, topK);

      long elapsed = System.currentTimeMillis() - startTime;
      result.put("success", true);
      result.put("elapsedMs", elapsed);
      result.put("count", citations.size());
      result.put("citations", citations);
      result.put("message", "搜索成功");

      log.info("【测试】向量搜索完成，找到 {} 个结果，耗时: {}ms", citations.size(), elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "搜索失败: " + e.getMessage());
      log.error("【测试】向量搜索失败", e);
    }

    return result;
  }

  /**
   * 重新处理所有资源 - 为所有资源重新生成切片并向量化。
   *
   * @return 处理结果
   */
  @PostMapping("/reprocess-all")
  public Map<String, Object> reprocessAllResources() {
    Map<String, Object> result = new HashMap<>();

    try {
      log.info("【测试】开始重新处理所有资源");
      long startTime = System.currentTimeMillis();

      // 获取所有资源
      List<Resource> resources = resourceMapper.findPaged(0, 1000);
      log.info("找到 {} 个资源", resources.size());

      int successCount = 0;
      int failCount = 0;
      int totalChunksCreated = 0;

      for (Resource resource : resources) {
        try {
          log.info("处理资源: id={}, title={}, type={}",
              resource.getId(), resource.getTitle(), resource.getFileType());

          // 调用ResourceService的reprocessResource方法
          int chunkCount = resourceService.reprocessResource(resource.getId());
          successCount++;
          totalChunksCreated += chunkCount;
          log.info("  成功处理资源，生成了 {} 个切片", chunkCount);

        } catch (Exception e) {
          failCount++;
          log.error("  处理资源失败: id={}", resource.getId(), e);
        }
      }

      long elapsed = System.currentTimeMillis() - startTime;
      result.put("success", true);
      result.put("elapsedMs", elapsed);
      result.put("totalResources", resources.size());
      result.put("successCount", successCount);
      result.put("failCount", failCount);
      result.put("totalChunksCreated", totalChunksCreated);
      result.put("message", String.format("处理完成：成功 %d，失败 %d，总切片数 %d",
          successCount, failCount, totalChunksCreated));

      log.info("【测试】重新处理所有资源完成，耗时: {}ms", elapsed);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "处理失败: " + e.getMessage());
      log.error("【测试】重新处理所有资源失败", e);
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
