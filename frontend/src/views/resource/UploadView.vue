<template>
  <div class="upload-view">
    <a-card title="上传学习资源" :bordered="false">
      <a-form
        :model="formState"
        name="resourceUpload"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
        @finish="handleSubmit"
      >
        <!-- 文件选择 -->
        <a-form-item label="选择文件" name="file" :rules="[{ required: true, message: '请选择文件' }]">
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
    <a-card v-if="uploadedResource" title="上传成功" :bordered="false" style="margin-top: 16px">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import ResourceService, { type ResourceUploadRequest, type Resource } from '@/services/resource.service'

const router = useRouter()

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

// 文件上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = file.name.endsWith('.md') ||
                      file.name.endsWith('.markdown') ||
                      file.name.endsWith('.pdf')
  if (!isValidType) {
    message.error('只支持 Markdown 和 PDF 文件')
    return false
  }

  const isValidSize = file.size <= 50 * 1024 * 1024 // 50MB
  if (!isValidSize) {
    message.error('文件大小不能超过 50MB')
    return false
  }

  // 保存文件路径（实际项目中这里应该上传到服务器）
  formState.filePath = file.name
  formState.title = formState.title || file.name.replace(/\.(md|markdown|pdf)$/, '')

  return false // 阻止自动上传
}

// 移除文件
const handleRemove = () => {
  formState.filePath = ''
  if (!formState.title) {
    formState.title = ''
  }
}

// 提交上传
const handleSubmit = async (values: ResourceUploadRequest) => {
  if (!formState.filePath) {
    message.error('请先选择文件')
    return
  }

  uploading.value = true
  try {
    // 注意：实际项目中需要先上传文件到服务器获取 filePath
    // 这里使用模拟路径
    const mockFilePath = `/path/to/uploads/${formState.filePath}`

    const resource = await ResourceService.upload({
      ...values,
      filePath: mockFilePath
    })

    uploadedResource.value = resource
    message.success('资源上传成功！')

    // 重置表单
    handleReset()
  } catch (error: any) {
    message.error(error.response?.data?.message || '上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 重置表单
const handleReset = () => {
  formState.filePath = ''
  formState.title = ''
  formState.tags = ''
  formState.description = ''
  fileList.value = []
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
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
</style>
