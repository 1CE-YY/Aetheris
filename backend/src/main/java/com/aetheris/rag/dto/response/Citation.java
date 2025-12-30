package com.aetheris.rag.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Value;

/**
 * 用于引用 RAG 答案和推荐中的证据来源的引用对象。
 *
 * <p>此类表示将 AI 生成的响应链接到其源材料的引用。每个引用提供完整的可追溯性，允许用户验证信息并导航到原始文档中的确切位置。
 *
 * <p>引用结构遵循 FR-014 和 FR-020 中定义的规范：
 *
 * <ul>
 *   <li>稳定的 JSON 结构供前端使用
 *   <li>支持 PDF（页码范围）和 Markdown（章节路径）位置格式
 *   <li>包含相似度分数用于排名
 *   <li>100% 可追溯到源文档
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@Value
public class Citation {

  @JsonProperty("resourceId")
  String resourceId;

  @JsonProperty("resourceTitle")
  String resourceTitle;

  @JsonProperty("chunkId")
  String chunkId;

  @JsonProperty("chunkIndex")
  int chunkIndex;

  @JsonProperty("location")
  CitationLocation location;

  @JsonProperty("snippet")
  String snippet;

  @JsonProperty("score")
  double score;

  /**
   * 创建完整的引用。
   *
   * <p>构造函数中包含完整的参数校验逻辑，确保数据完整性
   *
   * @param resourceId 资源 ID（UUID）
   * @param resourceTitle 资源标题
   * @param chunkId 分块 ID（UUID）
   * @param chunkIndex 分块索引（在文档中的从 0 开始的位置）
   * @param location 位置信息（页码范围或章节路径）
   * @param snippet 支持答案的文本摘录（100-200 个字符）
   * @param score 相似度分数（0.0 到 1.0）
   */
  public Citation(
      String resourceId,
      String resourceTitle,
      String chunkId,
      int chunkIndex,
      CitationLocation location,
      String snippet,
      double score) {

    if (resourceId == null || resourceId.isEmpty()) {
      throw new IllegalArgumentException("resourceId cannot be null or empty");
    }
    if (resourceTitle == null || resourceTitle.isEmpty()) {
      throw new IllegalArgumentException("resourceTitle cannot be null or empty");
    }
    if (chunkId == null || chunkId.isEmpty()) {
      throw new IllegalArgumentException("chunkId cannot be null or empty");
    }
    if (chunkIndex < 0) {
      throw new IllegalArgumentException("chunkIndex must be >= 0");
    }
    if (location == null) {
      throw new IllegalArgumentException("location cannot be null");
    }
    if (snippet == null || snippet.isEmpty()) {
      throw new IllegalArgumentException("snippet cannot be null or empty");
    }
    if (score < 0.0 || score > 1.0) {
      throw new IllegalArgumentException("score must be between 0.0 and 1.0");
    }

    this.resourceId = resourceId;
    this.resourceTitle = resourceTitle;
    this.chunkId = chunkId;
    this.chunkIndex = chunkIndex;
    this.location = location;
    this.snippet = snippet;
    this.score = score;
  }

  /**
   * 判断两个引用是否相等。
   *
   * <p>只比较核心字段：resourceId、chunkId、chunkIndex、score
   *
   * @param o 其他对象
   * @return 如果核心字段相等则返回 true
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Citation citation = (Citation) o;
    return chunkIndex == citation.chunkIndex
        && Double.compare(citation.score, score) == 0
        && Objects.equals(resourceId, citation.resourceId)
        && Objects.equals(chunkId, citation.chunkId);
  }

  /**
   * 生成哈希码。
   *
   * <p>只使用核心字段：resourceId、chunkId、chunkIndex、score
   *
   * @return 哈希码
   */
  @Override
  public int hashCode() {
    return Objects.hash(resourceId, chunkId, chunkIndex, score);
  }
}
