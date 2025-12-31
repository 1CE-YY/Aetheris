<template>
  <div class="resource-card">
    <a-card :bordered="true" :hoverable="true" class="card">
      <template #title>
        <a class="title-link" @click="goToDetail">
          {{ resource.title }}
        </a>
      </template>

      <template #extra>
        <a-tag :color="resource.fileType === 'PDF' ? 'red' : 'green'">
          {{ resource.fileType }}
        </a-tag>
      </template>

      <div class="card-content">
        <!-- 描述 -->
        <div v-if="resource.description" class="description">
          {{ resource.description }}
        </div>

        <!-- 标签 -->
        <div v-if="resource.tags" class="tags">
          <a-space>
            <a-tag v-for="tag in resource.tags.split(',')" :key="tag" color="blue" size="small">
              {{ tag.trim() }}
            </a-tag>
          </a-space>
        </div>

        <!-- 元数据 -->
        <a-row class="meta" :gutter="[16, 8]">
          <a-col :span="8">
            <div class="meta-item">
              <FileTextOutlined />
              <span>{{ formatFileSize(resource.fileSize) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="meta-item">
              <AppstoreOutlined />
              <span>{{ resource.chunkCount }} 个切片</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="meta-item">
              <CloudSyncOutlined :type="resource.vectorized ? 'success' : 'processing'" />
              <span>{{ resource.vectorized ? '已向量化' : '处理中' }}</span>
            </div>
          </a-col>
        </a-row>

        <!-- 上传时间 -->
        <div class="upload-time">
          <ClockCircleOutlined />
          {{ formatTime(resource.uploadTime) }}
        </div>
      </div>

      <template #actions>
        <a-button type="link" @click="goToDetail">
          <template #icon><EyeOutlined /></template>
          查看详情
        </a-button>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileTextOutlined,
  AppstoreOutlined,
  CloudSyncOutlined,
  ClockCircleOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import type { Resource } from '@/services/resource.service'

const props = defineProps<{
  resource: Resource
}>()

const router = useRouter()

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 格式化时间
const formatTime = (timeStr: string): string => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + ' 分钟前'
  if (diff < day) return Math.floor(diff / hour) + ' 小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + ' 天前'
  return date.toLocaleDateString('zh-CN')
}

// 跳转到详情页
const goToDetail = () => {
  router.push({ name: 'ResourceDetail', params: { id: props.resource.id } })
}
</script>

<style scoped>
.resource-card {
  margin-bottom: 16px;
}

.card {
  height: 100%;
}

.title-link {
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
  font-size: 16px;
}

.title-link:hover {
  text-decoration: underline;
}

.card-content {
  margin-bottom: 16px;
}

.description {
  color: #666;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tags {
  margin-bottom: 12px;
}

.meta {
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #999;
  font-size: 13px;
}

.upload-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #999;
  font-size: 12px;
}
</style>
