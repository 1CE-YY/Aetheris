/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import { ref } from 'vue'
import { message } from 'ant-design-vue'
import RecommendationService, {
  type RecommendationItem,
  type RecommendationResponse
} from '@/services/recommendation.service'
import { useUserStore } from '@/stores/user'

/**
 * 推荐 Composable
 *
 * <p>封装推荐逻辑，包括：
 * <ul>
 *   <li>获取个性化推荐</li>
 *   <li>加载状态管理</li>
 *   <li>点击/收藏行为记录</li>
 *   <li>错误处理</li>
 * </ul>
 */
export function useRecommendation() {
  const userStore = useUserStore()

  // ========== 状态 ==========
  /** 加载状态 */
  const loading = ref<boolean>(false)

  /** 推荐列表 */
  const recommendations = ref<RecommendationItem[]>([])

  /** 是否为个性化推荐 */
  const personalized = ref<boolean>(false)

  /** 推荐生成耗时 */
  const latencyMs = ref<number>(0)

  /** 错误信息 */
  const error = ref<string | null>(null)

  // ========== 方法 ==========

  /**
   * 获取推荐列表
   *
   * @param topN 推荐数量（默认 10）
   * @returns 是否成功
   */
  const fetchRecommendations = async (topN = 10): Promise<boolean> => {
    if (!userStore.isLoggedIn) {
      message.error('请先登录')
      return false
    }

    loading.value = true
    error.value = null

    try {
      const response: RecommendationResponse =
        await RecommendationService.getRecommendations(topN)

      recommendations.value = response.items || []
      personalized.value = response.personalized
      latencyMs.value = response.latencyMs

      if (response.personalized) {
        message.success(`已为您生成 ${response.count} 条个性化推荐`)
      } else {
        message.info('推荐基于热门资源，使用问答功能后可获得个性化推荐')
      }

      return true
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || err.message || '获取推荐失败'
      error.value = errorMessage
      message.error(errorMessage)
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 记录推荐点击行为
   *
   * <p>点击推荐资源卡片时调用，异步记录行为并更新用户画像
   *
   * @param resourceId 资源ID
   */
  const handleClickResource = async (resourceId: string) => {
    try {
      await RecommendationService.recordClick(resourceId)
      console.log('[Recommendation] 点击行为已记录:', resourceId)
    } catch (err: any) {
      console.warn('[Recommendation] 点击行为记录失败:', err.message)
    }
  }

  /**
   * 记录推荐收藏行为
   *
   * <p>收藏推荐资源时调用，异步记录行为并更新用户画像
   *
   * @param resourceId 资源ID
   */
  const handleFavorite = async (resourceId: string) => {
    try {
      await RecommendationService.recordFavorite(resourceId)
      message.success('收藏成功')
      console.log('[Recommendation] 收藏行为已记录:', resourceId)
    } catch (err: any) {
      console.warn('[Recommendation] 收藏行为记录失败:', err.message)
      message.error('收藏失败，请稍后重试')
    }
  }

  /**
   * 刷新推荐列表
   */
  const refresh = async (topN = 10): Promise<boolean> => {
    return fetchRecommendations(topN)
  }

  // ========== 返回 ==========
  return {
    loading,
    recommendations,
    personalized,
    latencyMs,
    error,
    fetchRecommendations,
    handleClickResource,
    handleFavorite,
    refresh
  }
}

export default useRecommendation
