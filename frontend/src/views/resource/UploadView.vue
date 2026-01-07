<template>
  <div class="upload-view">
    <a-layout-header class="header">
      <div class="header-content">
        <div class="logo">Aetheris RAG</div>
        <div class="user-info">
          <span class="username">欢迎，{{ userStore.username }}</span>
          <a-button type="link" @click="handleLogout">退出登录</a-button>
        </div>
      </div>
    </a-layout-header>

    <a-layout-content class="content">
      <a-page-header
        title="上传资源"
        @back="() => router.push('/resources')"
        class="page-header"
      />

      <a-card :bordered="false" class="upload-card">
      <a-form
        :model="formState"
        name="resourceUpload"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        @finish="handleSubmit"
      >
        <!-- 文件选择 -->
        <a-form-item label="选择文件">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
            :max-count="1"
            accept=".md,.markdown,.pdf"
            @remove="handleRemove"
          >
            <a-button :icon="h(UploadOutlined)">选择文件</a-button>
            <template #tip>
              <div class="upload-tip">
                支持 Markdown (.md) 和 PDF (.pdf) 文件，最大 50MB
              </div>
            </template>
          </a-upload>
        </a-form-item>

        <!-- 标题 -->
        <a-form-item label="资源标题" name="title" :rules="[{ required: true, message: '请输入标题' }]">
          <a-input
            v-model:value="formState.title"
            placeholder="请输入资源标题"
            :maxlength="100"
            show-count
          />
        </a-form-item>

        <!-- 标签 -->
        <a-form-item label="标签" name="tags">
          <a-input
            v-model:value="formState.tags"
            placeholder="多个标签用逗号分隔，如：机器学习,深度学习"
            :maxlength="100"
          />
        </a-form-item>

        <!-- 描述 -->
        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入资源描述（可选）"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <!-- 提交按钮 -->
        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-space>
            <a-button type="primary" html-type="submit" :loading="uploading">
              <template #icon><UploadOutlined /></template>
              上传资源
            </a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 上传结果 -->
    <a-card v-if="uploadedResource" title="上传成功" :bordered="false" class="result-card">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="资源ID">{{ uploadedResource.id }}</a-descriptions-item>
        <a-descriptions-item label="文件类型">{{ uploadedResource.fileType }}</a-descriptions-item>
        <a-descriptions-item label="文件大小">{{ formatFileSize(uploadedResource.fileSize) }}</a-descriptions-item>
        <a-descriptions-item label="切片数量">{{ uploadedResource.chunkCount }}</a-descriptions-item>
        <a-descriptions-item label="向量化状态">
          <a-tag :color="uploadedResource.vectorized ? 'success' : 'processing'">
            {{ uploadedResource.vectorized ? '已向量化' : '处理中' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="上传时间">
          {{ formatTime(uploadedResource.uploadTime) }}
        </a-descriptions-item>
      </a-descriptions>
      <a-button type="primary" style="margin-top: 16px" @click="goToDetail(uploadedResource.id)">
        查看详情
      </a-button>
    </a-card>
    </a-layout-content>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import ResourceService, { type ResourceUploadRequest, type Resource } from '@/services/resource.service'
import api from '@/services/api'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

// 表单状态
const formState = reactive<ResourceUploadRequest>({
  filePath: '',
  title: '',
  tags: '',
  description: ''
})

// 文件列表
const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadedResource = ref<Resource | null>(null)

// 保存上传时的表单数据（用于重复上传时覆盖）
const uploadFormData = ref<{
  file: File
  title: string
  tags: string
  description: string
} | null>(null)

// 文件上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  // 文件大小检查（0 字节 = 空文件）
  if (file.size === 0) {
    message.error('文件为空，请选择非空文件')
    return false
  }

  // 文件类型校验
  const isValidType = file.name.endsWith('.md') ||
                      file.name.endsWith('.markdown') ||
                      file.name.endsWith('.pdf')
  if (!isValidType) {
    message.error('只支持 Markdown 和 PDF 文件')
    return false
  }

  // 文件大小校验
  const isValidSize = file.size <= 50 * 1024 * 1024 // 50MB
  if (!isValidSize) {
    message.error('文件大小不能超过 50MB')
    return false
  }

  // 自动填充标题（如果为空）
  if (!formState.title) {
    formState.title = file.name.replace(/\.(md|markdown|pdf)$/, '')
  }

  return false // 阻止自动上传，我们将手动上传
}

// 移除文件
const handleRemove = () => {
  // 文件移除时，如果标题是从文件名自动生成的，则清空标题
  if (fileList.value[0] && formState.title === fileList.value[0].name.replace(/\.(md|markdown|pdf)$/, '')) {
    formState.title = ''
  }
}

// 提交上传
const handleSubmit = async (values: ResourceUploadRequest) => {
  // 1. 检查是否选择了文件
  if (fileList.value.length === 0) {
    message.error('请先选择文件')
    return
  }

  const file = fileList.value[0].originFileObj

  // 2. 再次检查文件大小（防止空文件绕过 beforeUpload）
  if (!file || file.size === 0) {
    message.error('文件为空，请选择非空文件')
    fileList.value = []  // 清空文件列表
    return
  }

  // 3. 检查文件类型
  const fileName = file.name
  if (!fileName.endsWith('.md') && !fileName.endsWith('.markdown') && !fileName.endsWith('.pdf')) {
    message.error('只支持 Markdown 和 PDF 文件')
    fileList.value = []
    return
  }

  // 4. 检查文件大小
  if (file.size > 50 * 1024 * 1024) {
    message.error('文件大小不能超过 50MB')
    fileList.value = []
    return
  }

  uploading.value = true
  try {

    // 创建 FormData
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', values.title)
    formData.append('tags', values.tags || '')
    formData.append('description', values.description || '')

    // 保存表单数据（用于可能的覆盖操作）
    uploadFormData.value = {
      file,
      title: values.title,
      tags: values.tags || '',
      description: values.description || ''
    }

    // 发送请求（使用 api 实例，会自动添加 Authorization 头）
    const response = await api.post<any, Resource>('/resources', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    // 检查是否为重复上传
    if (response.duplicate) {
      // 弹出询问对话框
      Modal.confirm({
        title: '文件已存在',
        content: `该文件已上传，是否覆盖为新的标题、标签和描述？`,
        okText: '覆盖',
        cancelText: '保留原有',
        onOk: async () => {
          // 用户选择覆盖
          try {
            await ResourceService.updateResource(response.id, {
              title: values.title,
              tags: values.tags || '',
              description: values.description || ''
            })
            message.success('资源信息已更新！')
            uploadedResource.value = { ...response, title: values.title, tags: values.tags, description: values.description }
            handleReset()
          } catch (error: any) {
            message.error(error.response?.data?.message || '更新失败，请重试')
          }
        },
        onCancel: () => {
          // 用户选择保留原有，显示原有资源信息
          message.info('已保留原有资源信息')
          uploadedResource.value = response
          handleReset()
        }
      })
    } else {
      // 新上传成功
      uploadedResource.value = response
      message.success('资源上传成功！')
      handleReset()
    }
  } catch (error: any) {
    const errorMsg = error.response?.data?.message || error.message || '上传失败，请重试'

    // 针对常见错误提供友好提示
    if (errorMsg.includes('Missing root object') || errorMsg.includes('PDF') && errorMsg.includes('解析')) {
      message.error('PDF 文件已损坏或不完整，请重新生成 PDF 文件')
    } else if (errorMsg.includes('文件为空')) {
      message.error('文件为空，请选择非空文件')
    } else {
      message.error('上传失败: ' + errorMsg)
    }
  } finally {
    uploading.value = false
  }
}

// 重置表单
const handleReset = () => {
  formState.title = ''
  formState.tags = ''
  formState.description = ''
  fileList.value = []
  uploadedResource.value = null
}

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 格式化时间
const formatTime = (timeStr: string): string => {
  return new Date(timeStr).toLocaleString('zh-CN')
}

// 跳转到详情页
const goToDetail = (id: number) => {
  router.push({ name: 'ResourceDetail', params: { id } })
}
</script>

<style scoped>
.upload-view {
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
}

.page-header {
  background: #fff;
  border-radius: 4px;
  margin-bottom: 16px;
}

.upload-card {
  background: #fff;
}

.result-card {
  background: #fff;
  margin-top: 16px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
</style>
