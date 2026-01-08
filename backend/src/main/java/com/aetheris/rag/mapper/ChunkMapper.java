/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.mapper;

import com.aetheris.rag.entity.Chunk;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资源切片数据访问接口。
 *
 * <p>提供资源切片的 CRUD 操作，包括批量插入、查询、更新向量化状态等。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Mapper
public interface ChunkMapper {

  /**
   * 插入单个切片。
   *
   * @param chunk 切片实体
   * @return 影响行数
   */
  int insert(Chunk chunk);

  /**
   * 批量插入切片。
   *
   * @param chunks 切片列表
   * @return 影响行数
   */
  int batchInsert(@Param("chunks") List<Chunk> chunks);

  /**
   * 根据切片ID查询切片。
   *
   * @param id 切片ID
   * @return 切片实体，不存在则返回 null
   */
  Chunk findById(@Param("id") Long id);

  /**
   * 根据资源ID查询所有切片。
   *
   * @param resourceId 资源ID
   * @return 切片列表
   */
  List<Chunk> findByResourceId(@Param("resourceId") Long resourceId);

  /**
   * 根据文本哈希查询切片。
   *
   * @param textHash 文本哈希（SHA-256）
   * @return 切片实体，不存在则返回 null
   */
  Chunk findByTextHash(@Param("textHash") String textHash);

  /**
   * 查询所有未向量化的切片。
   *
   * @return 切片列表
   */
  List<Chunk> findUnvectorized();

  /**
   * 根据资源ID查询未向量化的切片。
   *
   * @param resourceId 资源ID
   * @return 切片列表
   */
  List<Chunk> findUnvectorizedByResourceId(@Param("resourceId") Long resourceId);

  /**
   * 更新切片向量化状态。
   *
   * @param id 切片ID
   * @param vectorized 是否已向量化
   * @return 影响行数
   */
  int updateVectorized(@Param("id") Long id, @Param("vectorized") Boolean vectorized);

  /**
   * 批量更新切片向量化状态。
   *
   * @param ids 切片ID列表
   * @param vectorized 是否已向量化
   * @return 影响行数
   */
  int batchUpdateVectorized(
      @Param("ids") List<Long> ids, @Param("vectorized") Boolean vectorized);

  /**
   * 重置所有切片的向量化状态为未向量化。
   *
   * @return 影响行数
   */
  int resetAllVectorized();

  /**
   * 根据资源ID删除所有切片。
   *
   * @param resourceId 资源ID
   * @return 影响行数
   */
  int deleteByResourceId(@Param("resourceId") Long resourceId);

  /**
   * 统计资源的切片数量。
   *
   * @param resourceId 资源ID
   * @return 切片数量
   */
  int countByResourceId(@Param("resourceId") Long resourceId);

  /**
   * 批量查询切片。
   *
   * @param resourceIds 资源ID列表
   * @return 切片列表
   */
  List<Chunk> findByResourceIds(@Param("resourceIds") List<Long> resourceIds);
}
