/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */

import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由配置
 *
 * <p>定义应用的所有路由，包括：
 * <ul>
 *   <li>公开路由：登录页、注册页</li>
 *   <li>受保护路由：首页、资源管理、问答、推荐、个人中心等</li>
 * </ul>
 *
 * <p>使用路由守卫保护需要认证的页面
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/resources',
    name: 'ResourceList',
    component: () => import('@/views/resource/ResourceListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/resources/upload',
    name: 'ResourceUpload',
    component: () => import('@/views/resource/UploadView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/resources/:id',
    name: 'ResourceDetail',
    component: () => import('@/views/resource/ResourceDetailView.vue'),
    meta: { requiresAuth: true }
  },
  // {
  //   path: '/chat',
  //   name: 'Chat',
  //   component: () => import('@/views/chat/ChatView.vue'),
  //   meta: { requiresAuth: true }
  // },
  // {
  //   path: '/recommendations',
  //   name: 'Recommendations',
  //   component: () => import('@/views/recommendation/RecommendationView.vue'),
  //   meta: { requiresAuth: true }
  // },
  // {
  //   path: '/profile',
  //   name: 'Profile',
  //   component: () => import('@/views/profile/ProfileView.vue'),
  //   meta: { requiresAuth: true }
  // },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { requiresAuth: false }
  }
]

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

/**
 * 全局前置守卫
 *
 * <p>用于保护需要认证的路由：
 * <ul>
 *   <li>如果用户未登录且访问受保护的路由，则重定向到登录页</li>
 *   <li>如果用户已登录且访问登录/注册页，则重定向到首页</li>
 * </ul>
 *
 * <p>在每次导航前，等待 token 验证完成后再进行认证检查
 */
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  console.log('[Router] 导航到:', to.path)
  console.log('[Router] validatingToken:', userStore.validatingToken)
  console.log('[Router] isLoggedIn:', userStore.isLoggedIn)
  console.log('[Router] token:', userStore.token?.substring(0, 20))

  // 如果正在验证 token，等待验证完成
  if (userStore.validatingToken) {
    console.log('[Router] 正在验证 token，等待...')
    // 使用 $subscribe 等待验证完成
    const unwatch = userStore.$subscribe((_mutation, state) => {
      if (!state.validatingToken) {
        unwatch()
        // 验证完成，使用 router.replace 重新导航
        console.log('[Router] Token 验证完成，重新导航')
        router.replace(to.fullPath)
      }
    })
    // 阻止当前导航，等待验证完成后重新导航
    next(false)
    return
  }

  // 检查路由是否需要认证
  const requiresAuth = to.meta.requiresAuth !== false

  console.log('[Router] requiresAuth:', requiresAuth)

  if (requiresAuth && !userStore.isLoggedIn) {
    // 需要认证但用户未登录，重定向到登录页
    console.log('[Router] 需要认证但未登录，重定向到登录页')
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
  } else if (!requiresAuth && userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
    // 用户已登录但访问登录/注册页，重定向到首页
    console.log('[Router] 已登录但访问登录页，重定向到首页')
    next({ name: 'Home' })
  } else {
    // 其他情况，正常导航
    console.log('[Router] 正常导航')
    next()
  }
})

/**
 * 全局后置钩子
 *
 * <p>用于设置页面标题等操作
 */
router.afterEach((to) => {
  // 设置页面标题
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} - Aetheris RAG` : 'Aetheris RAG'

  // 滚动到页面顶部
  window.scrollTo(0, 0)
})

/**
 * 默认导出路由实例
 */
export default router
