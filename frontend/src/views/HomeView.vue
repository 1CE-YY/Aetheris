<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->
<template>
  <div class="home-container">
    <!-- 导航栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">Aetheris RAG</div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <a-typography-title>欢迎使用 Aetheris RAG 系统</a-typography-title>
    <a-typography-paragraph>
      这是一个面向高校的学习资源检索与推荐系统，支持语义检索、RAG 问答和个性化推荐。
    </a-typography-paragraph>

    <a-row :gutter="[16, 16]">
      <a-col :span="8">
        <a-card title="资源管理" hoverable>
          <template #extra>
            <LinkOutlined />
          </template>
          <p>上传和管理学习资源（PDF、Markdown）</p>
          <router-link to="/resources">
            <a-button type="primary">查看资源</a-button>
          </router-link>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="智能问答" hoverable>
          <template #extra>
            <MessageOutlined />
          </template>
          <p>基于 RAG 的智能问答系统</p>
          <router-link to="/chat">
            <a-button type="primary">开始问答</a-button>
          </router-link>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="个性化推荐" hoverable>
          <template #extra>
            <StarOutlined />
          </template>
          <p>基于用户画像的个性化推荐</p>
          <router-link to="/recommendations">
            <a-button type="primary">查看推荐</a-button>
          </router-link>
        </a-card>
      </a-col>
    </a-row>

    <a-divider />

    <a-card title="快速开始">
      <a-steps direction="vertical" :current="-1">
        <a-step title="上传学习资源" description="支持 PDF 和 Markdown 格式的学习文档" />
        <a-step title="语义检索" description="基于向量相似度的智能检索" />
        <a-step title="RAG 问答" description="带引用来源的智能问答" />
        <a-step title="个性化推荐" description="基于用户画像的推荐系统" />
      </a-steps>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { LinkOutlined, MessageOutlined, StarOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const userStore = useUserStore()

/**
 * 处理登出
 */
function handleLogout() {
  userStore.logout()
  message.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
  padding: 0 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #333;
}
</style>
