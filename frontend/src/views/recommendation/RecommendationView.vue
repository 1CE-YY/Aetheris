<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->

<template>
  <div class="recommendation-container">
    <!-- 顶部导航栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">
          <StarOutlined class="logo-icon" />
          <span>Aetheris RAG - 个性化推荐</span>
        </div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <!-- 页面头部 -->
    <a-layout-content class="content">
      <a-page-header
        title="个性化推荐"
        :sub-title="personalized ? '基于您的学习兴趣' : '热门学习资源'"
        @back="() => router.push('/')"
        class="page-header"
      >
        <template #extra>
          <a-button
            type="primary"
            :loading="loading"
            @click="refresh(10)"
          >
            刷新推荐
          </a-button>
        </template>
      </a-page-header>

      <!-- 非个性化提示 -->
      <a-alert
      v-if="!loading && !personalized && recommendations.length > 0"
      type="info"
      message="默认推荐"
      description="完成更多搜索后，系统将为您提供基于学习兴趣的个性化推荐"
      show-icon
      class="profile-alert"
    />

    <!-- 个性化提示 -->
    <a-alert
      v-if="!loading && personalized"
      type="success"
      message="个性化推荐"
      description="以下推荐基于您最近的学习兴趣生成"
      show-icon
      class="profile-alert"
    />

    <!-- 加载中 -->
    <a-spin :spinning="loading" tip="正在生成推荐...">
      <div class="recommendation-list">
        <a-list
          :grid="{ gutter: 16, xs: 1, sm: 1, md: 2, lg: 2, xl: 2, xxl: 3 }"
          :data-source="recommendations"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <RecommendationCard
                :item="item"
                @click="handleCardClick"
                @favorite="handleFavoriteResource"
              />
            </a-list-item>
          </template>
        </a-list>
      </div>

      <!-- 空状态 -->
      <a-empty
        v-if="!loading && recommendations.length === 0"
        description="暂无推荐，请先使用问答功能积累学习兴趣"
      >
        <a-button type="primary" @click="$router.push('/chat')">
          去问答
        </a-button>
      </a-empty>
    </a-spin>

    <!-- 性能信息 -->
    <div v-if="latencyMs > 0" class="performance-info">
      <a-typography-text type="secondary">
        推荐生成耗时：{{ latencyMs }}ms
      </a-typography-text>
    </div>
    </a-layout-content>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { StarOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import RecommendationCard from '@/components/recommendation/RecommendationCard.vue'
import { useRecommendation } from '@/composables/useRecommendation'

const router = useRouter()
const userStore = useUserStore()

const {
  loading,
  recommendations,
  personalized,
  latencyMs,
  fetchRecommendations,
  handleClickResource: recordClick,
  handleFavorite,
  refresh
} = useRecommendation()

/** 点击推荐资源 → 跳转资源详情 */
function handleCardClick(resourceId: string) {
  recordClick(resourceId)
  router.push({
    name: 'ResourceDetail',
    params: { id: resourceId }
  })
}

/** 收藏推荐资源 */
function handleFavoriteResource(resourceId: string) {
  handleFavorite(resourceId)
}

/** 退出登录 */
function handleLogout() {
  userStore.logout()
  message.success('已退出登录')
  router.push('/login')
}

/** 页面挂载时自动加载推荐 */
onMounted(() => {
  fetchRecommendations(10)
})
</script>

<style scoped>
.recommendation-container {
  min-height: 100vh;
  background: #f0f2f5;
}

.header {
  background: #fff;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
}

.logo-icon {
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #333;
}

.content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  background: #fff;
  padding: 16px 24px;
  margin-bottom: 16px;
  border-radius: 4px;
}

.profile-alert {
  margin-bottom: 16px;
}

.recommendation-list {
  min-height: 200px;
}

.performance-info {
  text-align: center;
  padding: 16px 0;
}
</style>
