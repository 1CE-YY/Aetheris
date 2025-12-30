/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

/**
 * 创建 Axios 实例
 *
 * <p>配置基础 URL、超时时间、默认请求头等
 */
const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器
 *
 * <p>在发送请求之前，自动添加 Authorization 头（如果 token 存在）
 */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 *
 * <p>统一处理错误响应，包括：
 * <ul>
 *   <li>401 未认证：清除 token，智能跳转（登录页不跳转）</li>
 *   <li>403 无权限：显示错误提示</li>
 *   <li>404 资源不存在：显示错误提示</li>
 *   <li>500 服务器错误：显示错误提示</li>
 *   <li>网络错误：显示错误提示</li>
 * </ul>
 */
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const requestUrl = error.config?.url || ''

      switch (status) {
        case 401:
          // 401 错误处理
          localStorage.removeItem('token')

          // 如果在登录页，不跳转，只抛出错误让组件显示
          if (window.location.pathname === '/login') {
            // 使用后端返回的错误信息
            const errorMsg = data?.message || '邮箱或密码错误'
            return Promise.reject({ ...error, customMessage: errorMsg })
          }

          // 在其他页面，显示提示并跳转到登录页
          // 但排除 /api/auth/me，让 validateToken() 静默处理
          if (requestUrl !== '/auth/me') {
            message.error('登录已过期，请重新登录')
          }
          window.location.href = '/login'
          break

        case 403:
          // 403 错误处理
          // 如果是 /api/auth/me，静默处理（token 验证失败）
          // 其他情况显示错误
          if (requestUrl !== '/auth/me') {
            message.error(data?.message || '无权限访问')
          }
          break

        case 404:
          message.error(data?.message || '请求的资源不存在')
          break

        case 409:
          // 409 冲突（如邮箱已注册）- 使用后端的具体错误信息
          const conflictMsg = data?.message || '请求失败，请稍后重试'
          message.error(conflictMsg)
          break

        case 500:
          message.error(data?.message || '服务器错误，请稍后重试')
          break

        default:
          // 其他错误，优先使用后端返回的 message
          const defaultMsg = data?.message || data?.detail || '请求失败，请稍后重试'
          message.error(defaultMsg)
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接')
    } else {
      message.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

/**
 * 默认导出 Axios 实例
 */
export default api
