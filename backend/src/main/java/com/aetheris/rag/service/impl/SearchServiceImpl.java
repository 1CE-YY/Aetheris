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
import com.aetheris.rag.util.PerformanceTimer;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ArrayOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
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

  /**
   * 自定义 Redis 命令：FT.SEARCH
   */
  private enum CustomCommand implements ProtocolKeyword {
    FT_SEARCH("FT.SEARCH");

    private final byte[] bytes;

    CustomCommand(String name) {
      this.bytes = name.getBytes();
    }

    @Override
    public byte[] getBytes() {
      return bytes;
    }
  }

  @Override
  public List<Citation> search(String query, int topK) {
    log.info("执行语义检索：query='{}', topK={}", query, topK);

    PerformanceTimer timer = new PerformanceTimer();

    // 1. 将查询文本向量化
    timer.recordStage("embedding");
    float[] queryVector = embeddingGateway.embed(query);
    log.debug("查询向量化完成，维度：{}", queryVector.length);
    timer.endStage();

    // 2. 将向量转换为二进制格式（FLOAT32）
    timer.recordStage("vector_conversion");
    byte[] queryVectorBytes = vectorToBytes(queryVector);
    log.debug("查询向量已转换为二进制格式，大小：{} 字节", queryVectorBytes.length);
    timer.endStage();

    // 3. 执行向量搜索
    timer.recordStage("vector_search");
    List<Citation> citations = redisTemplate.execute((RedisCallback<List<Citation>>) connection -> {
      try {
        // 获取 Lettuce 原生连接 - 使用反射处理代理
        Object nativeConnection = connection.getNativeConnection();

        if (!(nativeConnection instanceof RedisAsyncCommands)) {
          throw new RuntimeException("无法获取 RedisAsyncCommands，当前类型: " + nativeConnection.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        RedisAsyncCommands<byte[], byte[]> async = (RedisAsyncCommands<byte[], byte[]>) nativeConnection;

        // 构建 FT.SEARCH 命令参数
        CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
            .add(indexName.getBytes())
            .add(String.format("*=>[KNN %d @vector $query_vector AS __score]", topK).getBytes())
            .add("PARAMS".getBytes())
            .add("2".getBytes())
            .add("query_vector".getBytes())
            .add(queryVectorBytes)
            .add("RETURN".getBytes())
            .add("5".getBytes())
            .add("chunkId".getBytes())
            .add("resourceId".getBytes())
            .add("chunkIndex".getBytes())
            .add("chunkText".getBytes())
            .add("__score".getBytes())
            .add("DIALECT".getBytes())
            .add("2".getBytes());

        // 使用 dispatch 执行自定义命令，用 ArrayOutput 替代 NestedMultiOutput
        RedisFuture<List<Object>> future = async.dispatch(
            CustomCommand.FT_SEARCH,
            new ArrayOutput<>(ByteArrayCodec.INSTANCE),
            args
        );

        // 等待结果
        List<Object> result = future.get();
        System.out.println("=== FT.SEARCH 原始结果 ===");
        System.out.println("结果类型: " + (result != null ? result.getClass().getName() : "null"));
        System.out.println("结果大小: " + (result != null ? result.size() : 0));
        if (result != null && !result.isEmpty()) {
          for (int i = 0; i < Math.min(3, result.size()); i++) {
            Object item = result.get(i);
            System.out.println("  [" + i + "] 类型: " + item.getClass().getName() + ", 值: " + item);
          }
        }
        log.debug("FT.SEARCH 原始结果: {}", result);
        log.debug("结果类型: {}, 大小: {}", result != null ? result.getClass().getName() : "null", result != null ? result.size() : 0);
        return parseSearchResults(result);

      } catch (Exception e) {
        log.error("向量搜索失败", e);
        throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
      }
    });
    timer.endStage();

    // 4. 记录性能统计
    long totalTime = timer.getElapsedMs();
    Long embeddingTime = timer.getStageDuration("embedding");
    Long vectorSearchTime = timer.getStageDuration("vector_search");

    log.info("向量搜索完成，找到 {} 个结果，总耗时 {}ms（向量化 {}ms，搜索 {}ms）",
        citations.size(), totalTime, embeddingTime, vectorSearchTime);

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

    log.debug("开始解析搜索结果，result 类型: {}", result != null ? result.getClass().getName() : "null");

    if (result == null) {
      log.warn("搜索结果为 null");
      return citations;
    }

    try {
      // Redis FT.SEARCH 返回格式：
      // [总结果数(int), 文档ID(bytes), [字段-值对(bytes)], ...]
      // 或者 NestedMultiOutput 可能返回 [key, val, key, val, ...] 其中 key="results" 包含文档列表
      List<Object> resultList = (List<Object>) result;

      log.debug("结果列表大小: {}", resultList.size());
      if (resultList.size() < 3) {
        log.warn("搜索结果格式错误，大小 < 3");
        return citations;
      }

      // 检查第一个元素是否是数字（总结果数）
      if (resultList.get(0) instanceof Number) {
        // 标准 FT.SEARCH 返回格式: [总数, docId1, fields1, docId2, fields2, ...]
        int totalResults = ((Number) resultList.get(0)).intValue();
        log.info("Redis 返回 {} 个搜索结果", totalResults);

        // 解析文档，从索引 1 开始，每次跳 2（docId + fields）
        for (int i = 1; i < resultList.size(); i += 2) {
          if (i + 1 >= resultList.size()) break;

          try {
            byte[] docIdBytes = (byte[]) resultList.get(i);
            String docId = new String(docIdBytes, "UTF-8");
            log.debug("处理文档: {}", docId);

            List<Object> fieldsList = (List<Object>) resultList.get(i + 1);

            // fieldsList 是扁平的 [fieldName, fieldValue, ...]
            Map<String, String> fields = new HashMap<>();
            for (int j = 0; j < fieldsList.size(); j += 2) {
              if (j + 1 < fieldsList.size()) {
                byte[] fieldBytes = (byte[]) fieldsList.get(j);
                String field = new String(fieldBytes, "UTF-8");

                // 跳过 vector 字段（二进制数据）
                if ("vector".equals(field)) {
                  log.debug("  跳过 vector 字段（二进制数据）");
                  continue;
                }

                byte[] valueBytes = (byte[]) fieldsList.get(j + 1);
                String value = new String(valueBytes, "UTF-8");

                if (value.length() > 100) {
                  log.debug("  字段: {} -> {} (长度: {})", field, value.substring(0, 100) + "...", value.length());
                } else {
                  log.debug("  字段: {} -> {}", field, value);
                }

                fields.put(field, value);
              }
            }

            log.debug("文档 {} 解析到 {} 个字段: {}", docId, fields.size(), fields.keySet());

            Citation citation = mapFieldsToCitation(fields);
            log.info("文档 {} 评分: {}", docId, citation.getScore());

            if (citation.getScore() >= scoreThreshold) {
              citations.add(citation);
              log.info("✓ 添加文档到结果: chunkId={}", citation.getChunkId());
            } else {
              log.info("✗ 文档评分低于阈值 {}，跳过", scoreThreshold);
            }
          } catch (Exception e) {
            log.error("解析文档失败，索引: {}", i, e);
          }
        }
      } else {
        // NestedMultiOutput 格式: [key, val, key, val, ...]
        log.debug("检测到 NestedMultiOutput 格式");
        for (int i = 0; i < resultList.size(); i += 2) {
          if (i + 1 >= resultList.size()) break;

          byte[] keyBytes = (byte[]) resultList.get(i);
          String key = new String(keyBytes, "UTF-8");

          if ("results".equals(key)) {
            List<Object> resultsValue = (List<Object>) resultList.get(i + 1);
            log.debug("找到 results 键，值类型: {}, 大小: {}",
                resultsValue.getClass().getName(), resultsValue.size());

            // 打印 resultsValue 的每个元素类型
            for (int idx = 0; idx < resultsValue.size(); idx++) {
              Object elem = resultsValue.get(idx);
              String elemType = elem.getClass().getName();
              if (elem instanceof List) {
                log.debug("  resultsValue[{}]: List, 大小={}", idx, ((List<?>) elem).size());
              } else if (elem instanceof byte[]) {
                String str = new String((byte[]) elem, "UTF-8");
                log.debug("  resultsValue[{}]: byte[], 值={}", idx, str.length() > 50 ? str.substring(0, 50) + "..." : str);
              } else {
                log.debug("  resultsValue[{}]: {}, 值={}", idx, elemType, elem);
              }
            }

            // resultsValue 可能的结构：
            // 1. [[docId, field1, val1, ...], extra, ...]
            // 2. [docId, field1, val1, field2, val2, ...]

            // 检查第一个元素是 List 还是 byte[]
            if (resultsValue.isEmpty()) {
              log.warn("results 值为空");
              break;
            }

            Object firstElement = resultsValue.get(0);
            if (firstElement instanceof List) {
              // 结构1: [[docId, fields...], ...]
              List<Object> docList = (List<Object>) firstElement;
              log.debug("文档列表大小: {}", docList.size());

              // 打印 docList 中每个元素的类型
              for (int idx = 0; idx < docList.size(); idx++) {
                Object elem = docList.get(idx);
                if (elem instanceof byte[]) {
                  try {
                    String str = new String((byte[]) elem, "UTF-8");
                    log.debug("  docList[{}]: byte[] = \"{}\"", idx, str.length() > 50 ? str.substring(0, 50) + "..." : str);
                  } catch (Exception e) {
                    log.debug("  docList[{}]: byte[] (二进制数据)", idx);
                  }
                } else if (elem instanceof List) {
                  log.debug("  docList[{}]: List, 大小={}", idx, ((List<?>) elem).size());
                } else {
                  log.debug("  docList[{}]: {}", idx, elem.getClass().getSimpleName());
                }
              }

              if (docList.isEmpty()) {
                log.warn("文档列表为空");
                break;
              }

              try {
                // docList 结构: ["id", "chunk:327", "extra_attributes", [字段列表], "values", []]
                // 实际文档ID在 docList[1]，字段列表在 docList[3]
                if (docList.size() < 4) {
                  log.warn("文档结构不完整，大小: {}", docList.size());
                  break;
                }

                byte[] docIdBytes = (byte[]) docList.get(1);
                String docId = new String(docIdBytes, "UTF-8");
                log.debug("处理文档 ID: {}", docId);

                // 字段列表在索引 3
                Object fieldsObj = docList.get(3);
                if (!(fieldsObj instanceof List)) {
                  log.error("docList[3] 不是 List，而是: {}", fieldsObj.getClass().getName());
                  break;
                }

                List<Object> fieldsList = (List<Object>) fieldsObj;
                log.debug("字段列表大小: {}", fieldsList.size());

                Map<String, String> fields = new HashMap<>();
                for (int j = 0; j < fieldsList.size(); j += 2) {
                  if (j + 1 < fieldsList.size()) {
                    byte[] fieldBytes = (byte[]) fieldsList.get(j);
                    String field = new String(fieldBytes, "UTF-8");

                    // 跳过 vector 字段
                    if ("vector".equals(field)) {
                      log.debug("  跳过 vector 字段");
                      continue;
                    }

                    byte[] valueBytes = (byte[]) fieldsList.get(j + 1);
                    String value = new String(valueBytes, "UTF-8");

                    if (value.length() > 100) {
                      log.debug("  字段: {} -> {} (长度: {})", field, value.substring(0, 100) + "...", value.length());
                    } else {
                      log.debug("  字段: {} -> {}", field, value);
                    }

                    fields.put(field, value);
                  }
                }

                log.debug("文档 {} 解析到 {} 个字段: {}", docId, fields.size(), fields.keySet());

                Citation citation = mapFieldsToCitation(fields);
                if (citation.getScore() >= scoreThreshold) {
                  citations.add(citation);
                }
              } catch (Exception e) {
                log.error("解析文档失败", e);
              }
            } else if (firstElement instanceof byte[]) {
              // 结构2: [docId, field1, val1, ...]
              log.debug("扁平文档结构");
              try {
                byte[] docIdBytes = (byte[]) resultsValue.get(0);
                String docId = new String(docIdBytes, "UTF-8");
                log.debug("处理文档 ID: {}", docId);

                Map<String, String> fields = new HashMap<>();
                for (int j = 1; j < resultsValue.size(); j += 2) {
                  if (j + 1 < resultsValue.size()) {
                    Object fieldObj = resultsValue.get(j);
                    Object valueObj = resultsValue.get(j + 1);

                    if (fieldObj instanceof byte[] && valueObj instanceof byte[]) {
                      byte[] fieldBytes = (byte[]) fieldObj;
                      String field = new String(fieldBytes, "UTF-8");

                      // 跳过 vector 字段
                      if ("vector".equals(field)) {
                        continue;
                      }

                      byte[] valueBytes = (byte[]) valueObj;
                      String value = new String(valueBytes, "UTF-8");
                      fields.put(field, value);
                    }
                  }
                }

                log.debug("文档字段: {}", fields.keySet());

                Citation citation = mapFieldsToCitation(fields);
                if (citation.getScore() >= scoreThreshold) {
                  citations.add(citation);
                }
              } catch (Exception e) {
                log.error("解析扁平文档失败", e);
              }
            } else {
              log.error("results 第一个元素类型未知: {}", firstElement.getClass().getName());
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      log.error("搜索结果解析错误", e);
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
   * <p>用于 KNN 搜索的 PARAMS 参数传递。
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
   * 将向量转换为 Redis Vector 索引所需的二进制格式（FLOAT32）。
   *
   * <p>与存储格式保持一致，查询向量也必须是 FLOAT32 字节格式。
   *
   * @param vector 向量数组
   * @return 字节数组（FLOAT32 格式）
   */
  private byte[] vectorToBytes(float[] vector) {
    byte[] bytes = new byte[vector.length * 4]; // 每个 float 4 字节
    for (int i = 0; i < vector.length; i++) {
      int intBits = Float.floatToIntBits(vector[i]);
      // 使用小端序（Little-Endian）- Redis/RediSearch 要求
      bytes[i * 4] = (byte) intBits;              // 最低位字节
      bytes[i * 4 + 1] = (byte) (intBits >> 8);
      bytes[i * 4 + 2] = (byte) (intBits >> 16);
      bytes[i * 4 + 3] = (byte) (intBits >> 24);  // 最高位字节
    }
    return bytes;
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
