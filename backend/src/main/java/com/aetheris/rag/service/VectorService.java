/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

/**
 * 向量化服务接口。
 *
 * <p>负责将资源切片转换为向量并存储到 Redis 向量索引中。
 *
 * <p><strong>注意：</strong>当前为 stub 实现，返回 dummy vectors。
 * 完整的向量化功能将在 Phase 5 实现 EmbeddingGateway 后自动生效。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-31
 */
public interface VectorService {

  /**
   * 初始化 Redis 向量索引。
   *
   * <p>创建 RediSearch HNSW 索引，用于向量相似度检索。
   */
  void initializeVectorIndex();

  /**
   * 向量化指定资源的所有切片。
   *
   * @param resourceId 资源ID
   */
  void vectorizeChunks(Long resourceId);

  /**
   * 批量向量化所有未向量化的切片。
   */
  void vectorizeAllUnvectorized();
}
