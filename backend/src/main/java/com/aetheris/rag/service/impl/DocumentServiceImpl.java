/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service.impl;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.service.DocumentService;
import com.aetheris.rag.util.FileUtil;
import com.aetheris.rag.util.HashUtil;
import com.aetheris.rag.util.MarkdownProcessor;
import com.aetheris.rag.util.PdfProcessor;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文档处理服务实现类。
 *
 * <p>负责文档解析、切片生成、文件验证等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

  private final PdfProcessor pdfProcessor;
  private final MarkdownProcessor markdownProcessor;

  @Override
  public List<Chunk> processDocument(String filePath, String fileType, Long resourceId,
      int chunkSize, int chunkOverlap) throws java.io.IOException {
    log.debug("开始处理文档: filePath={}, fileType={}, resourceId={}", filePath, fileType,
        resourceId);

    List<Chunk> chunks;

    // 根据文件类型调用相应的处理器
    if ("PDF".equalsIgnoreCase(fileType)) {
      chunks = pdfProcessor.process(filePath, chunkSize, chunkOverlap);
    } else if ("MARKDOWN".equalsIgnoreCase(fileType)) {
      chunks = markdownProcessor.process(filePath, chunkSize, chunkOverlap);
    } else {
      throw new IllegalArgumentException("不支持的文件类型: " + fileType);
    }

    // 设置切片元数据
    Instant now = Instant.now();
    for (Chunk chunk : chunks) {
      chunk.setResourceId(resourceId);
      chunk.setVectorized(false);
      chunk.setCreatedAt(now);

      // 计算文本哈希（用于去重和缓存）
      String textHash = HashUtil.sha256(chunk.getChunkText());
      chunk.setTextHash(textHash);
    }

    log.debug("文档处理完成: 共生成 {} 个切片", chunks.size());
    return chunks;
  }

  @Override
  public FileUtil.ValidationResult validateFileFormat(byte[] fileBytes,
      String fileName) {
    return FileUtil.validateFileFormat(fileBytes, fileName);
  }

  @Override
  public String calculateContentHash(byte[] fileBytes) {
    return HashUtil.sha256(fileBytes);
  }
}
