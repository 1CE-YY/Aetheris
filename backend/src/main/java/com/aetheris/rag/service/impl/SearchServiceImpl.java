/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.dto.response.Citation;
import com.aetheris.rag.dto.response.CitationLocation;
import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import com.aetheris.rag.gateway.EmbeddingGateway;
import com.aetheris.rag.mapper.ChunkMapper;
import com.aetheris.rag.mapper.ResourceMapper;
import com.aetheris.rag.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 语义检索服务实现类。
 *
 * <p>基于 Redis Stack 向量搜索实现 Top-K 语义检索，支持：
 *
 * <ul>
 *   <li>查询文本向量化（调用 EmbeddingGateway）
 *   <li>Redis 向量 KNN 搜索（FT.SEARCH + VECTOR KNN）
 *   <li>结果映射为 Citation 对象
 *   <li>按资源聚合（同一资源只保留相似度最高的切片）
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

  private final EmbeddingGateway embeddingGateway;
  private final ChunkMapper chunkMapper;
  private final ResourceMapper resourceMapper;
  private final StringRedisTemplate redisTemplate;

  /** Redis 向量索引名称 */
  @Value("${rag.vector.index-name:chunk_vector_index}")
  private String indexName;

  /** 默认 Top-K */
  @Value("${rag.retrieval.top-k:5}")
  private int defaultTopK;

  /** 相似度阈值 */
  @Value("${rag.retrieval.score-threshold:0.5}")
  private double scoreThreshold;

  @Override
  public List<Citation> search(String query, int topK) {
    log.info("执行语义检索：query='{}', topK={}", query, topK);

    // 1. 将查询文本向量化
    float[] queryVector = embeddingGateway.embed(query);
    log.debug("查询向量化完成，维度：{}", queryVector.length);

    // 2. 将向量转换为 Redis 格式（逗号分隔的字符串）
    String vectorStr = vectorToString(queryVector);

    // 3. 执行向量搜索
    List<Citation> citations = redisTemplate.execute((RedisCallback<List<Citation>>) connection -> {
      // FT.SEARCH index_name "*=>[KNN topK @vector query_vector AS __score]" PARAMS 2 query_vector vector_str RETURN 5 chunkId resourceId chunkIndex chunkText __score
      // execute(String command, byte[]... args) - 第一个参数是 String 命令名，其余是 byte[]...
      Object result = connection.execute(
        "FT.SEARCH",
        indexName.getBytes(),
        String.format("*=>[KNN %d @vector query_vector AS __score]", topK).getBytes(),
        "PARAMS".getBytes(), "2".getBytes(),
        "query_vector".getBytes(), vectorStr.getBytes(),
        "RETURN".getBytes(), "5".getBytes(),
        "chunkId".getBytes(), "resourceId".getBytes(), "chunkIndex".getBytes(), "chunkText".getBytes(), "__score".getBytes()
      );

      // 解析结果
      return parseSearchResults(result);
    });

    log.info("向量搜索完成，找到 {} 个结果", citations.size());
    return citations;
  }

  /**
   * 解析 Redis 搜索结果。
   *
   * @param result FT.SEARCH 命令的原始返回结果
   * @return Citation 列表
   */
  @SuppressWarnings("unchecked")
  private List<Citation> parseSearchResults(Object result) {
    List<Citation> citations = new ArrayList<>();

    if (result == null) {
      return citations;
    }

    try {
      // Redis 返回格式：[count, doc1, doc2, ...]
      // 每个 doc 是一个 List，包含 [id, field1, value1, field2, value2, ...]
      List<Object> resultList = (List<Object>) result;

      if (resultList.isEmpty()) {
        return citations;
      }

      // 跳过第一个元素（总数），从索引 1 开始是文档数据
      for (int i = 1; i < resultList.size(); i++) {
        try {
          List<Object> docData = (List<Object>) resultList.get(i);

          // docData[0] 是文档 ID（chunk:xxx）
          // docData[1:] 是字段-值对
          Map<String, String> fields = new HashMap<>();
          for (int j = 1; j < docData.size(); j += 2) {
            if (j + 1 < docData.size()) {
              String field = (String) docData.get(j);
              String value = (String) docData.get(j + 1);
              fields.put(field, value);
            }
          }

          Citation citation = mapFieldsToCitation(fields);
          if (citation.getScore() >= scoreThreshold) {
            citations.add(citation);
          }
        } catch (Exception e) {
          log.error("解析搜索结果失败，索引: {}", i, e);
        }
      }
    } catch (ClassCastException e) {
      log.error("搜索结果格式错误", e);
    }

    return citations;
  }

  /**
   * 将字段映射为 Citation 对象。
   *
   * @param fields Redis Hash 字段
   * @return Citation 对象
   */
  private Citation mapFieldsToCitation(Map<String, String> fields) {
    String chunkId = fields.get("chunkId");
    String resourceIdStr = fields.get("resourceId");
    String chunkIndexStr = fields.get("chunkIndex");
    String chunkText = fields.get("chunkText");

    // 提取相似度分数
    double score = 0.0;
    String scoreStr = fields.get("__score");
    if (scoreStr != null && !scoreStr.isEmpty()) {
      try {
        score = Double.parseDouble(scoreStr);
      } catch (NumberFormatException e) {
        log.warn("分数格式错误: {}", scoreStr);
      }
    }

    // 查询数据库获取完整信息
    Long resourceId = Long.parseLong(resourceIdStr);
    Chunk chunk = chunkMapper.findById(Long.parseLong(chunkId));
    Resource resource = resourceMapper.findById(resourceId);

    if (chunk == null || resource == null) {
      log.warn("切片或资源不存在：chunkId={}, resourceId={}", chunkId, resourceId);
      throw new IllegalArgumentException("切片或资源不存在");
    }

    // 构建位置信息
    CitationLocation location = buildLocation(chunk);

    // 构建 Citation 对象
    return new Citation(
        resourceId.toString(),
        resource.getTitle(),
        chunkId,
        Integer.parseInt(chunkIndexStr),
        location,
        truncateText(chunkText, 200), // 限制 snippet 长度
        score);
  }

  @Override
  public List<Citation> searchAggregated(String query, int topK) {
    log.info("执行聚合语义检索：query='{}', topK={}", query, topK);

    // 1. 执行普通检索
    List<Citation> allCitations = search(query, topK * 2); // 检索更多结果用于聚合
    log.debug("检索到 {} 个切片，开始按资源聚合", allCitations.size());

    // 2. 按资源 ID 聚合，保留每个资源相似度最高的切片
    Map<String, Citation> bestCitations =
        allCitations.stream()
            .collect(
                Collectors.toMap(
                    Citation::getResourceId,
                    citation -> citation,
                    // 如果同一资源有多个切片，保留相似度最高的
                    (existing, incoming) ->
                        incoming.getScore() > existing.getScore() ? incoming : existing));

    // 3. 按相似度排序并取 Top-K
    List<Citation> aggregatedCitations =
        bestCitations.values().stream()
            .sorted(Comparator.comparingDouble(Citation::getScore).reversed())
            .limit(topK)
            .toList();

    log.info("聚合后的结果数量：{}", aggregatedCitations.size());
    return aggregatedCitations;
  }

  /**
   * 根据 Chunk 信息构建位置对象。
   *
   * @param chunk 切片实体
   * @return CitationLocation 对象（PdfLocation 或 MarkdownLocation）
   */
  private CitationLocation buildLocation(Chunk chunk) {
    // 优先使用 PDF 页码信息
    if (chunk.getPageStart() != null && chunk.getPageEnd() != null) {
      return new CitationLocation.PdfLocation(
          chunk.getPageStart(), chunk.getPageEnd());
    }

    // 其次使用 Markdown 章节路径
    if (chunk.getChapterPath() != null && !chunk.getChapterPath().isEmpty()) {
      return new CitationLocation.MarkdownLocation(chunk.getChapterPath());
    }

    // 如果都没有，返回一个默认的 PDF 位置（页码范围未知）
    log.warn("切片没有位置信息：chunkId={}", chunk.getId());
    return new CitationLocation.PdfLocation(0, 0);
  }

  /**
   * 将向量转换为字符串（逗号分隔）。
   *
   * @param vector 向量数组
   * @return 逗号分隔的字符串
   */
  private String vectorToString(float[] vector) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(vector[i]);
    }
    return sb.toString();
  }

  /**
   * 截断文本到指定长度。
   *
   * @param text 原始文本
   * @param maxLength 最大长度
   * @return 截断后的文本
   */
  private String truncateText(String text, int maxLength) {
    if (text == null) {
      return "";
    }
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}
