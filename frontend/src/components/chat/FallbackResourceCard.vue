<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->

<template>
  <a-card
    class="fallback-resource-card"
    hoverable
    @click="handleClick"
  >
    <template #title>
      <div class="resource-title">
        <FileTextOutlined class="icon" />
        <span>{{ resource.title }}</span>
      </div>
    </template>

    <template #actions>
      <a-button type="link" size="small" @click.stop="handleViewResource">
        <EyeOutlined /> 查看资源
      </a-button>
    </template>
  </a-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { FileTextOutlined, EyeOutlined } from '@ant-design/icons-vue'
import type { ResourceBrief } from '@/services/chat.service'

/**
 * 组件 Props
 */
interface Props {
  resource: ResourceBrief
}

const props = defineProps<Props>()

/**
 * 组件 Emits
 */
interface Emits {
  (e: 'click', resource: ResourceBrief): void
}

const emit = defineEmits<Emits>()
const router = useRouter()

/**
 * 处理卡片点击事件
 */
const handleClick = () => {
  emit('click', props.resource)
}

/**
 * 查看完整资源
 */
const handleViewResource = () => {
  router.push(`/resources/${props.resource.id}`)
}
</script>

<style scoped>
.fallback-resource-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 4px solid #faad14;
  background: #fff7e6;
}

.fallback-resource-card:hover {
  border-left-color: #d48806;
  box-shadow: 0 4px 12px rgba(250, 173, 20, 0.3);
  transform: translateY(-2px);
}

.resource-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-title .icon {
  font-size: 18px;
  color: #faad14;
}

.resource-title span {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
