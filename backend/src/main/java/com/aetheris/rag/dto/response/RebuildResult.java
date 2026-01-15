/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全量重建结果。
 *
 * <p>返回全量重建的统计信息，包括成功数、失败数、总切片数等。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildResult {

  /** 成功处理的资源数量 */
  private int successCount;

  /** 失败的资源数量 */
  private int failureCount;

  /** 总切片数量 */
  private int totalChunks;

  /** 总耗时（毫秒） */
  private long duration;

  /** 失败的资源ID列表 */
  private List<Long> failedResourceIds;
}
