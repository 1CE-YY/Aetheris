<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->

<template>
  <div class="answer-display">
    <!-- 答案文本 -->
    <a-card class="answer-card" :loading="loading">
      <template #title>
        <div class="answer-title">
          <MessageOutlined class="icon" />
          <span>AI 回答</span>
          <a-tag v-if="!loading && answer" color="blue" class="latency-tag">
            耗时 {{ latencyMs }}ms
          </a-tag>
        </div>
      </template>

      <!-- 空状态 -->
      <a-empty
        v-if="!loading && !answer"
        description="请在上方输入问题并提交"
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
      />

      <!-- 答案内容 -->
      <div v-else-if="answer" class="answer-content">
        <!-- 证据不足提示 -->
        <a-alert
          v-if="evidenceInsufficient"
          type="warning"
          show-icon
          message="证据不足"
          description="根据现有资料无法完整回答您的问题，请查看下方详细建议或切换到纯 LLM 模式。"
          style="margin-bottom: 16px"
        >
        </a-alert>

        <!-- LLM 不可用提示 -->
        <a-alert
          v-if="!evidenceInsufficient && fallbackResources && fallbackResources.length > 0"
          type="info"
          show-icon
          message="AI 服务暂时不可用"
          description="以下是基于检索到的相关资源："
          style="margin-bottom: 16px"
        />

        <!-- 答案文本（Markdown 渲染） -->
        <div class="answer-text" v-html="renderedAnswer"></div>
      </div>

      <!-- 加载中 -->
      <a-skeleton v-if="loading" active :paragraph="{ rows: 4 }" />
    </a-card>

    <!-- 引用列表 -->
    <div v-if="citations && citations.length > 0" class="citations-section">
      <a-divider>引用来源</a-divider>
      <a-row :gutter="[16, 16]">
        <a-col
          v-for="(citation, index) in citations"
          :key="citation.chunkId"
          :span="24"
        >
          <CitationCard
            :citation="citation"
            :index="index"
            @click="handleCitationClick"
          />
        </a-col>
      </a-row>
    </div>

    <!-- 降级资源列表 -->
    <div v-if="!evidenceInsufficient && fallbackResources && fallbackResources.length > 0" class="fallback-resources-section">
      <a-divider>降级资源</a-divider>
      <a-row :gutter="[16, 16]">
        <a-col
          v-for="resource in fallbackResources"
          :key="resource.id"
          :span="24"
        >
          <FallbackResourceCard :resource="resource" />
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  MessageOutlined,
  WarningOutlined,
  EditOutlined,
  SearchOutlined,
  BookOutlined
} from '@ant-design/icons-vue'
import { marked } from 'marked'
import CitationCard from './CitationCard.vue'
import FallbackResourceCard from './FallbackResourceCard.vue'
import type { Citation } from '@/services/chat.service'

/**
 * 组件 Props
 */
interface Props {
  loading?: boolean
  answer?: string
  citations?: Citation[]
  evidenceInsufficient?: boolean
  fallbackResources?: any[]
  latencyMs?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  answer: '',
  citations: () => [],
  evidenceInsufficient: false,
  fallbackResources: () => [],
  latencyMs: 0
})

/**
 * 组件 Emits
 */
interface Emits {
  (e: 'citationClick', citation: Citation): void
}

const emit = defineEmits<Emits>()

/**
 * 渲染 Markdown 格式的答案
 */
const renderedAnswer = computed(() => {
  if (!props.answer) return ''
  return marked(props.answer)
})

/**
 * 处理引用卡片点击事件
 */
const handleCitationClick = (citation: Citation) => {
  emit('citationClick', citation)
}
</script>

<style scoped>
.answer-display {
  margin-top: 24px;
}

.answer-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.answer-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-title .icon {
  font-size: 18px;
  color: #1890ff;
}

.latency-tag {
  margin-left: auto;
}

.answer-content {
  line-height: 1.8;
}

.answer-text {
  font-size: 15px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Markdown 样式 */
.answer-text :deep(h1) {
  font-size: 24px;
  font-weight: bold;
  margin: 16px 0 8px;
  color: #1890ff;
}

.answer-text :deep(h2) {
  font-size: 20px;
  font-weight: bold;
  margin: 14px 0 8px;
  color: #1890ff;
}

.answer-text :deep(h3) {
  font-size: 18px;
  font-weight: bold;
  margin: 12px 0 8px;
  color: #1890ff;
}

.answer-text :deep(p) {
  margin: 8px 0;
  line-height: 1.8;
}

.answer-text :deep(ul),
.answer-text :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.answer-text :deep(li) {
  margin: 4px 0;
}

.answer-text :deep(code) {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  color: #e74c3c;
}

.answer-text :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 12px 0;
}

.answer-text :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #333;
}

.answer-text :deep(blockquote) {
  border-left: 4px solid #1890ff;
  padding-left: 16px;
  margin: 12px 0;
  color: #666;
  font-style: italic;
}

.answer-text :deep(a) {
  color: #1890ff;
  text-decoration: none;
}

.answer-text :deep(a:hover) {
  text-decoration: underline;
}

.citations-section {
  margin-top: 24px;
}

.fallback-resources-section {
  margin-top: 24px;
}

/* 证据不足提示样式 */
.insufficient-evidence-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.insufficient-evidence-content {
  line-height: 1.8;
}

.insufficient-evidence-content p {
  margin-bottom: 12px;
  color: #333;
}

.suggestion-list {
  list-style: none;
  padding: 0;
  margin: 12px 0;
}

.suggestion-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  color: #666;
}

.suggestion-list li .anticon {
  color: #faad14;
  font-size: 16px;
}

:deep(.ant-statistic-title) {
  font-size: 14px;
  color: #666;
}
</style>
