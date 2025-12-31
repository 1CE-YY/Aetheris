/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import com.aetheris.rag.entity.Resource;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源响应。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

  /** 资源ID */
  private Long id;

  /** 资源标题 */
  private String title;

  /** 标签 */
  private String tags;

  /** 文件类型 */
  private String fileType;

  /** 文件大小（字节） */
  private Long fileSize;

  /** 资源描述 */
  private String description;

  /** 内容哈希 */
  private String contentHash;

  /** 上传者用户ID */
  private Long uploadedBy;

  /** 上传时间 */
  private Instant uploadTime;

  /** 切片数量 */
  private Integer chunkCount;

  /** 是否已向量化 */
  private Boolean vectorized;

  /**
   * 从 Resource 实体创建响应。
   *
   * @param resource 资源实体
   * @return 资源响应
   */
  public static ResourceResponse fromEntity(Resource resource) {
    return ResourceResponse.builder()
        .id(resource.getId())
        .title(resource.getTitle())
        .tags(resource.getTags())
        .fileType(resource.getFileType())
        .fileSize(resource.getFileSize())
        .description(resource.getDescription())
        .contentHash(resource.getContentHash())
        .uploadedBy(resource.getUploadedBy())
        .uploadTime(resource.getUploadTime())
        .chunkCount(resource.getChunkCount())
        .vectorized(resource.getVectorized())
        .build();
  }
}
