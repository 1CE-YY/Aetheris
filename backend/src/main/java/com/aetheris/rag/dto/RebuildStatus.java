/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto;

/**
 * 重建任务状态枚举。
 *
 * <p>用于跟踪重建任务的执行状态。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-14
 */
public enum RebuildStatus {

  /** 准备就绪 */
  READY,

  /** 执行中 */
  RUNNING,

  /** 已完成 */
  COMPLETED,

  /** 已失败 */
  FAILED,

  /** 已取消 */
  CANCELLED
}
