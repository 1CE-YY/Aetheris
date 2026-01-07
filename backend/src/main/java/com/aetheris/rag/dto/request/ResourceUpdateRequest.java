/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源更新请求。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateRequest {

  /** 资源标题 */
  @NotBlank(message = "标题不能为空")
  @Size(max = 200, message = "标题长度不能超过200个字符")
  private String title;

  /** 标签/课程方向（逗号分隔） */
  @Size(max = 500, message = "标签长度不能超过500个字符")
  private String tags;

  /** 资源描述 */
  @Size(max = 2000, message = "描述长度不能超过2000个字符")
  private String description;
}
