/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import java.util.List;

/**
 * 资源服务接口。
 *
 * <p>提供资源查询、更新等CRUD功能。
 * 文档处理和向量化功能已移至 ProcessingService。
 *
 * @author Aetheris Team
 * @version 2.0.0
 * @since 2025-12-30
 */
public interface ResourceService {

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

  /**
   * 更新资源信息。
   *
   * @param id 资源ID
   * @param title 标题
   * @param tags 标签
   * @param description 描述
   * @return 更新后的资源
   */
  Resource updateResource(Long id, String title, String tags, String description);

  /**
   * 更新资源的向量化状态。
   *
   * <p>供 VectorService 在向量化完成后回调使用。</p>
   *
   * @param resourceId 资源ID
   * @param vectorized 是否已向量化
   */
  void updateVectorizationStatus(Long resourceId, boolean vectorized);

  /**
   * 更新资源的切片数量和向量化状态。
   *
   * <p>供 VectorService 在向量化完成后回调使用。</p>
   *
   * @param resourceId 资源ID
   * @param chunkCount 切片数量
   * @param vectorized 是否已向量化
   */
  void updateChunkVectorizationStatus(Long resourceId, Integer chunkCount, boolean vectorized);
}
