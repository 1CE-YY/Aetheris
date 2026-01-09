<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->

<template>
  <div class="chat-container">
    <!-- 导航栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">
          <MessageOutlined class="logo-icon" />
          <span class="logo-text">Aetheris RAG - 智能问答</span>
        </div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <!-- 主内容区 -->
    <a-layout-content class="content">
      <!-- 问答输入区域 -->
      <a-card class="input-card" title="提问">
        <a-form
          :model="formState"
          layout="vertical"
          @submit="handleSubmit"
        >
          <a-form-item label="问题" required>
            <a-textarea
              v-model:value="formState.question"
              placeholder="请输入您的问题，例如：什么是 RAG？"
              :rows="4"
              :maxlength="1000"
              show-count
              :disabled="loading"
            />
          </a-form-item>

          <a-form-item label="问答模式">
            <a-row :gutter="16">
              <a-col :span="24">
                <a-switch
                  v-model:checked="formState.useRag"
                  checked-children="RAG 模式"
                  un-checked-children="纯 LLM 模式"
                  :disabled="loading"
                />
                <a-tooltip title="RAG 模式：基于学习资源生成答案，包含引用来源。纯 LLM 模式：直接调用大模型，无引用来源。">
                  <QuestionCircleOutlined style="margin-left: 8px; color: #999;" />
                </a-tooltip>
                <div class="form-item-hint">
                  <span v-if="formState.useRag">
                    <FileTextOutlined /> 基于学习资源检索生成答案，包含引用来源
                  </span>
                  <span v-else>
                    <RobotOutlined /> 直接调用大模型，不使用检索结果
                  </span>
                </div>
              </a-col>
            </a-row>
          </a-form-item>

          <a-form-item v-if="formState.useRag" label="检索参数">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="Top-K" :label-col="{ span: 24 }">
                  <a-input-number
                    v-model:value="formState.topK"
                    :min="1"
                    :max="20"
                    :disabled="loading"
                    style="width: 100%"
                  />
                  <div class="form-item-hint">
                    检索返回的相关切片数量（1-20）
                  </div>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                html-type="submit"
                :loading="loading"
                size="large"
              >
                <SendOutlined /> 提交问题
              </a-button>
              <a-button
                @click="handleClear"
                :disabled="loading"
              >
                <ClearOutlined /> 清空
              </a-button>
              <a-button
                v-if="error"
                @click="handleRetry"
                :disabled="loading"
              >
                <ReloadOutlined /> 重试
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 答案展示区域 -->
      <AnswerDisplay
        :loading="loading"
        :answer="answer"
        :citations="citations"
        :evidence-insufficient="evidenceInsufficient"
        :fallback-resources="fallbackResources"
        :latency-ms="latencyMs"
        @citation-click="handleCitationClick"
      />

      <!-- 历史记录（可选，暂时注释） -->
      <!-- <a-card class="history-card" title="历史记录">
        <a-empty
          description="暂无历史记录"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
        />
      </a-card> -->
    </a-layout-content>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import {
  MessageOutlined,
  SendOutlined,
  ClearOutlined,
  ReloadOutlined,
  QuestionCircleOutlined,
  FileTextOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChat } from '@/composables/useChat'
import AnswerDisplay from '@/components/chat/AnswerDisplay.vue'
import type { Citation } from '@/services/chat.service'

const router = useRouter()
const userStore = useUserStore()

// 使用问答 composable
const {
  loading,
  answer,
  citations,
  evidenceInsufficient,
  fallbackResources,
  latencyMs,
  error,
  lastQuestion,
  askQuestion,
  clearAnswerAndState,
  restoreState,
  retryQuestion
} = useChat()

// 表单状态
const formState = reactive({
  question: '',
  topK: 5,
  useRag: true
})

/**
 * 提交问答
 */
const handleSubmit = async () => {
  // 验证问题文本
  if (!formState.question || formState.question.trim().length === 0) {
    message.error('请输入问题')
    return
  }

  // 调用问答
  const success = await askQuestion({
    question: formState.question.trim(),
    topK: formState.topK,
    useRag: formState.useRag
  })

  if (success) {
    // 清空问题输入框（可选，保留问题方便再次提问）
    // formState.question = ''
  }
}

/**
 * 清空答案
 */
const handleClear = () => {
  clearAnswerAndState()
  formState.question = ''  // 清空问题输入框
  message.success('已清空')
}

/**
 * 重试问答
 */
const handleRetry = async () => {
  await retryQuestion()
}

/**
 * 处理引用卡片点击
 */
const handleCitationClick = (citation: Citation) => {
  console.log('点击引用:', citation)
  // 可以在这里添加额外的处理逻辑，如滚动到位置等
}

/**
 * 退出登录
 */
const handleLogout = () => {
  userStore.logout()
  message.success('已退出登录')
  router.push('/login')
}

/**
 * 组件挂载时恢复状态
 */
onMounted(() => {
  restoreState()

  // 恢复表单状态
  if (lastQuestion.value) {
    if (lastQuestion.value.question) {
      formState.question = lastQuestion.value.question
    }
    if (lastQuestion.value.topK !== undefined) {
      formState.topK = lastQuestion.value.topK
    }
    if (lastQuestion.value.useRag !== undefined) {
      formState.useRag = lastQuestion.value.useRag
    }
  }

  if (answer.value) {
    console.log('问答状态已恢复')
  }
})

/**
 * 离开路由前保存状态
 */
onBeforeRouteLeave((to, from, next) => {
  if (to.name === 'ResourceDetail' && answer.value) {
    console.log('保存问答状态')
  }
  next()
})
</script>

<style scoped>
.chat-container {
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

.input-card {
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-item-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.history-card {
  margin-top: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
