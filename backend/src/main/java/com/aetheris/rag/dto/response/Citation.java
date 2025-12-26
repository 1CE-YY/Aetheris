package com.aetheris.rag.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Citation object for referencing evidence sources in RAG answers and recommendations.
 *
 * <p>This class represents a citation that links an AI-generated response to its source
 * material. Each citation provides complete traceability, allowing users to verify the
 * information and navigate to the exact location in the original document.
 *
 * <p>Citation structure follows the specification defined in FR-014 and FR-020:
 *
 * <ul>
 *   <li>Stable JSON structure for frontend consumption
 *   <li>Supports both PDF (page ranges) and Markdown (chapter paths) location formats
 *   <li>Includes similarity scores for ranking
 *   <li>100% traceable to source documents
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-26
 */
public class Citation {

  @JsonProperty("resourceId")
  private final String resourceId;

  @JsonProperty("resourceTitle")
  private final String resourceTitle;

  @JsonProperty("chunkId")
  private final String chunkId;

  @JsonProperty("chunkIndex")
  private final int chunkIndex;

  @JsonProperty("location")
  private final CitationLocation location;

  @JsonProperty("snippet")
  private final String snippet;

  @JsonProperty("score")
  private final double score;

  /**
   * Creates a complete citation.
   *
   * @param resourceId the resource ID (UUID)
   * @param resourceTitle the resource title
   * @param chunkId the chunk ID (UUID)
   * @param chunkIndex the chunk index (0-based position in document)
   * @param location the location information (page range or chapter path)
   * @param snippet the text excerpt supporting the answer (100-200 characters)
   * @param score the similarity score (0.0 to 1.0)
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
   * Gets the resource ID.
   *
   * @return the resource UUID
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Gets the resource title.
   *
   * @return the resource title
   */
  public String getResourceTitle() {
    return resourceTitle;
  }

  /**
   * Gets the chunk ID.
   *
   * @return the chunk UUID
   */
  public String getChunkId() {
    return chunkId;
  }

  /**
   * Gets the chunk index.
   *
   * @return the chunk index (0-based)
   */
  public int getChunkIndex() {
    return chunkIndex;
  }

  /**
   * Gets the location information.
   *
   * @return the location (PDF page range or Markdown chapter path)
   */
  public CitationLocation getLocation() {
    return location;
  }

  /**
   * Gets the text snippet.
   *
   * @return the supporting text excerpt
   */
  public String getSnippet() {
    return snippet;
  }

  /**
   * Gets the similarity score.
   *
   * @return the score (0.0 to 1.0)
   */
  public double getScore() {
    return score;
  }

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

  @Override
  public int hashCode() {
    return Objects.hash(resourceId, chunkId, chunkIndex, score);
  }

  @Override
  public String toString() {
    return "Citation{"
        + "resourceId='"
        + resourceId
        + '\''
        + ", resourceTitle='"
        + resourceTitle
        + '\''
        + ", chunkId='"
        + chunkId
        + '\''
        + ", chunkIndex="
        + chunkIndex
        + ", location="
        + location
        + ", snippet='"
        + snippet
        + '\''
        + ", score="
        + score
        + '}';
  }
}
