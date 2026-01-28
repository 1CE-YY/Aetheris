/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件工具类。
 *
 * <p>统一的文件操作和验证工具，合并了 FileOperationUtil 和 FileValidationUtil。
 *
 * <p>功能包括：
 * <ul>
 *   <li>文件操作：保存、删除、类型获取
 *   <li>文件验证：格式验证、魔术字节检测
 * </ul>
 *
 * @author Aetheris Team
 * @version 2.0.0
 * @since 2025-01-15
 */
@Slf4j
public final class FileUtil {

  private FileUtil() {
  }

  // ==================== 文件操作方法 ====================

  /**
   * 保存上传的文件到指定目录。
   *
   * @param file 上传的文件
   * @param uploadDir 上传目录
   * @return 保存后的文件路径
   * @throws IOException 如果文件保存失败
   */
  public static Path saveUploadedFile(MultipartFile file, String uploadDir) throws IOException {
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }

    String originalFilename = file.getOriginalFilename();
    // 清理文件名，移除路径遍历字符和特殊字符
    String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    String fileName = System.currentTimeMillis() + "_" + safeFilename;
    Path targetPath = uploadPath.resolve(fileName).normalize();

    // 安全检查：确保解析后的路径仍在uploadDir内
    if (!targetPath.startsWith(uploadPath)) {
      throw new IOException("非法的文件路径: " + originalFilename);
    }

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    log.debug("文件已保存: {}", targetPath);
    return targetPath;
  }

  /**
   * 删除物理文件。
   *
   * @param filePath 文件路径
   */
  public static void deletePhysicalFile(String filePath) {
    try {
      Path path = Paths.get(filePath);
      if (Files.exists(path)) {
        Files.delete(path);
        log.debug("物理文件已删除: {}", filePath);
      }
    } catch (IOException e) {
      log.warn("删除物理文件失败: {}", filePath, e);
    }
  }

  /**
   * 根据文件名获取文件类型。
   *
   * @param fileName 文件名
   * @return 文件类型（PDF/MARKDOWN）
   * @throws IllegalArgumentException 不支持的文件类型
   */
  public static String getFileType(String fileName) {
    String lowerName = fileName.toLowerCase();
    if (lowerName.endsWith(".pdf")) {
      return "PDF";
    } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
      return "MARKDOWN";
    } else {
      throw new IllegalArgumentException("不支持的文件类型: " + fileName);
    }
  }

  /**
   * 验证上传的文件。
   *
   * @param file 上传的文件
   * @throws IllegalArgumentException 文件验证失败
   */
  public static void validateFile(MultipartFile file) {
    if (file.isEmpty() || file.getSize() == 0) {
      throw new IllegalArgumentException("文件为空，请上传非空文件");
    }
    if (file.getSize() > 50 * 1024 * 1024) {
      throw new IllegalArgumentException("文件大小超过 50MB 限制");
    }
    String fileName = file.getOriginalFilename();
    if (fileName == null ||
        (!fileName.endsWith(".md") && !fileName.endsWith(".markdown") && !fileName.endsWith(
            ".pdf"))) {
      throw new IllegalArgumentException("不支持的文件类型，仅支持 Markdown 和 PDF");
    }
  }

  // ==================== 文件验证方法 ====================

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

  // ==================== 内部类 ====================

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
