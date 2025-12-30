/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import api from './api'

/**
 * 登录请求接口
 */
export interface LoginRequest {
  email: string // 邮箱
  password: string
}

/**
 * 注册请求接口
 */
export interface RegisterRequest {
  username: string
  email: string
  password: string
}

/**
 * 用户信息接口
 */
export interface UserInfo {
  id: number
  username: string
  email: string
  createdAt: string
  lastActiveAt: string
}

/**
 * 登录响应接口
 */
export interface LoginResponse {
  token: string
  user: UserInfo
}

/**
 * 注册响应接口
 */
export interface RegisterResponse {
  id: number
  username: string
  email: string
  createdAt: string
  lastActiveAt: string
}

/**
 * 认证服务类
 *
 * <p>封装用户认证相关的 API 调用，包括：
 * <ul>
 *   <li>用户登录</li>
 *   <li>用户注册</li>
 *   <li>Token 管理</li>
 * </ul>
 */
export class AuthService {
  /**
   * 用户登录
   *
   * @param data 登录请求（邮箱/用户名 + 密码）
   * @returns 登录响应（包含 JWT token 和用户信息）
   */
  static async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<any, LoginResponse>('/auth/login', data)

    // 保存 token 到 localStorage
    if (response.token) {
      localStorage.setItem('token', response.token)
    }

    return response
  }

  /**
   * 用户注册
   *
   * @param data 注册请求（用户名、邮箱、密码）
   * @returns 注册响应（包含用户信息）
   */
  static async register(data: RegisterRequest): Promise<RegisterResponse> {
    const response = await api.post<any, RegisterResponse>('/auth/register', data)

    // 注册成功后自动登录，保存 token
    // 注意：如果后端注册接口也返回 token，则保存
    // 这里假设注册后需要单独登录
    return response
  }

  /**
   * 用户登出
   *
   * <p>清除本地存储的 token
   */
  static logout(): void {
    localStorage.removeItem('token')
  }

  /**
   * 获取当前存储的 token
   *
   * @returns JWT token 字符串，如果不存在则返回 null
   */
  static getToken(): string | null {
    return localStorage.getItem('token')
  }

  /**
   * 获取当前登录用户的信息
   *
   * <p>调用后端 /api/auth/me 接口验证 token 并获取用户信息
   *
   * @returns 用户信息
   * @throws Error 如果 token 无效或用户不存在
   */
  static async getCurrentUser(): Promise<UserInfo> {
    const response = await api.get<any, UserInfo>('/auth/me')
    return response
  }

  /**
   * 检查用户是否已登录
   *
   * @returns 如果 token 存在且不为空则返回 true，否则返回 false
   */
  static isAuthenticated(): boolean {
    const token = this.getToken()
    return token !== null && token !== ''
  }

  /**
   * 获取认证头信息
   *
   * <p>用于 Axios 请求拦截器中添加 Authorization 头
   *
   * @returns Authorization 头值（格式：Bearer {token}），如果 token 不存在则返回空字符串
   */
  static getAuthHeader(): string {
    const token = this.getToken()
    return token ? `Bearer ${token}` : ''
  }
}

/**
 * 默认导出 AuthService 实例
 */
export default AuthService
