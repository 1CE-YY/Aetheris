<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->
<template>
  <div class="register-container">
    <a-card class="register-card" title="用户注册">
      <a-form
        :model="formState"
        name="register"
        :rules="rules"
        @finish="handleRegister"
        autocomplete="off"
      >
        <!-- 用户名输入框 -->
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="formState.username"
            placeholder="请输入用户名"
            size="large"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <!-- 邮箱输入框 -->
        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
            size="large"
          >
            <template #prefix>
              <MailOutlined />
            </template>
          </a-input>
        </a-form-item>

        <!-- 密码输入框 -->
        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <!-- 确认密码输入框 -->
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="请再次输入密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <!-- 注册按钮 -->
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="userStore.loading"
          >
            注册
          </a-button>
        </a-form-item>

        <!-- 登录链接 -->
        <a-form-item>
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

/**
 * 路由实例
 */
const router = useRouter()

/**
 * 用户状态管理
 */
const userStore = useUserStore()

/**
 * 表单状态
 */
interface FormState {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const formState = reactive<FormState>({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

/**
 * 表单验证规则
 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在 3-50 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
    { max: 100, message: '邮箱长度不能超过 100 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码长度必须在 8-100 个字符之间', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码必须包含至少一个字母和一个数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (value !== formState.password) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 处理注册
 */
async function handleRegister() {
  try {
    await userStore.register({
      username: formState.username,
      email: formState.email,
      password: formState.password
    })

    message.success('注册成功，请登录')

    // 跳转到登录页
    router.push('/login')
  } catch (error: any) {
    // 错误已经由拦截器处理并显示了
    // 这里可以添加额外的处理逻辑（如果有）
    console.error('注册失败:', error)
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}
</style>
