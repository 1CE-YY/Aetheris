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
 * 资源服务类
 */
export class ResourceService {
  /**
   * 上传资源
   */
  static async upload(data: ResourceUploadRequest): Promise<Resource> {
    return api.post<Resource, Resource>('/resources', data)
  }

  /**
   * 获取资源列表
   */
  static async getResourceList(offset = 0, limit = 10): Promise<Resource[]> {
    return api.get<Resource[]>(`/resources?offset=${offset}&limit=${limit}`)
  }

  /**
   * 获取资源详情
   */
  static async getResourceById(id: number): Promise<Resource> {
    return api.get<Resource>(`/resources/${id}`)
  }

  /**
   * 获取资源切片列表
   */
  static async getChunksByResourceId(id: number): Promise<Chunk[]> {
    return api.get<Chunk[]>(`/resources/${id}/chunks`)
  }
}

export default ResourceService
