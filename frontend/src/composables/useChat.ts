/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ChatService, { type AskRequest, type AnswerResponse, type Citation } from '@/services/chat.service'
import { useUserStore } from '@/stores/user'

/**
 * sessionStorage 存储键
 */
const STORAGE_KEY = 'aetheris_chat_state'

/**
 * 状态过期时间（24小时）
 */
const STATE_EXPIRY_MS = 24 * 60 * 60 * 1000

/**
 * 问答 Composable
 *
 * <p>封装问答逻辑，包括：
 * <ul>
 *   <li>提交问答请求</li>
 *   <li>加载状态管理</li>
 *   <li>错误处理</li>
 *   <li>状态持久化（sessionStorage）</li>
 * </ul>
 */
export function useChat() {
  const userStore = useUserStore()

  // ========== 状态 ==========
  /**
   * 加载状态
   */
  const loading = ref<boolean>(false)

  /**
   * 答案响应
   */
  const answerResponse = ref<AnswerResponse | null>(null)

  /**
   * 答案文本
   */
  const answer = ref<string>('')

  /**
   * 引用列表
   */
  const citations = ref<Citation[]>([])

  /**
   * 证据是否不足
   */
  const evidenceInsufficient = ref<boolean>(false)

  /**
   * 降级资源列表
   */
  const fallbackResources = ref<any[]>([])

  /**
   * 响应时间（毫秒）
   */
  const latencyMs = ref<number>(0)

  /**
   * 错误信息
   */
  const error = ref<string | null>(null)

  /**
   * 上次问答请求（支持重试）
   */
  const lastQuestion = ref<AskRequest | null>(null)

  // ========== 方法 ==========

  /**
   * 提交问答请求
   *
   * @param request 问答请求
   * @returns 是否成功
   */
  const askQuestion = async (request: AskRequest): Promise<boolean> => {
    // 验证用户登录
    if (!userStore.isLoggedIn) {
      message.error('请先登录')
      return false
    }

    // 验证问题文本
    if (!request.question || request.question.trim().length === 0) {
      message.error('请输入问题')
      return false
    }

    // 重置状态
    loading.value = true
    error.value = null
    answer.value = ''
    citations.value = []
    evidenceInsufficient.value = false
    fallbackResources.value = []
    latencyMs.value = 0

    try {
      // 调用问答 API
      const response = await ChatService.ask(request)

      // 更新状态
      answerResponse.value = response
      answer.value = response.answer
      citations.value = response.citations || []
      evidenceInsufficient.value = response.evidenceInsufficient
      fallbackResources.value = response.fallbackResources || []
      latencyMs.value = response.latencyMs

      // 保存上次问题（支持重试）
      lastQuestion.value = request

      // 显示提示
      if (response.evidenceInsufficient) {
        message.warning('证据不足，检索到的相关信息较少')
      } else if (response.fallbackResources && response.fallbackResources.length > 0) {
        message.info('AI 服务暂时不可用，已返回检索结果')
      } else {
        message.success('问答完成')
      }

      // 保存状态到 sessionStorage
      saveState()

      // TODO: 行为记录触发（T059）
      // 每次问答调用时，调用 POST /api/behaviors/query 记录查询行为
      // await BehaviorService.recordQuery({
      //   userId: userStore.userInfo!.id,
      //   queryText: request.question,
      //   sessionId: getSessionId()
      // })

      return true
    } catch (err: any) {
      console.error('问答失败:', err)

      // 提取错误信息
      const errorMessage = err.response?.data?.message
        || err.customMessage
        || err.message
        || '问答失败，请稍后重试'

      error.value = errorMessage
      message.error(errorMessage)

      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 清空答案
   */
  const clearAnswer = () => {
    answerResponse.value = null
    answer.value = ''
    citations.value = []
    evidenceInsufficient.value = false
    fallbackResources.value = []
    latencyMs.value = 0
    error.value = null
  }

  /**
   * 保存状态到 sessionStorage
   *
   * <p>在问答成功后调用，将当前状态序列化为 JSON 并存储。
   * <p>如果数据超过 4MB，则跳过存储以避免超出 sessionStorage 限制。
   */
  const saveState = () => {
    try {
      const state = {
        answerResponse: answerResponse.value,
        answer: answer.value,
        citations: citations.value,
        evidenceInsufficient: evidenceInsufficient.value,
        fallbackResources: fallbackResources.value,
        latencyMs: latencyMs.value,
        lastQuestion: lastQuestion.value,
        timestamp: Date.now()
      }
      const json = JSON.stringify(state)

      // 检查大小（sessionStorage 限制约 5-10MB）
      if (json.length > 4 * 1024 * 1024) {
        console.warn('问答状态过大，跳过存储')
        return
      }

      sessionStorage.setItem(STORAGE_KEY, json)
    } catch (error) {
      console.warn('保存问答状态失败:', error)
    }
  }

  /**
   * 从 sessionStorage 恢复状态
   *
   * <p>在页面挂载时调用，从 sessionStorage 恢复上次的问答状态。
   */
  const restoreState = () => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY)
      if (saved) {
        const state = JSON.parse(saved)

        // 检查是否过期（24小时）
        const isExpired = Date.now() - state.timestamp > STATE_EXPIRY_MS
        if (isExpired) {
          clearState()
          return
        }

        // 恢复状态
        answerResponse.value = state.answerResponse
        answer.value = state.answer
        citations.value = state.citations || []
        evidenceInsufficient.value = state.evidenceInsufficient
        fallbackResources.value = state.fallbackResources || []
        latencyMs.value = state.latencyMs
        lastQuestion.value = state.lastQuestion
      }
    } catch (error) {
      console.warn('恢复问答状态失败:', error)
      clearState()
    }
  }

  /**
   * 清除 sessionStorage 中的状态
   */
  const clearState = () => {
    try {
      sessionStorage.removeItem(STORAGE_KEY)
    } catch (error) {
      console.warn('清除问答状态失败:', error)
    }
  }

  /**
   * 清空答案和状态
   *
   * <p>同时清除内存状态和 sessionStorage
   */
  const clearAnswerAndState = () => {
    clearAnswer()
    clearState()
  }

  /**
   * 重试上次问答
   */
  const retryQuestion = async (): Promise<boolean> => {
    if (!lastQuestion.value) {
      message.warning('没有可重试的问答')
      return false
    }
    return await askQuestion(lastQuestion.value)
  }

  // ========== 返回 ==========
  return {
    // 状态
    loading,
    answerResponse,
    answer,
    citations,
    evidenceInsufficient,
    fallbackResources,
    latencyMs,
    error,
    lastQuestion,

    // 方法
    askQuestion,
    clearAnswer,
    clearAnswerAndState,
    restoreState,
    retryQuestion
  }
}

/**
 * 默认导出 useChat
 */
export default useChat
