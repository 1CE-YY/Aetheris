/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.model.Chunk;
import com.aetheris.rag.model.Resource;
import java.nio.file.Path;
import java.util.List;

/**
 * 资源服务接口。
 *
 * <p>提供资源上传、查询、切片、向量化等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public interface ResourceService {

  /**
   * 上传资源。
   *
   * <p>支持 PDF 和 Markdown 文件，自动提取文本、切片、向量化入库。
   * 使用内容哈希去重，相同内容不会重复入库（幂等性）。
   *
   * @param filePath 文件路径
   * @param title 资源标题
   * @param tags 标签（逗号分隔）
   * @param description 描述
   * @param uploadedBy 上传者用户ID
   * @return 资源实体（如果已存在则返回现有资源）
   * @throws Exception 如果文件处理失败
   */
  Resource uploadResource(
      Path filePath, String title, String tags, String description, Long uploadedBy)
      throws Exception;

  /**
   * 根据ID查询资源。
   *
   * @param id 资源ID
   * @return 资源实体，不存在则返回 null
   */
  Resource getResourceById(Long id);

  /**
   * 查询资源列表（分页）。
   *
   * @param offset 偏移量
   * @param limit 限制数量
   * @return 资源列表
   */
  List<Resource> getResourceList(int offset, int limit);

  /**
   * 查询资源的所有切片。
   *
   * @param resourceId 资源ID
   * @return 切片列表
   */
  List<Chunk> getChunksByResourceId(Long resourceId);

  /**
   * 检查资源是否存在（根据内容哈希）。
   *
   * @param contentHash 内容哈希（SHA-256）
   * @return 资源实体，不存在则返回 null
   */
  Resource findByContentHash(String contentHash);

  /**
   * 统计资源总数。
   *
   * @return 资源总数
   */
  Long getResourceCount();
}
