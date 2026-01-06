<template>
  <div class="resource-list-container">
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">Aetheris RAG - 学习资源管理</div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <a-layout-content class="content">
      <a-page-header
        title="资源列表"
        @back="() => router.push('/')"
        class="page-header"
      >
        <template #extra>
          <a-button type="primary" @click="handleUpload">
            <template #icon><PlusOutlined /></template>
            上传资源
          </a-button>
        </template>
      </a-page-header>

      <div class="content-wrapper">
        <a-table
        :columns="columns"
        :data-source="resources"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="viewResource(record.id)">{{ record.title }}</a>
          </template>
          <template v-else-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-else-if="column.key === 'uploadTime'">
            {{ formatTime(record.uploadTime) }}
          </template>
          <template v-else-if="column.key === 'vectorized'">
            <a-tag :color="record.vectorized ? 'green' : 'orange'">
              {{ record.vectorized ? '已向量化' : '处理中' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="viewResource(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
      </div>
    </a-layout-content>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import ResourceService from '@/services/resource.service'
import type { Resource } from '@/services/resource.service'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const resources = ref<Resource[]>([])
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  {
    title: '标题',
    key: 'title',
    dataIndex: 'title'
  },
  {
    title: '类型',
    key: 'fileType',
    dataIndex: 'fileType',
    width: 100
  },
  {
    title: '大小',
    key: 'fileSize',
    dataIndex: 'fileSize',
    width: 120
  },
  {
    title: '切片数',
    key: 'chunkCount',
    dataIndex: 'chunkCount',
    width: 100
  },
  {
    title: '上传时间',
    key: 'uploadTime',
    dataIndex: 'uploadTime',
    width: 180
  },
  {
    title: '向量化状态',
    key: 'vectorized',
    dataIndex: 'vectorized',
    width: 120
  },
  {
    title: '操作',
    key: 'action',
    width: 100
  }
]

const loadResources = async () => {
  loading.value = true
  try {
    const page = pagination.value.current - 1  // 后端页码从 0 开始
    const size = pagination.value.pageSize

    const data = await ResourceService.getResourceList(page, size)
    resources.value = data.items
    pagination.value.total = data.total
  } catch (error: any) {
    message.error('加载资源列表失败: ' + (error.customMessage || error.message))
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadResources()
}

const handleUpload = () => {
  router.push({ name: 'ResourceUpload' })
}

const viewResource = (id: number) => {
  router.push({ name: 'ResourceDetail', params: { id } })
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const formatTime = (time: string): string => {
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadResources()
})
</script>

<style scoped>
.resource-list-container {
  min-height: 100vh;
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
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  background: #f0f2f5;
}

.page-header {
  background: #fff;
  padding: 16px 24px;
  margin-bottom: 16px;
  border-radius: 4px;
}

.content-wrapper {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
}
</style>
