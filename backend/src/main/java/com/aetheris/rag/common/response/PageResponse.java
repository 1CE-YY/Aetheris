/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应包装类。
 *
 * <p>用于包装列表接口的分页数据。
 *
 * @param <T> 列表项类型
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

  /** 数据列表 */
  private List<T> items;

  /** 总条数 */
  private Long total;

  /** 当前页码 */
  private Integer page;

  /** 每页大小 */
  private Integer size;

  /** 总页数 */
  private Integer totalPages;

  /** 是否有下一页 */
  private Boolean hasNext;

  /**
   * 构建分页响应。
   *
   * @param items 数据列表
   * @param total 总条数
   * @param page 当前页码（从 0 开始）
   * @param size 每页大小
   * @param <T> 列表项类型
   * @return 分页响应
   */
  public static <T> PageResponse<T> of(List<T> items, Long total, Integer page, Integer size) {
    Integer totalPages = (int) Math.ceil((double) total / size);
    return PageResponse.<T>builder()
        .items(items)
        .total(total)
        .page(page)
        .size(size)
        .totalPages(totalPages)
        .hasNext(page < totalPages - 1)
        .build();
  }
}
