<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->
<template>
  <a-card
    class="recommendation-card"
    hoverable
    @click="handleClick"
  >
    <template #title>
      <div class="card-header">
        <span class="title">{{ item.title }}</span>
        <a-tag v-if="scoreTag" :color="scoreTag.color">{{ scoreTag.label }}</a-tag>
      </div>
    </template>

    <template #extra>
      <a-space>
        <a-tag v-if="item.fileType" color="blue">{{ item.fileType }}</a-tag>
        <a-button
          type="text"
          size="small"
          @click.stop="handleFavoriteClick"
        >
          <template #icon>
            <star-outlined />
          </template>
        </a-button>
      </a-space>
    </template>

    <!-- 推荐理由 -->
    <div v-if="item.reason" class="reason-section">
      <div class="section-label">推荐理由</div>
      <p class="reason-text">{{ item.reason }}</p>
    </div>

    <!-- 学习建议 -->
    <div v-if="item.suggestion" class="suggestion-section">
      <div class="section-label">学习建议</div>
      <p class="suggestion-text">{{ item.suggestion }}</p>
    </div>

    <!-- 标签 -->
    <div v-if="item.tags" class="tags-section">
      <a-tag
        v-for="tag in tagList"
        :key="tag"
        size="small"
        class="resource-tag"
      >
        {{ tag }}
      </a-tag>
    </div>

    <!-- 证据引用 -->
    <div v-if="item.citations && item.citations.length > 0" class="citations-section">
      <a-collapse ghost size="small">
        <a-collapse-panel :header="`证据引用 (${item.citations.length})`">
          <div
            v-for="(citation, index) in item.citations"
            :key="index"
            class="citation-item"
          >
            <div class="citation-header">
              <span class="citation-index">#{{ index + 1 }}</span>
              <span class="citation-location">{{ formatLocation(citation.location) }}</span>
            </div>
            <p class="citation-snippet">{{ citation.snippet }}</p>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { StarOutlined } from '@ant-design/icons-vue'
import type { RecommendationItem } from '@/services/recommendation.service'

const props = defineProps<{
  item: RecommendationItem
}>()

const emit = defineEmits<{
  (e: 'click', resourceId: string): void
  (e: 'favorite', resourceId: string): void
}>()

/** 标签列表 */
const tagList = computed(() => {
  if (!props.item.tags) return []
  return props.item.tags.split(',').map(t => t.trim()).filter(Boolean)
})

/** 相似度分数标签 */
const scoreTag = computed(() => {
  const score = props.item.score
  if (score <= 0) return null
  if (score >= 0.8) return { label: `${(score * 100).toFixed(0)}%`, color: 'green' }
  if (score >= 0.6) return { label: `${(score * 100).toFixed(0)}%`, color: 'blue' }
  return { label: `${(score * 100).toFixed(0)}%`, color: 'default' }
})

/** 格式化位置信息 */
function formatLocation(location: any): string {
  if (!location) return '未知位置'
  if (location.type === 'pdf') {
    return `PDF 第 ${location.pageStart}-${location.pageEnd} 页`
  }
  if (location.type === 'markdown') {
    return location.chapterPath || '未知章节'
  }
  return '未知位置'
}

/** 点击卡片 */
function handleClick() {
  emit('click', props.item.resourceId)
}

/** 点击收藏 */
function handleFavoriteClick() {
  emit('favorite', props.item.resourceId)
}
</script>

<style scoped>
.recommendation-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 400px;
}

.section-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
  font-weight: 500;
}

.reason-section {
  margin-bottom: 10px;
}

.reason-text {
  margin: 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}

.suggestion-section {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f6ffed;
  border-radius: 6px;
  border-left: 3px solid #52c41a;
}

.suggestion-text {
  margin: 0;
  font-size: 13px;
  color: #389e0d;
  line-height: 1.6;
}

.tags-section {
  margin-bottom: 8px;
}

.resource-tag {
  margin-bottom: 4px;
}

.citations-section {
  margin-top: 8px;
}

.citation-item {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.citation-item:last-child {
  border-bottom: none;
}

.citation-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.citation-index {
  font-size: 12px;
  font-weight: 600;
  color: #1890ff;
}

.citation-location {
  font-size: 12px;
  color: #8c8c8c;
}

.citation-snippet {
  margin: 0;
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
