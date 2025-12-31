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
 * 资源上传请求。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadRequest {

  /** 文件路径（相对路径或绝对路径） */
  @NotBlank(message = "文件路径不能为空")
  private String filePath;

  /** 资源标题 */
  @NotBlank(message = "标题不能为空")
  @Size(max = 200, message = "标题长度不能超过200字符")
  private String title;

  /** 标签（逗号分隔） */
  @Size(max = 500, message = "标签长度不能超过500字符")
  private String tags;

  /** 资源描述 */
  private String description;
}
