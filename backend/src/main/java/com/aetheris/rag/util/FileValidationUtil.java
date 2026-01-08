/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件验证工具类。
 *
 * <p>提供文件格式验证、魔术字节检测等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-08
 */
@Slf4j
public class FileValidationUtil {

  /**
   * 文件验证结果。
   */
  @Data
  @AllArgsConstructor
  public static class ValidationResult {
    private boolean valid;
    private String errorMessage;
  }

  /**
   * 验证文件格式是否与扩展名匹配。
   *
   * @param fileBytes 文件字节数组
   * @param fileName 文件名
   * @return 验证结果
   */
  public static ValidationResult validateFileFormat(byte[] fileBytes, String fileName) {
    if (fileBytes == null || fileBytes.length < 4) {
      return new ValidationResult(false, "文件内容为空或无法读取");
    }

    String lowerFileName = fileName.toLowerCase();
    FileType detectedType = detectFileType(fileBytes);

    // PDF 文件验证
    if (lowerFileName.endsWith(".pdf")) {
      if (detectedType != FileType.PDF) {
        return new ValidationResult(false,
            String.format("文件扩展名为 .pdf 但实际格式为 %s，请上传正确的 PDF 文件",
                detectedType.displayName));
      }
    }

    // Markdown 文件不需要魔术字节验证（没有固定格式）

    return new ValidationResult(true, null);
  }

  /**
   * 检测文件类型（基于魔术字节）。
   *
   * @param fileBytes 文件字节数组
   * @return 文件类型
   */
  private static FileType detectFileType(byte[] fileBytes) {
    // 读取前 8 字节用于判断
    if (fileBytes.length < 8) {
      return FileType.UNKNOWN;
    }

    // PDF: %PDF- (0x25 50 44 46 2D)
    if (fileBytes[0] == 0x25 && fileBytes[1] == 0x50
        && fileBytes[2] == 0x44 && fileBytes[3] == 0x46
        && fileBytes[4] == 0x2D) {
      return FileType.PDF;
    }

    // ZIP 格式（ODT、DOCX 等）: PK (0x50 0x4B 0x03 0x04)
    if (fileBytes[0] == 0x50 && fileBytes[1] == 0x4B
        && fileBytes[2] == 0x03 && fileBytes[3] == 0x04) {
      // 进一步判断是否为 ODT
      if (fileBytes.length > 30 && new String(fileBytes, 0, 30).contains("mimetype")) {
        return FileType.ODT;
      }
      return FileType.ZIP;
    }

    return FileType.UNKNOWN;
  }

  /**
   * 文件类型枚举。
   */
  @AllArgsConstructor
  private enum FileType {
    PDF("PDF 文档"),
    ODT("ODT 文档"),
    ZIP("ZIP 压缩包"),
    MARKDOWN("Markdown 文档"),
    UNKNOWN("未知格式");

    private final String displayName;
  }
}
