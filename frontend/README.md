# Aetheris RAG Frontend

Vue 3 + TypeScript 前端应用，提供用户界面和 API 集成。

---

## 📋 技术栈

- **Vue** 3.3.8（Composition API）
- **TypeScript** 5.x
- **Vite** 5.x（构建工具）
- **Ant Design Vue** 4.x（UI 组件库）
- **Vue Router** 4.2.5（路由）
- **Pinia** 2.1.7（状态管理）
- **Axios** 1.6.2（HTTP 客户端）
- **Day.js**（日期处理）

---

## 🚀 快速开始

### 1. 安装依赖

```bash
pnpm install
```

或使用 npm：

```bash
npm install
```

### 2. 启动开发服务器

```bash
pnpm dev
```

访问：http://localhost:5173

### 3. 构建生产版本

```bash
pnpm build
```

输出目录：`dist/`

---

## 📂 项目结构

```
frontend/src/
├── api/             # API 调用封装
├── assets/          # 静态资源
│   └── vue.svg
├── components/      # Vue 组件
├── views/           # 页面组件
│   ├── auth/        # 认证相关页面
│   ├── chat/        # RAG 问答页面
│   ├── profile/     # 用户画像页面
│   ├── recommendation/  # 推荐页面
│   └── resource/    # 资源管理页面
├── router/          # 路由配置
├── stores/          # Pinia 状态管理
├── types/           # TypeScript 类型定义
├── utils/           # 工具函数
├── App.vue          # 根组件
└── main.ts          # 应用入口
```

---

## 🔧 配置文件

### vite.config.ts

Vite 配置，包含：

- **端口**: 5173
- **API 代理**: `/api` → `http://localhost:8080`
- **路径别名**: `@` → `./src`
- **构建优化**: Terser 压缩

### tsconfig.json

TypeScript 配置，包含：

- **严格模式**: 启用
- **路径别名**: `@/*` → `./src/*`
- **模块解析**: Node Next

### .env.development

开发环境变量：

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_API_TIMEOUT=30000
VITE_APP_TITLE=Aetheris RAG System
VITE_APP_VERSION=1.0.0
```

---

## 🎨 组件库

### Ant Design Vue

已集成 Ant Design Vue 4.x，提供：

- **Button** - 按钮
- **Form** - 表单
- **Input** - 输入框
- **Table** - 表格
- **Modal** - 模态框
- **Message** - 消息提示
- **Spin** - 加载动画
- 等等...

使用示例：

```vue
<template>
  <a-button type="primary" @click="handleClick">
    点击我
  </a-button>
</template>

<script setup lang="ts">
const handleClick = () => {
  message.success('按钮已点击');
};
</script>
```

---

## 🛣️ 路由

### 路由配置

路由定义在 `router/index.ts`：

```typescript
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue')
  },
  // ...
];
```

### 路由导航

```typescript
import { useRouter } from 'vue-router';

const router = useRouter();

router.push({ name: 'Login' });
router.push({ path: '/auth/login' });
```

---

## 🗃️ 状态管理

### Pinia Stores

Store 定义在 `stores/` 目录：

```typescript
// stores/user.ts
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: null
  }),
  actions: {
    setUser(user: any) {
      this.user = user;
    },
    setToken(token: string) {
      this.token = token;
    }
  }
});
```

### 使用 Store

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

console.log(userStore.user);
userStore.setUser({ name: 'Alice' });
</script>
```

---

## 📡 API 集成

### Axios 配置

API 客户端配置在 `api/` 目录：

```typescript
// api/request.ts
import axios from 'axios';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 错误处理
    return Promise.reject(error);
  }
);
```

### API 调用示例

```typescript
// api/auth.ts
import request from './request';

export const login = (data: LoginRequest) => {
  return request.post('/api/auth/login', data);
};

export const register = (data: RegisterRequest) => {
  return request.post('/api/auth/register', data);
};
```

---

## 🧪 测试

### 单元测试

```bash
pnpm test
```

### E2E 测试

```bash
pnpm test:e2e
```

---

## 📝 代码规范

### 命名规范

- **组件名**: PascalCase（如 `UserList.vue`）
- **文件名**: PascalCase（组件）或 kebab-case（工具）
- **变量/函数**: camelCase（如 `userName`、`getUserData`）
- **常量**: UPPER_SNAKE_CASE（如 `API_BASE_URL`）
- **类型/接口**: PascalCase（如 `User`、`LoginRequest`）

### Vue 组件规范

使用 `<script setup>` 语法：

```vue
<template>
  <div class="user-list">
    <h1>{{ title }}</h1>
    <UserCard v-for="user in users" :key="user.id" :user="user" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import UserCard from '@/components/UserCard.vue';
import type { User } from '@/types/user';

interface Props {
  title: string;
}

const props = defineProps<Props>();
const users = ref<User[]>([]);

onMounted(async () => {
  users.value = await fetchUsers();
});
</script>

<style scoped>
.user-list {
  padding: 20px;
}
</style>
```

### TypeScript 类型

```typescript
// types/user.ts
export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
  lastActiveAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
```

---

## 🎯 页面开发指南

### Phase 1-2 状态

**已实现**：
- ✅ 项目脚手架搭建
- ✅ Vite + Vue 3 + TypeScript 配置
- ✅ Ant Design Vue 集成
- ✅ 路由和状态管理配置
- ✅ API 客户端封装

**待实现**（Phase 3-5）：
- ⏳ 认证页面（登录/注册）
- ⏳ 资源管理页面（上传、列表、详情）
- ⏳ RAG 问答页面
- ⏳ 推荐系统页面
- ⏳ 用户画像页面

### 新建页面

1. 在 `views/` 目录创建组件：

```bash
touch src/views/NewPage.vue
```

2. 编写组件代码：

```vue
<template>
  <div class="new-page">
    <h1>New Page</h1>
  </div>
</template>

<script setup lang="ts">
// 组件逻辑
</script>

<style scoped>
.new-page {
  /* 样式 */
}
</style>
```

3. 添加路由：

```typescript
// router/index.ts
{
  path: '/new',
  name: 'NewPage',
  component: () => import('@/views/NewPage.vue')
}
```

4. 导航到页面：

```vue
<router-link :to="{ name: 'NewPage' }">Go to New Page</router-link>
```

---

## 🐛 常见问题

### Q: Vite 启动失败，提示端口被占用？

**A**: 杀死占用 5173 端口的进程：

```bash
lsof -i :5173
kill -9 <PID>
```

### Q: API 请求跨域错误？

**A**: Vite 已配置代理，确保后端运行在 http://localhost:8080

### Q: TypeScript 类型错误？

**A**: 运行 `pnpm type-check` 查看详细错误信息

---

## 📚 相关文档

- [Vue 3 文档](https://vuejs.org/)
- [Vite 文档](https://vitejs.dev/)
- [Ant Design Vue 文档](https://antdv.com/)
- [Pinia 文档](https://pinia.vuejs.org/)
- [Vue Router 文档](https://router.vuejs.org/)

---

## 📮 联系方式

- 问题反馈：提交 GitHub Issue
- 技术支持：1307792296@qq.com

---

**最后更新**: 2025-12-29
**版本**: v1.0.0
