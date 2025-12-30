package com.aetheris.rag.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

/**
 * 引用分块的位置信息。
 *
 * <p>此类表示分块在文档中的位置，支持 PDF 和 Markdown 格式。
 * 它提供完整的可追溯性，供用户导航到确切的源位置。
 *
 * <p>位置格式：
 *
 * <ul>
 *   <li>PDF：页码范围（起始和结束页码）
 *   <li>Markdown：章节路径（例如，"Chapter 1 > 1.1 Introduction"）
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CitationLocation.PdfLocation.class, name = "pdf"),
  @JsonSubTypes.Type(value = CitationLocation.MarkdownLocation.class, name = "markdown")
})
public abstract class CitationLocation {

  /**
   * 获取用于显示的格式化位置字符串。
   *
   * @return 人类可读的位置字符串
   */
  public abstract String getDisplayString();

  /**
   * 带页码范围的 PDF 位置。
   */
  public static class PdfLocation extends CitationLocation {

    private final int pageStart;
    private final int pageEnd;

    /**
     * 创建 PDF 位置。
     *
     * @param pageStart 起始页码（从 1 开始）
     * @param pageEnd 结束页码（从 1 开始，必须 >= pageStart）
     */
    @JsonCreator
    public PdfLocation(
        @JsonProperty("pageStart") int pageStart,
        @JsonProperty("pageEnd") int pageEnd) {
      if (pageStart < 1) {
        throw new IllegalArgumentException("pageStart must be >= 1");
      }
      if (pageEnd < pageStart) {
        throw new IllegalArgumentException("pageEnd must be >= pageStart");
      }

      this.pageStart = pageStart;
      this.pageEnd = pageEnd;
    }

    @JsonProperty("pageStart")
    public int getPageStart() {
      return pageStart;
    }

    @JsonProperty("pageEnd")
    public int getPageEnd() {
      return pageEnd;
    }

    @Override
    public String getDisplayString() {
      if (pageStart == pageEnd) {
        return "第 " + pageStart + " 页";
      }
      return "第 " + pageStart + "-" + pageEnd + " 页";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PdfLocation that = (PdfLocation) o;
      return pageStart == that.pageStart && pageEnd == that.pageEnd;
    }

    @Override
    public int hashCode() {
      return Objects.hash(pageStart, pageEnd);
    }

    @Override
    public String toString() {
      return "PdfLocation{" + "pageStart=" + pageStart + ", pageEnd=" + pageEnd + '}';
    }
  }

  /**
   * 带章节路径的 Markdown 位置。
   */
  public static class MarkdownLocation extends CitationLocation {

    private final String chapterPath;

    /**
     * 创建 Markdown 位置。
     *
     * @param chapterPath 章节路径（例如，"Chapter 1 > 1.1 Introduction"）
     */
    @JsonCreator
    public MarkdownLocation(@JsonProperty("chapterPath") String chapterPath) {
      if (chapterPath == null || chapterPath.isEmpty()) {
        throw new IllegalArgumentException("chapterPath cannot be null or empty");
      }

      this.chapterPath = chapterPath;
    }

    @JsonProperty("chapterPath")
    public String getChapterPath() {
      return chapterPath;
    }

    @Override
    public String getDisplayString() {
      return chapterPath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MarkdownLocation that = (MarkdownLocation) o;
      return Objects.equals(chapterPath, that.chapterPath);
    }

    @Override
    public int hashCode() {
      return Objects.hash(chapterPath);
    }

    @Override
    public String toString() {
      return "MarkdownLocation{" + "chapterPath='" + chapterPath + '\'' + '}';
    }
  }
}
