/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import api from './api'

/**
 * 资源上传请求
 */
export interface ResourceUploadRequest {
  filePath: string
  title: string
  tags?: string
  description?: string
}

/**
 * 资源信息
 */
export interface Resource {
  id: number
  title: string
  tags?: string
  fileType: string
  fileSize: number
  description?: string
  contentHash: string
  uploadedBy: number
  uploadTime: string
  chunkCount: number
  vectorized: boolean
}

/**
 * 切片信息
 */
export interface Chunk {
  id: number
  resourceId: number
  chunkIndex: number
  chunkText: string
  locationInfo?: string
  pageStart?: number
  pageEnd?: number
  chapterPath?: string
  vectorized: boolean
  createdAt: string
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
}

/**
 * 资源服务类
 */
export class ResourceService {
  /**
   * 上传资源
   */
  static async upload(data: ResourceUploadRequest): Promise<Resource> {
    const response = await api.post<any, Resource>('/resources', data)
    return response
  }

  /**
   * 获取资源列表（分页）
   */
  static async getResourceList(page = 0, size = 10): Promise<PageResponse<Resource>> {
    const response = await api.get<any, PageResponse<Resource>>('/resources', {
      params: { page, size }
    })
    return response
  }

  /**
   * 获取资源详情
   */
  static async getResourceById(id: number): Promise<Resource> {
    const response = await api.get<any, Resource>(`/resources/${id}`)
    return response
  }

  /**
   * 获取资源切片列表
   */
  static async getChunksByResourceId(id: number): Promise<Chunk[]> {
    const response = await api.get<any, Chunk[]>(`/resources/${id}/chunks`)
    return response
  }
}

export default ResourceService
