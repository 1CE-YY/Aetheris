<template>
  <div class="resource-table">
    <a-table
      :columns="columns"
      :data-source="resources"
      :loading="loading"
      :pagination="paginationConfig"
      :row-key="rowKey"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 标题 -->
        <template v-if="column.key === 'title'">
          <a @click="$emit('view', record.id)" class="title-link">
            {{ record.title }}
          </a>
        </template>

        <!-- 标签 -->
        <template v-else-if="column.key === 'tags'">
          <a-space v-if="record.tags" :size="4">
            <a-tag v-for="tag in record.tags.split(',')" :key="tag" color="blue" size="small">
              {{ tag.trim() }}
            </a-tag>
          </a-space>
          <span v-else style="color: #999;">-</span>
        </template>

        <!-- 文件类型 -->
        <template v-else-if="column.key === 'fileType'">
          <a-tag :color="record.fileType === 'PDF' ? 'red' : 'green'">
            {{ record.fileType }}
          </a-tag>
        </template>

        <!-- 文件大小 -->
        <template v-else-if="column.key === 'fileSize'">
          {{ formatFileSize(record.fileSize) }}
        </template>

        <!-- 切片数量 -->
        <template v-else-if="column.key === 'chunkCount'">
          <a-badge :count="record.chunkCount" :number-style="{ backgroundColor: '#52c41a' }" />
        </template>

        <!-- 向量化状态 -->
        <template v-else-if="column.key === 'vectorized'">
          <a-tag :color="record.vectorized ? 'success' : 'processing'">
            {{ record.vectorized ? '已向量化' : '处理中' }}
          </a-tag>
        </template>

        <!-- 上传时间 -->
        <template v-else-if="column.key === 'uploadTime'">
          {{ formatTime(record.uploadTime) }}
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="$emit('view', record.id)">
            查看详情
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Resource } from '@/services/resource.service'

const props = defineProps<{
  resources: Resource[]
  loading?: boolean
  pagination?: any
  rowKey?: string
}>()

const emit = defineEmits<{
  (e: 'view', id: number): void
  (e: 'change', pag: any): void
}>()

// 表格列配置
const columns = [
  {
    title: '资源标题',
    dataIndex: 'title',
    key: 'title',
    width: '25%',
    ellipsis: true
  },
  {
    title: '标签',
    dataIndex: 'tags',
    key: 'tags',
    width: '15%'
  },
  {
    title: '类型',
    dataIndex: 'fileType',
    key: 'fileType',
    width: '10%'
  },
  {
    title: '大小',
    dataIndex: 'fileSize',
    key: 'fileSize',
    width: '10%'
  },
  {
    title: '切片数',
    dataIndex: 'chunkCount',
    key: 'chunkCount',
    width: '10%',
    align: 'center'
  },
  {
    title: '状态',
    dataIndex: 'vectorized',
    key: 'vectorized',
    width: '10%',
    align: 'center'
  },
  {
    title: '上传时间',
    dataIndex: 'uploadTime',
    key: 'uploadTime',
    width: '10%'
  },
  {
    title: '操作',
    key: 'action',
    width: '10%',
    align: 'center'
  }
]

const paginationConfig = computed(() => ({
  current: props.pagination?.current || 1,
  pageSize: props.pagination?.pageSize || 10,
  total: props.pagination?.total || 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
}))

// 表格分页变化
const handleTableChange = (pag: any) => {
  emit('change', pag)
}

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
</script>

<style scoped>
.title-link {
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.title-link:hover {
  text-decoration: underline;
}
</style>
