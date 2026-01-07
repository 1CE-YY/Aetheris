/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.entity.Chunk;
import com.aetheris.rag.entity.Resource;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

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
   * @param file 上传的文件
   * @param title 资源标题
   * @param tags 标签（逗号分隔）
   * @param description 描述
   * @param uploadedBy 上传者用户ID
   * @return 资源实体（如果已存在则返回现有资源）
   * @throws Exception 如果文件处理失败
   */
  Resource uploadResource(
      MultipartFile file, String title, String tags, String description, Long uploadedBy)
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
   * 删除资源（级联删除切片）。
   *
   * @param id 资源ID
   * @param userId 操作用户ID
   * @return 被删除的资源
   * @throws RuntimeException 如果资源不存在或无权删除
   */
  Resource deleteResource(Long id, Long userId);

  /**
   * 批量删除资源。
   *
   * @param ids 资源ID列表
   * @param userId 操作用户ID
   * @return 删除的资源列表
   */
  List<Resource> deleteResources(List<Long> ids, Long userId);
}
