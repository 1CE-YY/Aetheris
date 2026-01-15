/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Chunk;
import java.time.Instant;
import java.util.List;

/**
 * 文档处理工具类。
 *
 * <p>提供统一的文档处理方法，消除代码重复。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
public final class DocumentProcessorUtil {

  private DocumentProcessorUtil() {
  }

  /**
   * 统一的文档处理入口。
   *
   * @param filePath 文件路径
   * @param fileType 文件类型（PDF/MARKDOWN）
   * @param resourceId 资源ID
   * @param chunkSize 切片大小
   * @param chunkOverlap 切片重叠
   * @param pdfProcessor PDF处理器
   * @param markdownProcessor Markdown处理器
   * @return 切片列表
   * @throws java.io.IOException 如果文件处理失败
   */
  public static List<Chunk> processDocument(
      String filePath,
      String fileType,
      Long resourceId,
      int chunkSize,
      int chunkOverlap,
      PdfProcessor pdfProcessor,
      MarkdownProcessor markdownProcessor) throws java.io.IOException {

    List<Chunk> chunks;

    if ("PDF".equalsIgnoreCase(fileType)) {
      chunks = pdfProcessor.process(filePath, chunkSize, chunkOverlap);
    } else if ("MARKDOWN".equalsIgnoreCase(fileType)) {
      chunks = markdownProcessor.process(filePath, chunkSize, chunkOverlap);
    } else {
      throw new IllegalArgumentException("不支持的文件类型: " + fileType);
    }

    // 设置切片元数据
    setChunkMetadata(chunks, resourceId);

    return chunks;
  }

  /**
   * 设置切片元数据。
   *
   * @param chunks 切片列表
   * @param resourceId 资源ID
   */
  public static void setChunkMetadata(List<Chunk> chunks, Long resourceId) {
    Instant now = Instant.now();
    for (Chunk chunk : chunks) {
      chunk.setResourceId(resourceId);
      chunk.setVectorized(false);
      chunk.setCreatedAt(now);

      // 计算文本哈希（用于去重和缓存）
      String textHash = HashUtil.sha256(chunk.getChunkText());
      chunk.setTextHash(textHash);
    }
  }
}
