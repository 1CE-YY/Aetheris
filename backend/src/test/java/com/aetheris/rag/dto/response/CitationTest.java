package com.aetheris.rag.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Citation}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("Citation Tests")
class CitationTest {

  @Test
  @DisplayName("Constructor should create valid citation with all fields")
  void testConstructorValid() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);
    Citation citation =
        new Citation(
            "resource-123",
            "Test Resource",
            "chunk-456",
            0,
            location,
            "This is a test snippet",
            0.95);

    assertEquals("resource-123", citation.getResourceId());
    assertEquals("Test Resource", citation.getResourceTitle());
    assertEquals("chunk-456", citation.getChunkId());
    assertEquals(0, citation.getChunkIndex());
    assertEquals(location, citation.getLocation());
    assertEquals("This is a test snippet", citation.getSnippet());
    assertEquals(0.95, citation.getScore());
  }

  @Test
  @DisplayName("Constructor should throw exception for null resourceId")
  void testConstructorNullResourceId() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Citation(null, "Test", "chunk", 0, location, "snippet", 0.9));
  }

  @Test
  @DisplayName("Constructor should throw exception for negative chunkIndex")
  void testConstructorNegativeChunkIndex() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Citation("resource", "Test", "chunk", -1, location, "snippet", 0.9));
  }

  @Test
  @DisplayName("Constructor should throw exception for score outside [0,1]")
  void testConstructorInvalidScore() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Citation("resource", "Test", "chunk", 0, location, "snippet", 1.5));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Citation("resource", "Test", "chunk", 0, location, "snippet", -0.1));
  }

  @Test
  @DisplayName("equals should return true for identical citations")
  void testEqualsIdentical() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);
    Citation citation1 =
        new Citation("resource", "Test", "chunk", 0, location, "snippet", 0.9);
    Citation citation2 =
        new Citation("resource", "Test", "chunk", 0, location, "snippet", 0.9);

    assertEquals(citation1, citation2);
  }

  @Test
  @DisplayName("hashCode should be same for identical citations")
  void testHashCodeIdentical() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);
    Citation citation1 =
        new Citation("resource", "Test", "chunk", 0, location, "snippet", 0.9);
    Citation citation2 =
        new Citation("resource", "Test", "chunk", 0, location, "snippet", 0.9);

    assertEquals(citation1.hashCode(), citation2.hashCode());
  }

  @Test
  @DisplayName("toString should contain all fields")
  void testToString() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 3);
    Citation citation =
        new Citation("resource-123", "Test", "chunk-456", 0, location, "snippet", 0.9);

    String str = citation.toString();

    assertTrue(str.contains("resource-123"));
    assertTrue(str.contains("Test"));
    assertTrue(str.contains("chunk-456"));
    assertTrue(str.contains("0.9"));
  }
}
