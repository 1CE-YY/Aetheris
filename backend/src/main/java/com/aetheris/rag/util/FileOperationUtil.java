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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件操作工具类。
 *
 * <p>提供统一的文件操作方法，消除ServiceImpl层的重复代码。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
@Slf4j
public final class FileOperationUtil {

  private FileOperationUtil() {
  }

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
}
