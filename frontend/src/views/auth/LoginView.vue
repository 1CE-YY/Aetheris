<!--
  Copyright 2025 Aetheris RAG Team. All rights reserved.
-->
<template>
  <div class="login-container">
    <a-card class="login-card" title="用户登录">
      <a-form
        :model="formState"
        name="login"
        :rules="rules"
        @finish="handleLogin"
        autocomplete="off"
      >
        <!-- 邮箱输入框 -->
        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
            size="large"
          >
            <template #prefix>
              <UserOutlined />
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

        <!-- 登录按钮 -->
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="userStore.loading"
          >
            登录
          </a-button>
        </a-form-item>

        <!-- 注册链接 -->
        <a-form-item>
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
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
  email: string
  password: string
}

const formState = reactive<FormState>({
  email: '',
  password: ''
})

/**
 * 表单验证规则
 */
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码长度必须在 8-100 个字符之间', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 */
async function handleLogin() {
  try {
    console.log('[Login] 开始登录流程...')
    const response = await userStore.login({
      email: formState.email,
      password: formState.password
    })

    console.log('[Login] userStore.login 返回:', response)
    console.log('[Login] token:', userStore.token)
    console.log('[Login] userInfo:', userStore.userInfo)
    console.log('[Login] tokenValidated:', userStore.tokenValidated)
    console.log('[Login] isLoggedIn:', userStore.isLoggedIn)

    // 跳转到首页或之前的页面
    const redirect = router.currentRoute.value.query.redirect as string
    const targetUrl = redirect || '/'

    console.log('[Login] 准备跳转到:', targetUrl)

    // 使用 router.push 而不是 window.location.href，避免页面刷新导致状态丢失
    router.push(targetUrl)
  } catch (error: any) {
    console.error('[Login] 登录失败:', error)
    // 显示具体的错误信息
    message.error(error.message || '登录失败，请稍后重试')
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}
</style>
