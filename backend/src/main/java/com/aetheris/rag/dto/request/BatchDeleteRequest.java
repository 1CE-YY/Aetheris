/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量删除资源请求。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteRequest {

  /** 资源ID列表 */
  @NotEmpty(message = "删除列表不能为空")
  private List<Long> ids;
}
