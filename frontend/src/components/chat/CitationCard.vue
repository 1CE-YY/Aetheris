<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->

<template>
  <a-card
    class="citation-card"
    hoverable
    @click="handleClick"
  >
    <template #title>
      <div class="citation-title">
        <FileTextOutlined class="icon" />
        <span class="resource-title">{{ citation.resourceTitle }}</span>
        <a-tag :color="getScoreColor(citation.score)" class="score-tag">
          相似度: {{ (citation.score * 100).toFixed(1) }}%
        </a-tag>
      </div>
    </template>

    <div class="citation-content">
      <!-- 位置信息 -->
      <div class="citation-location">
        <EnvironmentOutlined class="location-icon" />
        <span class="location-text">{{ locationText }}</span>
        <a-tag color="blue" class="chunk-index-tag">
          切片 #{{ citation.chunkIndex }}
        </a-tag>
      </div>

      <!-- 文本摘录 -->
      <div class="citation-snippet">
        <a-typography-text
          :ellipsis="{ rows: 3, expandable: true, symbol: '展开' }"
        >
          {{ citation.snippet }}
        </a-typography-text>
      </div>
    </div>

    <template #actions>
      <a-button type="link" size="small" @click.stop="handleViewResource">
        <EyeOutlined /> 查看资源
      </a-button>
      <a-button
        v-if="citation.location.type === 'pdf'"
        type="link"
        size="small"
        @click.stop="handleViewChunk"
      >
        <SearchOutlined /> 定位切片
      </a-button>
    </template>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileTextOutlined,
  EnvironmentOutlined,
  EyeOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import type { Citation } from '@/services/chat.service'

/**
 * 组件 Props
 */
interface Props {
  citation: Citation
  index?: number
}

const props = withDefaults(defineProps<Props>(), {
  index: 0
})

/**
 * 组件 Emits
 */
interface Emits {
  (e: 'click', citation: Citation): void
}

const emit = defineEmits<Emits>()
const router = useRouter()

/**
 * 格式化位置信息文本
 */
const locationText = computed(() => {
  const location = props.citation.location

  if (location.type === 'pdf') {
    const { pageStart, pageEnd } = location
    return pageStart === pageEnd
      ? `第 ${pageStart} 页`
      : `第 ${pageStart}-${pageEnd} 页`
  } else {
    return location.chapterPath
  }
})

/**
 * 根据相似度分数返回标签颜色
 */
const getScoreColor = (score: number): string => {
  if (score >= 0.8) return 'green'
  if (score >= 0.6) return 'blue'
  if (score >= 0.4) return 'orange'
  return 'red'
}

/**
 * 处理卡片点击事件
 */
const handleClick = () => {
  emit('click', props.citation)
}

/**
 * 查看完整资源
 */
const handleViewResource = () => {
  router.push(`/resources/${props.citation.resourceId}`)
}

/**
 * 定位到具体切片（跳转到资源详情页并高亮）
 */
const handleViewChunk = () => {
  // 跳转到资源详情页，携带 chunkId 参数
  router.push({
    path: `/resources/${props.citation.resourceId}`,
    query: {
      chunkId: props.citation.chunkId,
      chunkIndex: props.citation.chunkIndex.toString()
    }
  })
}
</script>

<style scoped>
.citation-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 4px solid transparent;
}

.citation-card:hover {
  border-left-color: #1890ff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.citation-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.citation-title .icon {
  font-size: 18px;
  color: #1890ff;
}

.resource-title {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-tag {
  margin-left: auto;
}

.citation-content {
  margin-top: 12px;
}

.citation-location {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.location-icon {
  color: #1890ff;
  font-size: 16px;
}

.location-text {
  flex: 1;
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.chunk-index-tag {
  font-size: 12px;
}

.citation-snippet {
  padding: 12px;
  background: #fafafa;
  border-left: 3px solid #1890ff;
  border-radius: 4px;
  line-height: 1.6;
  color: #333;
  font-size: 14px;
}

:deep(.ant-typography-expand) {
  color: #1890ff;
  cursor: pointer;
}
</style>
