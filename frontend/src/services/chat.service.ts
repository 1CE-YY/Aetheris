/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import api from './api'

/**
 * 问答请求接口
 */
export interface AskRequest {
  question: string // 问题文本
  topK?: number // 检索返回的切片数量（可选，默认 5）
  useRag?: boolean // 是否使用 RAG 流程（可选，默认 true）
}

/**
 * PDF 位置信息
 */
export interface PdfLocation {
  type: 'pdf'
  pageStart: number // 起始页码
  pageEnd: number // 结束页码
}

/**
 * Markdown 位置信息
 */
export interface MarkdownLocation {
  type: 'markdown'
  chapterPath: string // 章节路径（如 "第一章>1.1节"）
}

/**
 * 引用位置信息（联合类型）
 */
export type CitationLocation = PdfLocation | MarkdownLocation

/**
 * 引用对象
 */
export interface Citation {
  resourceId: string // 资源 ID
  resourceTitle: string // 资源标题
  chunkId: string // 切片 ID
  chunkIndex: number // 切片索引
  location: CitationLocation // 位置信息
  snippet: string // 文本摘录（100-200 字符）
  score: number // 相似度分数（0.0-1.0）
}

/**
 * 资源简略信息（降级场景）
 */
export interface ResourceBrief {
  id: string // 资源 ID
  title: string // 资源标题
  tags: string // 标签（逗号分隔）
  fileType: string // 文件类型（PDF/MARKDOWN）
  description?: string // 资源描述
  uploadTime: string // 上传时间
}

/**
 * 问答响应接口
 */
export interface AnswerResponse {
  answer: string // AI 生成的答案文本
  citations: Citation[] // 答案引用的证据列表
  evidenceInsufficient: boolean // 标识证据是否不足
  fallbackResources?: ResourceBrief[] // 降级时返回的候选资源列表
  latencyMs: number // 总响应时间（毫秒）
}

/**
 * 聊天服务类
 *
 * <p>封装 RAG 问答相关的 API 调用，包括：
 * <ul>
 *   <li>提交问答请求</li>
 * </ul>
 */
export class ChatService {
  /**
   * 提交问答请求
   *
   * @param request 问答请求（包含 question 和 topK）
   * @returns 问答响应（包含 answer、citations、evidenceInsufficient、fallbackResources、latencyMs）
   */
  static async ask(request: AskRequest): Promise<AnswerResponse> {
    const response = await api.post<any, AnswerResponse>('/chat/ask', request)
    return response
  }
}

/**
 * 默认导出 ChatService 实例
 */
export default ChatService
