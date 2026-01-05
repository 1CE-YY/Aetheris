/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import AuthService, { type LoginRequest, type RegisterRequest, type UserInfo } from '@/services/auth.service'

/**
 * 用户状态管理 Store
 *
 * <p>使用 Pinia 管理用户登录状态和用户信息，包括：
 * <ul>
 *   <li>用户登录状态</li>
 *   <li>用户信息</li>
 *   <li>登录/登出操作</li>
 *   <li>注册操作</li>
 * </ul>
 */
export const useUserStore = defineStore('user', () => {
  // ========== 状态 ==========
  /**
   * 用户信息
   */
  const userInfo = ref<UserInfo | null>(null)

  /**
   * Token
   */
  const token = ref<string | null>(null)

  /**
   * 加载状态
   */
  const loading = ref<boolean>(false)

  /**
   * 正在验证 Token
   */
  const validatingToken = ref<boolean>(false)

  // ========== 计算属性 ==========
  /**
   * 是否已登录
   * <p>需要 token 存在且用户信息存在
   */
  const isLoggedIn = computed(() => {
    return !!token.value && !!userInfo.value
  })

  /**
   * 用户名
   */
  const username = computed(() => {
    return userInfo.value?.username ?? ''
  })

  /**
   * 邮箱
   */
  const email = computed(() => {
    return userInfo.value?.email ?? ''
  })

  // ========== 操作方法 ==========

  /**
   * 验证 Token 有效性
   *
   * <p>调用后端 /api/auth/me 接口验证 token 并获取用户信息
   *
   * @returns 验证是否成功
   */
  async function validateToken(): Promise<boolean> {
    // 防止重复验证
    if (validatingToken.value) {
      return false
    }

    const savedToken = AuthService.getToken()
    if (!savedToken) {
      return false
    }

    validatingToken.value = true

    try {
      const user = await AuthService.getCurrentUser()

      // 验证成功，保存用户信息
      token.value = savedToken
      userInfo.value = user
      localStorage.setItem('userInfo', JSON.stringify(user))

      return true
    } catch (error: any) {
      // Token 无效，清除本地状态
      console.error('Token 验证失败:', error)
      token.value = null
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')

      return false
    } finally {
      validatingToken.value = false
    }
  }

  /**
   * 初始化用户状态
   *
   * <p>从 localStorage 恢复 token，并调用 /api/auth/me 验证 token 有效性
   */
  async function initialize() {
    const savedToken = AuthService.getToken()
    if (savedToken) {
      token.value = savedToken
      // 验证 token 有效性
      await validateToken()
    }
  }

  /**
   * 用户登录
   *
   * @param data 登录请求
   * @returns 登录是否成功
   * @throws {Error} 如果登录失败，包含具体的错误信息
   */
  async function login(data: LoginRequest): Promise<boolean> {
    loading.value = true

    try {
      const response = await AuthService.login(data)

      // 保存 token 和用户信息
      token.value = response.token
      userInfo.value = response.user

      // 持久化用户信息到 localStorage
      localStorage.setItem('userInfo', JSON.stringify(response.user))

      return true
    } catch (error: any) {
      console.error('登录失败:', error)

      // 如果有自定义错误消息，抛出让组件处理
      if (error.customMessage) {
        throw new Error(error.customMessage)
      }

      // 否则抛出通用错误
      throw new Error('登录失败，请检查邮箱和密码')
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户注册
   *
   * @param data 注册请求
   * @returns 注册是否成功
   * @throws {Error} 如果注册失败，包含具体的错误信息
   */
  async function register(data: RegisterRequest): Promise<boolean> {
    loading.value = true

    try {
      const response = await AuthService.register(data)

      // 注册成功后，可以自动登录或跳转到登录页
      // 这里选择跳转到登录页，让用户手动登录
      return true
    } catch (error: any) {
      console.error('注册失败:', error)

      // 如果是 409 错误（邮箱已注册等），已经由拦截器处理并显示了
      // 这里抛出通用错误让组件也能显示
      throw new Error('注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户登出
   *
   * <p>清除本地状态和 localStorage 中的 token 和用户信息
   */
  function logout() {
    AuthService.logout()
    token.value = null
    userInfo.value = null
    localStorage.removeItem('userInfo')
  }

  /**
   * 更新用户信息
   *
   * @param newUserInfo 新的用户信息
   */
  function updateUserInfo(newUserInfo: UserInfo) {
    userInfo.value = newUserInfo
    localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
  }

  // ========== 返回 ==========
  return {
    // 状态
    userInfo,
    token,
    loading,
    validatingToken,

    // 计算属性
    isLoggedIn,
    username,
    email,

    // 操作方法
    initialize,
    validateToken,
    login,
    register,
    logout,
    updateUserInfo
  }
})
