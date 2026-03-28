/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import api from './api'

/**
 * 推荐资源项接口
 */
export interface RecommendationItem {
  /** 资源ID */
  resourceId: string
  /** 资源标题 */
  title: string
  /** 标签（逗号分隔） */
  tags: string
  /** AI 生成的推荐理由 */
  reason: string
  /** AI 生成的学习建议 */
  suggestion: string
  /** 证据引用列表 */
  citations: Citation[]
  /** 相似度分数 */
  score: number
  /** 文件类型 */
  fileType: string
  /** 资源描述 */
  description: string
}

/**
 * 引用对象（复用 chat.service.ts 中的 Citation 结构）
 */
export interface Citation {
  resourceId: string
  resourceTitle: string
  chunkId: string
  chunkIndex: number
  location: CitationLocation
  snippet: string
  score: number
}

/**
 * 引用位置信息
 */
export type CitationLocation = PdfLocation | MarkdownLocation

/**
 * PDF 位置信息
 */
export interface PdfLocation {
  type: 'pdf'
  pageStart: number
  pageEnd: number
}

/**
 * Markdown 位置信息
 */
export interface MarkdownLocation {
  type: 'markdown'
  chapterPath: string
}

/**
 * 推荐响应接口
 */
export interface RecommendationResponse {
  /** 推荐资源项列表 */
  items: RecommendationItem[]
  /** 是否为个性化推荐 */
  personalized: boolean
  /** 推荐生成耗时（毫秒） */
  latencyMs: number
  /** 推荐结果数量 */
  count: number
}

/**
 * 推荐服务类
 *
 * <p>封装推荐相关的 API 调用，包括：
 * <ul>
 *   <li>获取推荐列表</li>
 *   <li>记录推荐点击行为</li>
 *   <li>记录推荐收藏行为</li>
 * </ul>
 */
export class RecommendationService {
  /**
   * 获取个性化推荐列表
   *
   * @param topN 推荐数量（默认 10）
   * @returns 推荐响应
   */
  static async getRecommendations(topN = 10): Promise<RecommendationResponse> {
    const response = await api.get<any, RecommendationResponse>('/recommendations', {
      params: { topN }
    })
    return response
  }

  /**
   * 记录推荐资源点击行为
   *
   * @param resourceId 资源ID
   */
  static async recordClick(resourceId: string): Promise<void> {
    await api.post('/recommendations/click', null, {
      params: { resourceId }
    })
  }

  /**
   * 记录推荐资源收藏行为
   *
   * @param resourceId 资源ID
   */
  static async recordFavorite(resourceId: string): Promise<void> {
    await api.post('/recommendations/favorite', null, {
      params: { resourceId }
    })
  }
}

export default RecommendationService
