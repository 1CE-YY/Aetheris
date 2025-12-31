/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.mapper;

import com.aetheris.rag.entity.Resource;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资源数据访问接口。
 *
 * <p>提供学习资源的 CRUD 操作，包括上传、查询、更新向量化状态等。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@Mapper
public interface ResourceMapper {

  /**
   * 插入新资源。
   *
   * @param resource 资源实体
   * @return 影响行数
   */
  int insert(Resource resource);

  /**
   * 根据ID查询资源。
   *
   * @param id 资源ID
   * @return 资源实体，不存在则返回 null
   */
  Resource findById(@Param("id") Long id);

  /**
   * 根据内容哈希查询资源。
   *
   * @param contentHash 内容哈希（SHA-256）
   * @return 资源实体，不存在则返回 null
   */
  Resource findByContentHash(@Param("contentHash") String contentHash);

  /**
   * 根据上传者查询资源列表。
   *
   * @param uploadedBy 上传者用户ID
   * @return 资源列表
   */
  List<Resource> findByUploader(@Param("uploadedBy") Long uploadedBy);

  /**
   * 分页查询资源列表。
   *
   * @param offset 偏移量
   * @param limit 限制数量
   * @return 资源列表
   */
  List<Resource> findPaged(@Param("offset") int offset, @Param("limit") int limit);

  /**
   * 更新资源切片数量和向量化状态。
   *
   * @param id 资源ID
   * @param chunkCount 切片数量
   * @param vectorized 是否已向量化
   * @return 影响行数
   */
  int updateChunkStatus(
      @Param("id") Long id,
      @Param("chunkCount") Integer chunkCount,
      @Param("vectorized") Boolean vectorized);

  /**
   * 统计资源总数。
   *
   * @return 资源总数
   */
  int count();
}
