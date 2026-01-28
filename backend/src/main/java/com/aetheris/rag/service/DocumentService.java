/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.util.FileUtil;
import java.util.List;

/**
 * 文档处理服务接口。
 *
 * <p>负责文档解析、切片生成、文件验证等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
public interface DocumentService {

  /**
   * 处理文档并生成切片。
   *
   * @param filePath 文件路径
   * @param fileType 文件类型（PDF/MARKDOWN）
   * @param resourceId 资源ID
   * @param chunkSize 切片大小
   * @param chunkOverlap 切片重叠
   * @return 切片列表
   * @throws java.io.IOException 如果文件处理失败
   */
  List<Chunk> processDocument(String filePath, String fileType, Long resourceId, int chunkSize,
      int chunkOverlap) throws java.io.IOException;

  /**
   * 验证文件格式。
   *
   * @param fileBytes 文件字节数组
   * @param fileName 文件名
   * @return 验证结果
   */
  FileUtil.ValidationResult validateFileFormat(byte[] fileBytes, String fileName);

  /**
   * 计算文件内容哈希。
   *
   * @param fileBytes 文件字节数组
   * @return SHA-256哈希值
   */
  String calculateContentHash(byte[] fileBytes);
}
