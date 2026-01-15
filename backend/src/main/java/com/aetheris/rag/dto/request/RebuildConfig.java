/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重建任务配置。
 *
 * <p>用于配置重建任务的行为参数。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildConfig {

  /** 是否删除旧索引（默认 true） */
  @Builder.Default private boolean dropIndex = true;

  /** 是否重新切片（默认 true） */
  @Builder.Default private boolean rechunk = true;

  /** 批次大小（默认 50） */
  @Builder.Default private int batchSize = 50;

  /** 最大重试次数（默认 3） */
  @Builder.Default private int maxRetries = 3;

  /** 跳过错误继续执行（默认 true） */
  @Builder.Default private boolean skipOnError = true;

  /** 启用虚拟线程并发（默认 true） */
  @Builder.Default private boolean enableConcurrency = true;

  /** 是否跳过向量化（默认 false） */
  /**
   * 是否跳过向量化（用于完全重建流程）。
   *
   * <p>当设置为 true 时，资源重建过程中不会触发向量化。
   * 适用于：先重建所有资源（切片），然后统一批量向量化的场景。
   *
   * <p>默认为 false，以保持向后兼容性。
   */
  @Builder.Default private boolean skipVectorization = false;
}
