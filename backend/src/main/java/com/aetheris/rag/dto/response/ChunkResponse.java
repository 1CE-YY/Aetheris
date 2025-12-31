/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import com.aetheris.rag.entity.Chunk;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切片响应。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {

  /** 切片ID */
  private Long id;

  /** 资源ID */
  private Long resourceId;

  /** 切片序号 */
  private Integer chunkIndex;

  /** 切片文本 */
  private String chunkText;

  /** 定位信息 */
  private String locationInfo;

  /** PDF起始页码 */
  private Integer pageStart;

  /** PDF结束页码 */
  private Integer pageEnd;

  /** Markdown章节路径 */
  private String chapterPath;

  /** 是否已向量化 */
  private Boolean vectorized;

  /** 创建时间 */
  private Instant createdAt;

  /**
   * 从 Chunk 实体创建响应。
   *
   * @param chunk 切片实体
   * @return 切片响应
   */
  public static ChunkResponse fromEntity(Chunk chunk) {
    return ChunkResponse.builder()
        .id(chunk.getId())
        .resourceId(chunk.getResourceId())
        .chunkIndex(chunk.getChunkIndex())
        .chunkText(chunk.getChunkText())
        .locationInfo(chunk.getLocationInfo())
        .pageStart(chunk.getPageStart())
        .pageEnd(chunk.getPageEnd())
        .chapterPath(chunk.getChapterPath())
        .vectorized(chunk.getVectorized())
        .createdAt(chunk.getCreatedAt())
        .build();
  }
}
