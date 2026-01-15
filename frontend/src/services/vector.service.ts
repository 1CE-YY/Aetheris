import api from './api'

/**
 * 向量化服务
 *
 * <p>提供向量化和向量索引管理的API接口。
 */
export class VectorService {
  /**
   * 向量化指定资源的所有切片
   *
   * <p>仅进行向量化，不重新切片。如果资源没有切片，会返回错误。
   *
   * @param id 资源ID
   */
  static async vectorizeResource(id: number): Promise<void> {
    await api.post(`/vectors/resources/${id}/vectorize`)
  }

  /**
   * 重新处理指定资源（切片+向量化）
   *
   * <p>删除旧切片，重新解析文档，生成新切片，并向量化。
   *
   * @param id 资源ID
   */
  static async reprocessResource(id: number): Promise<void> {
    await api.post(`/vectors/resources/${id}/reprocess`)
  }

  /**
   * 批量向量化多个资源
   *
   * @param ids 资源ID列表
   */
  static async batchVectorize(ids: number[]): Promise<void> {
    await api.post('/vectors/batch-vectorize', { ids })
  }

  /**
   * 向量化所有未向量化的切片
   */
  static async vectorizeAll(): Promise<void> {
    await api.post('/vectors/vectorize-all')
  }

  /**
   * 查询向量化进度
   *
   * @returns 进度信息
   */
  static async getProgress(): Promise<{
    totalChunks: number
    vectorizedCount: number
    unvectorizedCount: number
    progress: number
  }> {
    const response = await api.get<{ data: any }>('/vectors/progress')
    return response.data
  }
}
