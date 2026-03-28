package com.aetheris.rag.service;

import com.aetheris.rag.dto.response.Citation;
import java.util.List;

/**
 * 语义检索服务接口。
 *
 * <p>提供基于向量相似度的语义检索功能，支持：
 *
 * <ul>
 *   <li>Top-K 切片检索（Redis 向量搜索）
 *   <li>按资源聚合（同一资源的多个切片合并）
 *   <li>检索结果缓存
 * </ul>
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2026-01-06
 */
public interface SearchService {

  /**
   * 执行语义检索，返回 Top-K 相关切片。
   *
   * @param query 查询文本
   * @param topK 返回结果数量
   * @return 引用列表（包含 resourceId、chunkId、chunkIndex、location、snippet、score）
   */
  List<Citation> search(String query, int topK);

  /**
   * 执行语义检索，返回 Top-K 相关切片，按资源聚合。
   *
   * @param query 查询文本
   * @param topK 返回结果数量
   * @return 引用列表（同一资源的多个切片只保留相似度最高的）
   */
  List<Citation> searchAggregated(String query, int topK);

  /**
   * 使用预计算的向量执行语义检索，返回 Top-K 相关切片。
   *
   * <p>跳过文本向量化步骤，直接使用传入的向量进行检索。
   * 用于推荐服务直接传入用户画像向量进行检索，避免重复调用 Embedding API。
   *
   * @param vector 预计算的查询向量
   * @param topK 返回结果数量
   * @return 引用列表
   */
  List<Citation> searchByVector(float[] vector, int topK);

  /**
   * 使用预计算的向量执行语义检索，按资源聚合。
   *
   * <p>与 {@link #searchByVector(float[], int)} 的区别是结果按资源聚合，
   * 同一资源只保留相似度最高的切片。
   *
   * @param vector 预计算的查询向量
   * @param topK 返回结果数量
   * @return 聚合后的引用列表
   */
  List<Citation> searchByVectorAggregated(float[] vector, int topK);
}
