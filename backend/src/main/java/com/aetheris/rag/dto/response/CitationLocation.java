package com.aetheris.rag.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

/**
 * Location information for a citation chunk.
 *
 * <p>This class represents the position of a chunk within a document, supporting both PDF
 * and Markdown formats. It provides complete traceability for users to navigate to the exact
 * source location.
 *
 * <p>Location formats:
 *
 * <ul>
 *   <li>PDF: Page range (start and end page numbers)
 *   <li>Markdown: Chapter path (e.g., "Chapter 1 > 1.1 Introduction")
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
   * Gets the formatted location string for display.
   *
   * @return the human-readable location string
   */
  public abstract String getDisplayString();

  /**
   * PDF location with page range.
   */
  public static class PdfLocation extends CitationLocation {

    private final int pageStart;
    private final int pageEnd;

    /**
     * Creates a PDF location.
     *
     * @param pageStart the starting page number (1-based)
     * @param pageEnd the ending page number (1-based, must be >= pageStart)
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
   * Markdown location with chapter path.
   */
  public static class MarkdownLocation extends CitationLocation {

    private final String chapterPath;

    /**
     * Creates a Markdown location.
     *
     * @param chapterPath the chapter path (e.g., "Chapter 1 > 1.1 Introduction")
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
