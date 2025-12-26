package com.aetheris.rag.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CitationLocation}.
 *
 * @author Aetheris Team
 * @version 1.0.0
 */
@DisplayName("CitationLocation Tests")
class CitationLocationTest {

  @Test
  @DisplayName("PdfLocation should create valid page range")
  void testPdfLocationValid() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(1, 5);

    assertEquals(1, location.getPageStart());
    assertEquals(5, location.getPageEnd());
    assertEquals("第 1-5 页", location.getDisplayString());
  }

  @Test
  @DisplayName("PdfLocation should handle single page")
  void testPdfLocationSinglePage() {
    CitationLocation.PdfLocation location = new CitationLocation.PdfLocation(3, 3);

    assertEquals(3, location.getPageStart());
    assertEquals(3, location.getPageEnd());
    assertEquals("第 3 页", location.getDisplayString());
  }

  @Test
  @DisplayName("PdfLocation should throw exception for pageStart < 1")
  void testPdfLocationInvalidStart() {
    assertThrows(IllegalArgumentException.class, () -> new CitationLocation.PdfLocation(0, 5));
  }

  @Test
  @DisplayName("PdfLocation should throw exception for pageEnd < pageStart")
  void testPdfLocationInvalidEnd() {
    assertThrows(
        IllegalArgumentException.class, () -> new CitationLocation.PdfLocation(5, 3));
  }

  @Test
  @DisplayName("PdfLocation equals should work correctly")
  void testPdfLocationEquals() {
    CitationLocation.PdfLocation location1 = new CitationLocation.PdfLocation(1, 5);
    CitationLocation.PdfLocation location2 = new CitationLocation.PdfLocation(1, 5);
    CitationLocation.PdfLocation location3 = new CitationLocation.PdfLocation(2, 5);

    assertEquals(location1, location2);
    assertNotEquals(location1, location3);
  }

  @Test
  @DisplayName("MarkdownLocation should create valid chapter path")
  void testMarkdownLocationValid() {
    CitationLocation.MarkdownLocation location =
        new CitationLocation.MarkdownLocation("Chapter 1 > 1.1 Introduction");

    assertEquals("Chapter 1 > 1.1 Introduction", location.getChapterPath());
    assertEquals("Chapter 1 > 1.1 Introduction", location.getDisplayString());
  }

  @Test
  @DisplayName("MarkdownLocation should throw exception for null chapter path")
  void testMarkdownLocationNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CitationLocation.MarkdownLocation(null));
  }

  @Test
  @DisplayName("MarkdownLocation should throw exception for empty chapter path")
  void testMarkdownLocationEmpty() {
    assertThrows(
        IllegalArgumentException.class, () -> new CitationLocation.MarkdownLocation(""));
  }

  @Test
  @DisplayName("MarkdownLocation equals should work correctly")
  void testMarkdownLocationEquals() {
    CitationLocation.MarkdownLocation location1 =
        new CitationLocation.MarkdownLocation("Chapter 1");
    CitationLocation.MarkdownLocation location2 =
        new CitationLocation.MarkdownLocation("Chapter 1");
    CitationLocation.MarkdownLocation location3 =
        new CitationLocation.MarkdownLocation("Chapter 2");

    assertEquals(location1, location2);
    assertNotEquals(location1, location3);
  }
}
