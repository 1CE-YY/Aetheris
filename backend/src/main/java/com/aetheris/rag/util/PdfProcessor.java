/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Chunk;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * PDF 文档处理器。
 *
 * <p>使用 Apache PDFBox 提取 PDF 文本内容，并记录每个切片的页码范围。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Slf4j
@Component
public class PdfProcessor {

  /** 默认切片大小（字符数） */
  private static final int DEFAULT_CHUNK_SIZE = 1000;

  /** 默认切片重叠大小（字符数） */
  private static final int DEFAULT_CHUNK_OVERLAP = 200;

  /**
   * 处理 PDF 文件，生成文本切片。
   *
   * @param filePath PDF 文件路径
   * @param chunkSize 切片大小（字符数）
   * @param chunkOverlap 切片重叠大小（字符数）
   * @return 切片列表
   * @throws IOException 如果文件读取失败
   */
  public List<Chunk> process(String filePath, int chunkSize, int chunkOverlap)
      throws IOException {
    log.info("开始处理 PDF 文件: {}", filePath);

    List<Chunk> chunks = new ArrayList<>();

    try (PDDocument document = Loader.loadPDF(new File(filePath))) {
      int totalPages = document.getNumberOfPages();
      log.debug("PDF 总页数: {}", totalPages);

      // 逐页提取文本
      List<PageText> pageTexts = extractPageTexts(document);

      // 按页码范围合并文本并切片
      int chunkIndex = 0;
      StringBuilder currentText = new StringBuilder();
      int currentPageStart = 0;
      int currentPageEnd = 0;

      for (int i = 0; i < pageTexts.size(); i++) {
        PageText pageText = pageTexts.get(i);
        String text = pageText.text();

        if (currentText.length() + text.length() > chunkSize && currentText.length() > 0) {
          // 创建切片
          Chunk chunk =
              Chunk.builder()
                  .chunkIndex(chunkIndex++)
                  .chunkText(currentText.toString())
                  .locationInfo(String.format("第%d-%d页", currentPageStart, currentPageEnd))
                  .pageStart(currentPageStart)
                  .pageEnd(currentPageEnd)
                  .build();

          chunks.add(chunk);
          log.debug("创建切片 {}: 页 {}-{}, 长度 {}", chunkIndex - 1, currentPageStart, currentPageEnd, currentText.length());

          // 处理重叠
          String overlapText = getOverlapText(currentText.toString(), chunkOverlap);
          currentText = new StringBuilder(overlapText);
          currentPageStart = pageText.pageNumber();
        }

        currentText.append(text);
        currentPageEnd = pageText.pageNumber();
      }

      // 处理最后一个切片
      if (currentText.length() > 0) {
        Chunk chunk =
            Chunk.builder()
                .chunkIndex(chunkIndex++)
                .chunkText(currentText.toString())
                .locationInfo(String.format("第%d-%d页", currentPageStart, currentPageEnd))
                .pageStart(currentPageStart)
                .pageEnd(currentPageEnd)
                .build();

        chunks.add(chunk);
        log.debug("创建切片 {}: 页 {}-{}, 长度 {}", chunkIndex - 1, currentPageStart, currentPageEnd, currentText.length());
      }

      log.info("PDF 处理完成，共生成 {} 个切片", chunks.size());
    }

    return chunks;
  }

  /**
   * 使用默认参数处理 PDF 文件。
   *
   * @param filePath PDF 文件路径
   * @return 切片列表
   * @throws IOException 如果文件读取失败
   */
  public List<Chunk> process(String filePath) throws IOException {
    return process(filePath, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
  }

  /**
   * 提取每页的文本内容。
   *
   * @param document PDF 文档
   * @return 页文本列表
   * @throws IOException 如果提取失败
   */
  private List<PageText> extractPageTexts(PDDocument document) throws IOException {
    List<PageText> pageTexts = new ArrayList<>();
    PDFTextStripper textStripper = new PDFTextStripper();

    // 按页提取文本
    for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
      textStripper.setStartPage(pageNum + 1);
      textStripper.setEndPage(pageNum + 1);

      String text = textStripper.getText(document).trim();
      if (!text.isEmpty()) {
        pageTexts.add(new PageText(pageNum, text));
      }
    }

    return pageTexts;
  }

  /**
   * 获取文本的重叠部分。
   *
   * @param text 原文本
   * @param overlapSize 重叠大小
   * @return 重叠文本
   */
  private String getOverlapText(String text, int overlapSize) {
    if (text.length() <= overlapSize) {
      return text;
    }
    return text.substring(text.length() - overlapSize);
  }

  /** 页文本记录（内部类）。 */
  private record PageText(int pageNumber, String text) {}
}
