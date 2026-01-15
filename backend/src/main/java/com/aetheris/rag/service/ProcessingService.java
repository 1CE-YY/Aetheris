/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.dto.request.RebuildConfig;
import com.aetheris.rag.dto.response.RebuildResult;
import com.aetheris.rag.entity.Resource;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源处理协调服务接口。
 *
 * <p>负责协调资源处理过程中的多个Service调用，包括文件验证、文档解析、切片生成、向量化等复合操作。
 *
 * <p>此服务作为协调层，消除了ResourceService和VectorService之间的循环依赖。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-01-15
 */
public interface ProcessingService {

  /**
   * 处理资源上传的完整流程。
   *
   * <p>协调以下步骤：
   * <ol>
   *   <li>文件验证（大小、格式、内容验证）</li>
   *   <li>计算内容哈希</li>
   *   <li>去重检查（基于内容哈希）</li>
   *   <li>保存文件到服务器</li>
   *   <li>创建资源记录</li>
   *   <li>文档解析和切片生成</li>
   *   <li>插入切片记录到数据库</li>
   *   <li>触发向量化操作</li>
   * </ol>
   *
   * @param file 上传的文件
   * @param title 资源标题
   * @param tags 标签（可选）
   * @param description 描述（可选）
   * @param uploadedBy 上传者用户ID
   * @return 上传的资源实体
   * @throws Exception 如果处理失败
   */
  Resource processResourceUpload(MultipartFile file, String title, String tags,
      String description, Long uploadedBy) throws Exception;

  /**
   * 处理资源删除的完整流程。
   *
   * <p>协调以下步骤：
   * <ol>
   *   <li>权限检查</li>
   *   <li>删除向量数据（从Redis）</li>
   *   <li>删除切片记录（从数据库）</li>
   *   <li>删除资源记录（从数据库）</li>
   *   <li>删除物理文件</li>
   * </ol>
   *
   * @param id 资源ID
   * @param userId 操作用户ID
   * @return 被删除的资源实体
   */
  Resource processResourceDeletion(Long id, Long userId);

  /**
   * 处理批量资源删除的完整流程。
   *
   * @param ids 资源ID列表
   * @param userId 操作用户ID
   * @return 被删除的资源实体列表
   */
  List<Resource> processBatchResourceDeletion(List<Long> ids, Long userId);

  /**
   * 重新处理单个资源。
   *
   * <p>协调以下步骤：
   * <ol>
   *   <li>删除旧的切片和向量数据</li>
   *   <li>重新解析文档</li>
   *   <li>生成新切片</li>
   *   <li>插入新切片记录</li>
   *   <li>触发向量化</li>
   * </ol>
   *
   * @param resourceId 资源ID
   * @return 生成的切片数量
   * @throws Exception 如果处理失败
   */
  int reprocessResource(Long resourceId) throws Exception;

  /**
   * 完全重建所有资源（完整流程）。
   *
   * <p>协调以下步骤：
   * <ol>
   *   <li>删除所有向量数据和索引</li>
   *   <li>重新解析所有文档</li>
   *   <li>批量向量化</li>
   * </ol>
   *
   * @param config 重建配置
   * @return 重建结果
   * @throws Exception 如果重建失败
   */
  RebuildResult fullRebuild(RebuildConfig config) throws Exception;

  /**
   * 取消正在执行的重建任务。
   *
   * <p>委托给VectorService执行。
   *
   * @return 是否成功取消
   */
  boolean cancelRebuild();

  /**
   * 检查是否有正在执行的重建任务。
   *
   * @return 是否正在执行
   */
  boolean isRebuilding();
}
