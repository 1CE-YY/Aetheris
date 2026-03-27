# Aetheris RAG System

> 面向高校的学习资源检索与推荐系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.3.8-brightgreen.svg)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📖 项目简介

**Aetheris** 是一个基于 RAG（Retrieval-Augmented Generation）架构的学习资源检索与推荐系统，面向高校学习场景，提供智能化的资源发现和问答服务。

### 核心功能

- 🔍 **语义检索**：基于向量嵌入的智能搜索
- 💬 **RAG 问答**：带引用来源的智能问答系统
- 🎯 **个性化推荐**：基于用户画像的精准推荐
- 📚 **资源管理**：支持 PDF/Markdown 文档入库
- 📊 **离线评测**：Precision@K、Recall@K 等指标度量

### 技术栈

**后端**：
- Spring Boot 3.5.9 + Java 21
- MyBatis
- LangChain4j
- Redis Stack
- MySQL

**前端**：
- Vue 3.3 + TypeScript
- Ant Design Vue 4.x
- Vite 5.x + Pinia

**基础设施**：
- Docker Compose（MySQL + Redis）
- JWT 认证
- Flyway（数据库迁移）

---

## 🚀 快速开始

### ⚠️ 重要：服务管理规范

**必须使用根目录下的脚本启动和停止服务，不要使用其他方式！**

#### 一键启动（推荐）

```bash
cd /Users/hubin5/app/Aetheris

# 交互模式（弹出菜单，选择后自动执行并退出）
./start.sh

# 命令行模式（推荐）
./start.sh --all              # 启动所有服务（Docker + 后端 + 前端）
```

#### 选择性启动

```bash
./start.sh --frontend-only    # 仅启动前端
./start.sh --backend-only     # 仅启动后端
./start.sh --docker-only      # 仅启动 Docker（MySQL + Redis）
./start.sh --all              # 启动所有服务
./start.sh --help             # 显示帮助信息
```

#### 停止服务

```bash
# 交互模式（弹出菜单，选择后自动执行并退出）
./stop.sh

# 命令行模式（推荐）
./stop.sh --all               # 停止所有服务（前端 + 后端）
./stop.sh --frontend-only     # 仅停止前端
./stop.sh --backend-only      # 仅停止后端
./stop.sh --docker-only       # 仅停止 Docker（MySQL + Redis）
./stop.sh --help              # 显示帮助信息
```

#### 查看服务状态和日志

```bash
cat .pids.json | jq           # 查看服务状态
tail -f logs/backend.log      # 后端日志
tail -f logs/application.log  # 应用日志
tail -f logs/frontend.log     # 前端日志
docker-compose logs -f        # Docker 日志
```

### ❌ 禁止的操作

- ❌ 直接使用 `mvn spring-boot:run` 启动后端
- ❌ 直接使用 `npm run dev` 启动前端
- ❌ 单独使用 `docker-compose up -d` 启动基础设施
- ❌ 手动使用 `kill` 命令杀进程
- ❌ 手动 export JAVA_HOME（脚本会自动处理）

### 手动启动（仅用于开发调试）

如果需要单独启动某个组件进行调试，请按以下步骤操作：

#### 1. 启动基础设施

```bash
docker-compose up -d
```

#### 2. 启动后端（仅调试用）

```bash
# ⚠️ 注意：start.sh 已自动配置 Java 21，但如果需要在新的终端中调试
cd backend
mvn spring-boot:run
```

#### 3. 启动前端（仅调试用）

```bash
cd frontend
pnpm install
pnpm dev
```

---

## 🌐 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 🌐 **前端** | http://localhost:5173 | Vue 3 开发服务器 |
| 🔧 **后端 API** | http://localhost:8080 | Spring Boot 应用 |
| 📊 **健康检查** | http://localhost:8080/actuator/health | Actuator 端点 |
| 🗄️ **MySQL** | localhost:3306 | 数据库 |
| 🔴 **Redis** | localhost:6379 | Redis Stack |


---

## 📂 项目结构

```
Aetheris/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/aetheris/rag/
│   │       ├── controller/      # REST API 控制器
│   │       ├── service/         # 业务接口
│   │       │   └── impl/        # 业务实现
│   │       ├── common/          # 公共组件
│   │       │   └── response/    # 统一响应格式
│   │       ├── mapper/          # MyBatis 接口
│   │       ├── exception/       # 自定义异常
│   │       ├── entity/          # 数据模型
│   │       ├── dto/             # 请求/响应 DTO
│   │       │   ├── request/     # 请求 DTO
│   │       │   └── response/    # 响应 DTO
│   │       ├── gateway/         # ModelGateway 框架
│   │       │   ├── cache/       # 缓存
│   │       │   ├── retry/       # 重试
│   │       │   └── sanitize/    # 日志脱敏
│   │       ├── config/          # Spring 配置
│   │       ├── util/            # 工具类
│   │       ├── validation/      # 自定义校验
│   │       ├── constants/       # 常量定义
│   │       └── schedule/        # 定时任务
│   ├── src/main/resources/
│   │   ├── application.yml              # 主配置
│   │   ├── db/migration/                # Flyway 迁移
│   │   └── mapper/                      # MyBatis XML
│   └── src/test/                        # 单元测试

├── frontend/                # Vue 3 前端
│   ├── src/
│   │   ├── api/             # API 调用
│   │   ├── components/      # Vue 组件
│   │   ├── views/           # 页面组件
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # Pinia 状态
│   │   └── utils/           # 工具函数
│   └── vite.config.ts       # Vite 配置
├── docs/                    # 项目文档
│   ├── STARTUP_GUIDE.md            # 启动指南
│   ├── PHASE1_2_ACCEPTANCE_REPORT.md  # 验收报告
│   └── dev-logs/                   # 开发日志
├── specs/                   # 需求与规范
│   └── 001-rag-recommendation-system/
│       ├── spec.md                 # 需求规格
│       ├── plan.md                 # 实施计划
│       ├── tasks.md                # 任务清单
│       └── contracts/openapi.yaml  # API 规范
├── data/                    # 数据持久化（gitignore）
├── logs/                    # 日志文件（gitignore）
├── docker-compose.yml       # Docker 编排
├── start.sh                 # 一键启动脚本
├── stop.sh                  # 一键停止脚本
├── .env                     # 环境变量
└── README.md                # 本文件
```

---

## 📋 开发进度

### ✅ Phase 1-2：项目初始化 + 基础设施层（已完成）

**完成度**: 100% ✅
**验收状态**: ⭐⭐⭐⭐⭐ 97.1%（优秀）
**验收日期**: 2025-12-29

**已实现功能**：
- ✅ 项目结构搭建（Spring Boot + Vue 3）
- ✅ Docker Compose 基础设施（MySQL 8 + Redis Stack）
- ✅ 数据库表结构设计（8 张表）
- ✅ ModelGateway 框架（EmbeddingGateway、ChatGateway stub）
- ✅ 用户认证系统（JWT + BCrypt + Spring Security）
- ✅ 工具类库（HashUtil、TextNormalizer、PerformanceTimer）
- ✅ Citations 统一结构
- ✅ 日志脱敏工具（LogSanitizer）

**详细报告**: [docs/PHASE1_2_ACCEPTANCE_REPORT.md](docs/PHASE1_2_ACCEPTANCE_REPORT.md)

### ✅ Phase 3：用户认证与前端页面（已完成）

**完成度**: 100% ✅
**验收日期**: 2025-12-31

**已实现功能**：
- ✅ 用户注册/登录功能（后端 API + 前端页面）
- ✅ 用户行为记录系统（后端 API）
- ✅ Token 验证系统（GET /api/auth/me）
- ✅ 前端用户状态管理（Pinia store + 路由守卫）
- ✅ Token 失效自动跳转
- ✅ 错误处理优化

### ✅ Phase 4：资源入库与向量化（已完成）

**完成度**: 100% ✅
**验收日期**: 2026-01-07

**已实现功能**：
- ✅ 资源上传功能（支持 PDF/Markdown 文件）
- ✅ PDF 文本提取与页码记录（Apache PDFBox）
- ✅ Markdown 解析与章节路径记录（CommonMark）
- ✅ 文档自动切片（固定大小 + 重叠）
- ✅ 向量化批量处理（调用 ModelGateway.embed()）
- ✅ Redis Stack 向量索引创建
- ✅ 包名重构（model → entity）

### 🚧 Phase 5：RAG 问答系统（进行中）

**当前状态**: 开发中

**待实现功能**：
- ⏳ 向量检索（RediSearch）
- ⏳ Prompt 模板设计
- ⏳ LLM 调用与响应解析
- ⏳ 引用来源生成
- ⏳ 问答 API 实现

### 📋 Phase 6：推荐系统与评测（待开始）

**计划功能**：
- ⏳ 用户行为记录完善
- ⏳ 用户画像更新
- ⏳ 个性化推荐算法
- ⏳ 离线评测系统
- ⏳ 性能优化

---

## 🔧 环境要求

### 必需

- **Java 21**（必须！不支持其他版本）
  ```bash
  java -version  # openjdk version "21.0.8"
  ```
- **Maven 3.6+**
  ```bash
  mvn -version
  ```
- **Node.js 18+**
  ```bash
  node -v
  ```
- **Docker & Docker Compose**
  ```bash
  docker --version
  docker-compose --version
  ```

### 可选

- **Make**（用于便捷命令）
- **jq**（用于 JSON 处理）

---


### 生成安全的 JWT 密钥

```bash
openssl rand -base64 32
```
---

## 📝 开发指南

### 代码规范

#### 注释和文档语言
- ✅ **所有代码注释必须使用中文**
  - Javadoc 类注释：中文
  - Javadoc 方法注释：中文
  - 行内注释：中文
  - TODO/FIXME 标记：中文
  - 日志输出：中文
- ✅ **所有项目文档必须使用中文**
  - 技术文档、设计文档、API 文档
  - README、指南、验收报告、开发日志
  - 配置文件注释
- ✅ **变量和方法命名使用英文**（遵循 Java/TypeScript 命名规范）
- ✅ **技术术语保留英文**（如：API、JWT、Redis、Spring Boot 等）

#### 后端规范
- ✅ 不使用 Java Record，使用 Lombok `@Data`、`@Builder`
- ✅ MyBatis SQL 必须写在 XML 文件，禁止使用 `@Select` 等注解
- ✅ Service 接口和实现分离：`service/XXXService.java` + `service/impl/XXXServiceImpl.java`
- ✅ 依赖注入统一使用 `@RequiredArgsConstructor`（构造器注入）
- ✅ 虚拟线程必须启用：`spring.threads.virtual.enabled: true`

#### 前端规范
- 使用 TypeScript 严格模式
- 组件命名采用 PascalCase
- 遵循 Vue 3 Composition API 最佳实践


## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件
