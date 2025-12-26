# 快速开始指南

**功能**：学习资源检索与推荐 RAG 系统
**分支**：`001-rag-recommendation-system`
**最后更新**：2025-12-26

## 概述

本指南将帮助您在本地快速搭建和运行 Aetheris RAG 系统的开发环境。系统采用前后端分离架构：
- **后端**：Spring Boot 3.5 + Java 21 + MyBatis + Redis Stack + MySQL 8
- **前端**：Vue 3 + TypeScript + Ant Design Vue 4.x + Vite 5.x
- **基础设施**：Docker Compose（MySQL + Redis Stack）

## 前置要求

### 必需软件

1. **Java 21+**
   ```bash
   java -version  # openjdk 21.0.1 或更高版本
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   ```

3. **Docker & Docker Compose**
   ```bash
   docker --version      # Docker 20.10+
   docker-compose --version  # Docker Compose 2.0+
   ```

4. **Node.js 18+**（前端开发）
   ```bash
   node --version  # v18.0.0 或更高版本
   npm --version
   ```

5. **pnpm 8+**（推荐，前端包管理器）
   ```bash
   npm install -g pnpm
   pnpm --version
   ```

### 可选软件

- **IntelliJ IDEA 2024.1+**：推荐用于 Java 开发（社区版即可，完整支持 Java 21）
- **Visual Studio Code**：推荐用于前端开发（Vue 3 支持好）
- **Postman** 或 **curl**：用于 API 测试
- **RedisInsight**：Redis Stack 可视化工具（可选）

## 1. 启动基础设施（MySQL + Redis Stack）

### 1.1 创建 docker-compose.yml

在项目根目录创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: aetheris-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: aetheris_rag
      MYSQL_USER: aetheris
      MYSQL_PASSWORD: aetheris123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/db/migration:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password

  redis-stack:
    image: redis/redis-stack-server:latest
    container_name: aetheris-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data/redis

volumes:
  mysql_data:
  redis_data:
```

### 1.2 启动服务

```bash
# 启动 MySQL 和 Redis Stack
docker-compose up -d

# 查看日志
docker-compose logs -f

# 验证服务状态
docker-compose ps
```

### 1.3 验证连接

**MySQL**：
```bash
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123
mysql> SHOW DATABASES;
mysql> USE aetheris_rag;
mysql> SHOW TABLES;
```

**Redis Stack**：
```bash
docker exec -it aetheris-redis redis-cli
127.0.0.1:6379> PING
PONG
127.0.0.1:6379> MODULE LIST
# 应该看到 RediSearch、RedisJSON 等模块
```

## 2. 配置后端（Spring Boot）

### 2.1 创建配置文件

**backend/src/main/resources/application.yml**：

```yaml
spring:
  application:
    name: aetheris-rag

  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/aetheris_rag?useSSL=false&serverTimezone=Asia/Shanghai
    username: aetheris
    password: aetheris123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  # Flyway 数据库迁移
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.aetheris.rag.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# Redis Stack 配置
spring.redis:
  host: localhost
  port: 6379
  lettuce:
    pool:
      max-active: 8
      max-idle: 8
      min-idle: 0

# 应用配置
app:
  # 智谱 AI 配置（需要申请 API Key）
  zhipu-ai:
    api-key: ${ZHIPU_AI_API_KEY:your-api-key-here}
    embedding:
      model-name: embedding-v2
      base-url: https://open.bigmodel.cn/api/paas/v4/
      timeout: 30s
      max-tokens: 8192
    chat:
      model-name: glm-4-flash
      temperature: 0.7
      top-p: 0.9
      max-tokens: 2048
      timeout: 60s

  # RAG 配置
  rag:
    chunk:
      size: 1000        # 切片大小（字符数）
      overlap: 200      # 重叠字符数
    retrieval:
      top-k: 5          # 检索切片数量
      min-score: 0.5    # 最小相似度分数
    recommendation:
      top-n: 10         # 推荐数量
      window-size: 10   # 用户画像窗口（最近N次查询）
    cache:
      embedding-ttl: 30d      # Embedding 缓存 TTL
      search-ttl: 1h          # 检索结果缓存 TTL

# 日志配置
logging:
  level:
    root: INFO
    com.aetheris.rag: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /api
```

### 2.2 创建 pom.xml

**backend/pom.xml** 核心配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.9</version>
    <relativePath/>
  </parent>

  <groupId>com.aetheris</groupId>
  <artifactId>aetheris-rag</artifactId>
  <version>1.0.0</version>
  <name>Aetheris RAG System</name>
  <description>学习资源检索与推荐 RAG 系统</description>

  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <mybatis.version>3.5.16</mybatis.version>
    <mybatis-spring-boot.version>3.0.4</mybatis-spring-boot.version>
    <lombok.version>1.18.36</lombok.version>
    <guava.version>33.4.0</guava.version>
    <commons-lang3.version>3.17.0</commons-lang3.version>
    <langchain4j.version>0.37.2</langchain4j.version>
  </properties>

  <dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
      <groupId>org.mybatis</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>${mybatis-spring-boot.version}</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <version>${lombok.version}</version>
      <scope>provided</scope>
    </dependency>

    <!-- Redis Stack (Lettuce) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
      <groupId>io.lettuce</groupId>
      <artifactId>lettuce-core</artifactId>
      <version>6.5.2.RELEASE</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-mysql</artifactId>
    </dependency>

    <!-- LangChain4j -->
    <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-spring-boot-starter</artifactId>
      <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-zhipu-ai</artifactId>
      <version>${langchain4j.version}</version>
    </dependency>

    <!-- 工具类 -->
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>${guava.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-lang3</artifactId>
      <version>${commons-lang3.version}</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>mysql</artifactId>
      <version>1.20.4</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>21</source>
          <target>21</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### 2.3 智谱 AI API Key 申请

1. 访问 [智谱 AI 开放平台](https://open.bigmodel.cn/)
2. 注册并登录
3. 进入"API Key"页面，创建新的 API Key
4. 设置环境变量（推荐）：
   ```bash
   export ZHIPU_AI_API_KEY=your-actual-api-key
   ```

### 2.4 初始化数据库

Flyway 会自动执行迁移脚本 `backend/src/main/resources/db/migration/V1__init_schema.sql`。

如需手动初始化：
```bash
mvn flyway:migrate
```

### 2.5 启动后端

**方式一：使用 Maven**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**方式二：使用 IDE**
- 在 IntelliJ IDEA 中打开 `backend` 目录
- 确保项目使用 Java 21 SDK
- 运行 `AetherisRagApplication.java`

**验证启动**：
```bash
curl http://localhost:8080/api/actuator/health
```

应返回：
```json
{"status": "UP"}
```

**Java 21 虚拟线程配置（必须）**：

在 `application.yml` 中添加：
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 必须启用虚拟线程以提升并发性能
```

## 3. 配置前端（Vue 3 + Ant Design Vue）

### 3.1 安装依赖

使用 pnpm（推荐）：
```bash
cd frontend
pnpm install
```

或使用 npm：
```bash
cd frontend
npm install
```

### 3.2 package.json 核心依赖

**frontend/package.json** 应包含以下核心依赖：

```json
{
  "name": "aetheris-rag-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .vue,.js,.jsx,.cjs,.mjs,.ts,.tsx,.cts,.mts --fix --ignore-path .gitignore"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.5",
    "pinia": "^2.1.7",
    "ant-design-vue": "^4.1.0",
    "@ant-design/icons-vue": "^7.0.1",
    "axios": "^1.6.0",
    "dayjs": "^1.11.10",
    "lodash-es": "^4.17.21"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0",
    "vue-tsc": "^1.8.27",
    "typescript": "^5.3.0",
    "sass": "^1.69.0",
    "unplugin-vue-components": "^0.26.0",
    "unplugin-auto-import": "^0.17.3"
  }
}
```

### 3.3 配置 Vite

**frontend/vite.config.ts**：

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import AutoImport from 'unplugin-auto-import/vite'

export default defineConfig({
  plugins: [
    vue(),
    // Ant Design Vue 按需引入
    Components({
      resolvers: [
        AntDesignVueResolver({
          importStyle: false, // css in js
        })
      ]
    }),
    // API 自动导入（ref、reactive等）
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/styles/variables.scss" as *;`
      }
    }
  }
})
```

### 3.4 配置 TypeScript

**frontend/tsconfig.json**：

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### 3.5 主入口配置

**frontend/src/main.ts**：

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css'
import '@/assets/styles/main.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')
```

### 3.6 启动前端

```bash
# 方式一：使用 pnpm（推荐）
cd frontend
pnpm dev

# 方式二：使用 npm
cd frontend
npm run dev
```

访问：http://localhost:3000

### 3.7 前端目录结构（已创建）

```
frontend/
├── src/
│   ├── main.ts                     # 应用入口
│   ├── App.vue                     # 根组件
│   ├── router/                     # 路由配置
│   ├── stores/                     # Pinia 状态管理
│   ├── views/                      # 页面组件（auth、resource、chat、recommendation、profile）
│   ├── components/                 # 可复用组件（layout、resource、chat、recommendation、common）
│   ├── services/                   # API 服务层（auth、resource、chat、recommendation、behavior）
│   ├── composables/                # Composition API 可组合函数
│   ├── utils/                      # 工具函数（format、validation）
│   ├── types/                      # TypeScript 类型定义
│   └── assets/                     # 静态资源（styles、images）
├── public/
│   ├── favicon.ico
│   └── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── README.md
```

### 3.8 Ant Design Vue 组件使用示例

**使用 Card 组件**：

```vue
<template>
  <a-card title="资源详情" :bordered="false">
    <p>{{ resource.description }}</p>
    <template #extra>
      <a-tag color="blue">{{ resource.fileType }}</a-tag>
    </template>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const resource = ref({
  description: '深度学习基础教程',
  fileType: 'PDF'
})
</script>
```

**使用 Table 组件**：

```vue
<template>
  <a-table
    :columns="columns"
    :data-source="resources"
    :pagination="{ pageSize: 10 }"
    :loading="loading"
    @change="handleTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'tags'">
        <a-tag v-for="tag in record.tags" :key="tag" color="green">
          {{ tag }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a-button type="link" @click="viewDetail(record)">查看</a-button>
          <a-button type="link" @click="favorite(record)">收藏</a-button>
        </a-space>
      </template>
    </template>
  </a-table>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '标签', dataIndex: 'tags', key: 'tags' },
  { title: '操作', key: 'action' }
]

const resources = ref([])
const loading = ref(false)

const handleTableChange = (pagination: any) => {
  console.log('分页变化：', pagination)
}

const viewDetail = (record: any) => {
  console.log('查看详情：', record)
}

const favorite = (record: any) => {
  console.log('收藏：', record)
}
</script>
```

**使用 Upload 组件**：

```vue
<template>
  <a-upload
    :action="uploadUrl"
    :headers="uploadHeaders"
    :before-upload="beforeUpload"
    @change="handleUploadChange"
  >
    <a-button type="primary">
      <upload-outlined></upload-outlined>
      点击上传
    </a-button>
  </a-upload>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadOutlined } from '@ant-design/icons-vue'

const uploadUrl = ref('/api/resources')
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

const beforeUpload = (file: File) => {
  const isValidType = file.type === 'application/pdf' || file.name.endsWith('.md')
  if (!isValidType) {
    message.error('仅支持 PDF 或 Markdown 文件')
  }
  const isValidSize = file.size / 1024 / 1024 < 50
  if (!isValidSize) {
    message.error('文件大小不能超过 50MB')
  }
  return isValidType && isValidSize
}

const handleUploadChange = (info: any) => {
  if (info.file.status === 'done') {
    message.success('上传成功')
  } else if (info.file.status === 'error') {
    message.error('上传失败')
  }
}
</script>
```

### 3.9 API 服务层示例

**frontend/src/services/api.ts**（Axios 配置）：

```typescript
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          message.error('未认证，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          message.error('无权限访问')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error('服务器错误')
          break
        default:
          message.error(data.message || '请求失败')
      }
    } else {
      message.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default api
```

**frontend/src/services/chat.service.ts**：

```typescript
import api from './api'

export interface AskRequest {
  question: string
  topK?: number
}

export interface Citation {
  resourceId: number
  resourceTitle: string
  chunkId: number
  chunkIndex: number
  location: string
  snippet: string
  score: number
}

export interface AnswerResponse {
  answer: string
  citations: Citation[]
  evidenceInsufficient: boolean
  fallbackResources?: any[]
  latencyMs: number
}

export const chatService = {
  ask: (data: AskRequest) => {
    return api.post<any, AnswerResponse>('/chat/ask', data)
  }
}
```

## 4. 示例 API 调用

### 4.1 用户注册

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "test_user",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**响应**：
```json
{
  "id": 1,
  "username": "test_user",
  "email": "test@example.com",
  "createdAt": "2025-12-25T10:00:00Z"
}
```

### 4.2 用户登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "identifier": "test@example.com",
    "password": "password123"
  }'
```

**响应**：
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "test_user",
    "email": "test@example.com"
  }
}
```

**保存 Token**：后续请求需要使用：
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 4.3 上传学习资源

```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@/path/to/deep_learning.pdf' \
  -F 'title=深度学习基础教程' \
  -F 'tags=机器学习,深度学习' \
  -F 'description=本书介绍深度学习的基础概念'
```

**响应**：
```json
{
  "id": 1,
  "title": "深度学习基础教程",
  "tags": "机器学习,深度学习",
  "fileType": "PDF",
  "fileSize": 5242880,
  "chunkCount": 150,
  "vectorized": true
}
```

### 4.4 RAG 问答

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "什么是 RAG？",
    "topK": 5
  }'
```

**响应**：
```json
{
  "answer": "RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合信息检索和生成式AI的技术...",
  "citations": [
    {
      "resourceId": 1,
      "resourceTitle": "深度学习基础教程",
      "chunkId": 45,
      "chunkIndex": 8,
      "location": "PDF 第 25-27 页",
      "snippet": "检索增强生成（RAG）通过引入外部知识库...",
      "score": 0.85
    }
  ],
  "evidenceInsufficient": false,
  "latencyMs": 2340
}
```

### 4.5 获取推荐

```bash
curl -X GET 'http://localhost:8080/api/recommendations?topN=10' \
  -H "Authorization: Bearer $TOKEN"
```

**响应**：
```json
{
  "recommendations": [
    {
      "resource": { "id": 3, "title": "Transformer 架构详解" },
      "reason": "基于您最近的查询兴趣'深度学习'",
      "suggestion": "建议先学习第 2 章'Self-Attention 机制'",
      "citations": [...]
    }
  ],
  "hasProfile": true
}
```

## 5. 常见问题排查

### 5.1 MySQL 连接失败

**问题**：`java.sql.SQLException: Access denied for user`

**解决方案**：
1. 检查 `application.yml` 中的数据库用户名和密码
2. 验证 MySQL 容器是否正常运行：
   ```bash
   docker-compose ps mysql
   docker-compose logs mysql
   ```
3. 重置 MySQL 密码：
   ```bash
   docker exec -it aetheris-mysql mysql -u root -proot123
   mysql> ALTER USER 'aetheris'@'%' IDENTIFIED BY 'aetheris123';
   ```

### 5.2 Redis Stack 模块未加载

**问题**：`ERR Error loading module`

**解决方案**：
1. 确保使用 `redis/redis-stack-server` 镜像（不是标准 Redis）
2. 验证模块加载：
   ```bash
   docker exec -it aetheris-redis redis-cli
   127.0.0.1:6379> MODULE LIST
   ```
3. 重新创建容器：
   ```bash
   docker-compose down
   docker-compose up -d
   ```

### 5.3 智谱 AI 调用失败

**问题**：`ModelException: API 调用失败，状态码 401`

**解决方案**：
1. 验证 API Key 是否正确：
   ```bash
   echo $ZHIPU_AI_API_KEY
   ```
2. 检查 API Key 是否激活（需要在智谱 AI 平台激活）
3. 确认余额充足（智谱 AI 按使用量计费）
4. 测试 API 连接：
   ```bash
   curl -X POST https://open.bigmodel.cn/api/paas/v4/chat/completions \
     -H "Authorization: Bearer $ZHIPU_AI_API_KEY" \
     -H 'Content-Type: application/json' \
     -d '{"model":"glm-4-flash","messages":[{"role":"user","content":"hello"}]}'
   ```

### 5.4 PDF 文件上传失败

**问题**：`文件格式错误或文件损坏`

**解决方案**：
1. 检查文件大小（限制 50MB）：
   ```bash
   ls -lh /path/to/file.pdf
   ```
2. 验证 PDF 是否损坏：
   ```bash
   docker exec -it aetheris-mysql mysql -u aetheris -paetheris123
   mysql> SELECT file_size, content_hash FROM resources WHERE id = 1;
   ```
3. 查看 PDFBox 日志（启用 DEBUG 日志）：
   ```yaml
   logging:
     level:
       org.apache.pdfbox: DEBUG
   ```

### 5.5 向量检索结果为空

**问题**：问答返回"未找到相关学习资源"

**可能原因**：
1. 资源未向量化：检查 `resource_chunks.vectorized` 字段
2. Redis 索引未创建：执行 `FT.CREATE chunk_idx ...`
3. 查询与资源不相关：尝试更通用的查询（如"深度学习"）

**排查步骤**：
```bash
# 检查 Redis 索引
docker exec -it aetheris-redis redis-cli
FT.INFO chunk_idx

# 检查切片数量
FT.SEARCH chunk_idx *

# 检查向量化状态
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123
mysql> SELECT COUNT(*) FROM resource_chunks WHERE vectorized = TRUE;
```

## 6. 代码规范

### 6.1 Java 代码规范（后端）

遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)：

**基本规则**：
- 使用 2 空格缩进（不使用 Tab）
- 每行最多 100 字符
- 命名规范：
  - 类名：`PascalCase`（如 `UserService`）
  - 方法名：`camelCase`（如 `getUserById`）
  - 常量：`UPPER_SNAKE_CASE`（如 `MAX_PAGE_SIZE`）
  - 包名：全小写（如 `com.aetheris.rag.service`）
- 版权声明在每个文件顶部
- 导入顺序：标准库 → 第三方库 → 项目内部（字母排序）
- 大括号左换行（K&R 风格）

**示例**：
```java
/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
package com.aetheris.rag.service;

import com.aetheris.rag.model.User;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

  private static final int MAX_PAGE_SIZE = 100;

  public User getUserById(Long id) {
    log.info("Getting user by id: {}", id);
    return userMapper.findById(id);
  }
}
```

### 6.2 TypeScript/Vue 代码规范（前端）

遵循 [Google TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)：

**基本规则**：
- 使用 2 空格缩进（不使用 Tab）
- 每行最多 80 字符（字符串除外可到 100）
- 命名规范：
  - 类/接口/枚举：`PascalCase`（如 `UserService`）
  - 变量/函数：`camelCase`（如 `userName`、`getUserById`）
  - 常量：`UPPER_SNAKE_CASE`（如 `MAX_PAGE_SIZE`）
  - 文件名：`PascalCase.vue`（如 `UserList.vue`）
- 字符串优先使用单引号
- 使用分号结尾
- 组件使用 Composition API + `<script setup>`

**示例**：
```vue
<script setup lang="ts">
/*
 * Copyright 2025 Aetheris RAG Team. All rights reserved.
 */
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { User } from '@/types/user'

const router = useRouter()
const user = ref<User | null>(null)
const MAX_PAGE_SIZE = 20

const displayName = computed(() => {
  return user.value?.name ?? 'Guest'
})

const handleLogin = async () => {
  // TODO: Implement login logic
}
</script>

<template>
  <div class="user-info">
    <h1>{{ displayName }}</h1>
  </div>
</template>

<style scoped lang="scss">
.user-info {
  padding: 16px;
}
</style>
```

## 7. 开发工具推荐

### 7.1 API 测试

**Postman**：
- 导入 OpenAPI 规范：`contracts/openapi.yaml`
- 设置环境变量：`baseUrl = http://localhost:8080/api`
- 保存 Token 到环境变量：`{{token}}`

**REST Client（IntelliJ IDEA 内置）**：
- 创建 `http-client.env.json`：
  ```json
  {
    "dev": {
      "baseUrl": "http://localhost:8080/api",
      "token": "your-jwt-token"
    }
  }
  ```
- 创建 `test.http`：
  ```http
  ### 登录
  POST {{baseUrl}}/auth/login
  Content-Type: application/json

  {
    "identifier": "test@example.com",
    "password": "password123"
  }

  ### 问答
  POST {{baseUrl}}/chat/ask
  Authorization: Bearer {{token}}
  Content-Type: application/json

  {
    "question": "什么是 RAG？",
    "topK": 5
  }
  ```

### 7.2 数据库工具

**MySQL Workbench** 或 **DBeaver**：
- 连接：`localhost:3306`
- 用户：`aetheris`
- 密码：`aetheris123`
- 数据库：`aetheris_rag`

**RedisInsight**：
- 连接：`localhost:6379`
- 查看向量索引：`Browse Keys` → `chunk:*`
- 执行搜索：`FT.SEARCH chunk_idx *`

## 8. 下一步

1. **查看 API 文档**：`contracts/openapi.yaml`
2. **查看数据模型**：`data-model.md`
3. **运行单元测试**：`mvn test`
4. **运行集成测试**：`mvn verify -Pintegration-test`
5. **查看实施计划**：`plan.md`

## 9. 获取帮助

- **文档**：查看 `specs/001-rag-recommendation-system/` 目录下的所有文档
- **日志**：`backend/logs/aetheris-rag.log`
- **问题反馈**：在项目仓库提交 Issue

---

**最后更新**：2025-12-25
**文档维护者**：Aetheris RAG Team
