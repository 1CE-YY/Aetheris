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
        :row-selection="rowSelection"
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

      <!-- 批量删除浮层 -->
      <div v-if="selectedRowKeys.length > 0" class="batch-actions">
        <a-space>
          <span>已选择 {{ selectedRowKeys.length }} 项</span>
          <a-button type="primary" danger @click="handleBatchDelete">
            批量删除
          </a-button>
          <a-button @click="clearSelection">取消选择</a-button>
        </a-space>
      </div>
      </div>
    </a-layout-content>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
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

// 行选择状态
const selectedRowKeys = ref<number[]>([])
const rowSelection = {
  selectedRowKeys: selectedRowKeys,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}

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

// 删除处理
const handleDelete = (record: Resource) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除资源"${record.title}"吗？删除后无法恢复。`,
    icon: h(ExclamationCircleOutlined),
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        // 单个删除也调用批量删除接口，传入单个 ID
        await ResourceService.batchDeleteResources([record.id])
        message.success('资源已删除')
        clearSelection()
        await loadResources()
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败，请重试')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个资源吗？删除后无法恢复。`,
    icon: h(ExclamationCircleOutlined),
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await ResourceService.batchDeleteResources(selectedRowKeys.value)
        message.success(`成功删除 ${selectedRowKeys.value.length} 个资源`)
        clearSelection()
        await loadResources()
      } catch (error: any) {
        message.error(error.response?.data?.message || '批量删除失败，请重试')
      }
    }
  })
}

// 清除选择
const clearSelection = () => {
  selectedRowKeys.value = []
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

.batch-actions {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 20px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  z-index: 1000;
}
</style>
