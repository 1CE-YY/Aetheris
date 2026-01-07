<template>
  <div class="resource-detail-container">
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">Aetheris RAG - 资源详情</div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <a-layout-content class="content">
      <a-page-header
        :title="resource?.title"
        @back="handleBack"
      >
        <template #extra>
          <a-space>
            <a-tag :color="resource?.vectorized ? 'green' : 'orange'">
              {{ resource?.vectorized ? '已向量化' : '处理中' }}
            </a-tag>
            <a-button type="default" @click="handleEdit">编辑</a-button>
            <a-button type="primary" danger @click="handleDelete">删除资源</a-button>
          </a-space>
        </template>
      </a-page-header>

      <a-card title="资源信息" class="info-card" v-if="resource">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="文件类型">
            {{ resource.fileType }}
          </a-descriptions-item>
          <a-descriptions-item label="文件大小">
            {{ formatFileSize(resource.fileSize) }}
          </a-descriptions-item>
          <a-descriptions-item label="切片数量">
            {{ resource.chunkCount }}
          </a-descriptions-item>
          <a-descriptions-item label="上传时间">
            {{ formatTime(resource.uploadTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="标签" v-if="resource.tags">
            {{ resource.tags }}
          </a-descriptions-item>
          <a-descriptions-item label="描述" :span="2" v-if="resource.description">
            {{ resource.description }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card title="切片列表" class="chunks-card" style="margin-top: 16px">
        <a-table
          :columns="chunkColumns"
          :data-source="chunks"
          :loading="loading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'chunkText'">
              <div class="chunk-text">{{ record.chunkText.substring(0, 200) }}...</div>
            </template>
            <template v-else-if="column.key === 'locationInfo'">
              <a-tag color="blue">{{ record.locationInfo }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-layout-content>

    <!-- 编辑资源 Modal -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑资源"
      @ok="handleEditSubmit"
      :confirm-loading="editSubmitting"
      @cancel="handleEditCancel"
    >
      <a-form
        :model="editFormState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="资源标题">
          <a-input
            v-model:value="editFormState.title"
            placeholder="请输入资源标题"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item label="标签">
          <a-input
            v-model:value="editFormState.tags"
            placeholder="多个标签用逗号分隔"
            :maxlength="500"
          />
        </a-form-item>

        <a-form-item label="描述">
          <a-textarea
            v-model:value="editFormState.description"
            placeholder="请输入资源描述"
            :rows="4"
            :maxlength="2000"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import ResourceService from '@/services/resource.service'
import type { Resource } from '@/services/resource.service'
import type { Chunk } from '@/services/resource.service'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const resource = ref<Resource>()
const chunks = ref<Chunk[]>([])

// 编辑相关状态
const editModalVisible = ref(false)
const editSubmitting = ref(false)
const editingResourceId = ref<number | null>(null)
const editFormState = ref({
  title: '',
  tags: '',
  description: ''
})

const chunkColumns = [
  {
    title: '序号',
    key: 'chunkIndex',
    dataIndex: 'chunkIndex',
    width: 80
  },
  {
    title: '文本内容',
    key: 'chunkText',
    dataIndex: 'chunkText'
  },
  {
    title: '位置信息',
    key: 'locationInfo',
    dataIndex: 'locationInfo',
    width: 200
  },
  {
    title: '创建时间',
    key: 'createdAt',
    dataIndex: 'createdAt',
    width: 180
  }
]

const loadResource = async () => {
  const id = Number(route.params.id)
  if (!id) {
    message.error('无效的资源ID')
    router.push('/resources')
    return
  }

  loading.value = true
  try {
    const data = await ResourceService.getResourceById(id)
    resource.value = data
  } catch (error: any) {
    message.error('加载资源失败: ' + (error.customMessage || error.message))
    router.push('/resources')
  } finally {
    loading.value = false
  }
}

const loadChunks = async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const data = await ResourceService.getChunksByResourceId(id)
    chunks.value = data
  } catch (error: any) {
    message.error('加载切片失败: ' + (error.customMessage || error.message))
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/resources')
}

const handleDelete = () => {
  if (!resource.value) return

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除资源"${resource.value.title}"吗？删除后无法恢复。`,
    icon: h(ExclamationCircleOutlined),
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await ResourceService.deleteResource(resource.value!.id)
        message.success('资源已删除')
        router.push('/resources')
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败，请重试')
      }
    }
  })
}

const handleEdit = () => {
  if (!resource.value) return

  editingResourceId.value = resource.value.id
  editFormState.value = {
    title: resource.value.title,
    tags: resource.value.tags || '',
    description: resource.value.description || ''
  }
  editModalVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editingResourceId.value) return

  editSubmitting.value = true
  try {
    await ResourceService.updateResource(editingResourceId.value, {
      title: editFormState.value.title,
      tags: editFormState.value.tags || undefined,
      description: editFormState.value.description || undefined
    })

    message.success('资源信息已更新')
    editModalVisible.value = false

    // 重新加载资源详情
    await loadResource()
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败，请重试')
  } finally {
    editSubmitting.value = false
  }
}

const handleEditCancel = () => {
  editModalVisible.value = false
  editFormState.value = {
    title: '',
    tags: '',
    description: ''
  }
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
  loadResource()
  loadChunks()
})
</script>

<style scoped>
.resource-detail-container {
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

.chunk-text {
  max-width: 600px;
  word-break: break-all;
}
</style>
