/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.util;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.mapper.ResourceMapper;
import java.util.List;

/**
 * 向量化状态工具类。
 * <p>统一处理向量化状态的计算和更新逻辑。
 */
public final class VectorizationStatusUtil {

  private VectorizationStatusUtil() {
  }

  /**
   * 计算资源的向量化状态。
   * <p>只有存在切片且所有切片都已向量化时，才返回true。
   */
  public static boolean calculateVectorizationStatus(List<Chunk> allChunks) {
    return !allChunks.isEmpty() && allChunks.stream().allMatch(Chunk::getVectorized);
  }

  /**
   * 更新资源的向量化状态到数据库。
   */
  public static void updateResourceVectorizationStatus(
      ResourceMapper resourceMapper,
      Long resourceId,
      List<Chunk> allChunks) {

    if (allChunks.isEmpty()) {
      return;
    }

    boolean allVectorized = calculateVectorizationStatus(allChunks);
    resourceMapper.updateChunkStatus(resourceId, allChunks.size(), allVectorized);
  }

  /**
   * 修复资源的向量化状态。
   * <p>重新计算切片的向量化状态并更新到数据库。
   */
  public static boolean repairVectorizationStatus(
      ResourceMapper resourceMapper,
      Long resourceId,
      List<Chunk> allChunks) {

    if (allChunks.isEmpty()) {
      return false;
    }

    boolean allVectorized = calculateVectorizationStatus(allChunks);
    resourceMapper.updateChunkStatus(resourceId, allChunks.size(), allVectorized);
    return true;
  }
}
