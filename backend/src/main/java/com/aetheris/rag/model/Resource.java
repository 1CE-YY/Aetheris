/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习资源实体类。
 *
 * <p>表示用户上传的学习资源（PDF/Markdown），包含元数据、文件信息和向量化状态。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

  /** 资源ID */
  private Long id;

  /** 资源标题 */
  private String title;

  /** 标签/课程方向（逗号分隔，如：机器学习,深度学习） */
  private String tags;

  /** 文件类型（PDF/MARKDOWN） */
  private String fileType;

  /** 文件存储路径 */
  private String filePath;

  /** 文件大小（字节） */
  private Long fileSize;

  /** 资源描述 */
  private String description;

  /**
   * 内容哈希（SHA-256，用于去重）。
   *
   * <p>相同内容哈希的文档被视为重复，不会重复入库。
   */
  private String contentHash;

  /** 上传者用户ID */
  private Long uploadedBy;

  /** 上传时间 */
  private Instant uploadTime;

  /** 切片数量 */
  private Integer chunkCount;

  /** 是否已向量化 */
  private Boolean vectorized;
}
