# Aetheris Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-12-26

## 项目概述

**Aetheris** 是一个面向高校学习场景的**性能优先 RAG 检索与推荐系统**，目标让学生能够"找得到、找得准、说得清"。

**核心功能**：
- 学习资源入库管理（PDF、Markdown 文档）
- 基于向量相似度的语义检索
- 带引用来源的 RAG 智能问答
- 基于轻量用户画像的 Top-N 个性化推荐
- 离线评测与性能度量

**架构特点**：
- Spring Boot 单体应用（前后端分离 B/S 架构）
- Redis Stack 作为唯一向量存储与缓存层
- ModelGateway 统一模型调用出口（智谱 AI）
- 所有答案/推荐必须包含可追溯引用来源

## 当前功能分支

### 001-rag-recommendation-system

**状态**：实施阶段（Phase 1 设计已完成，92个任务待实施）

**规格文档**：`specs/001-rag-recommendation-system/spec.md`
**实施计划**：`specs/001-rag-recommendation-system/plan.md`
**任务清单**：`specs/001-rag-recommendation-system/tasks.md`
**快速开始**：`specs/001-rag-recommendation-system/quickstart.md`
**技术研究**：`specs/001-rag-recommendation-system/research.md`

**用户故事**：
1. 资源入库与可追溯切片（P1）
2. 语义检索与 RAG 问答（P1）
3. 个性化推荐（P2）
4. 用户账户与行为记录（P1）

## 技术栈

### 后端技术栈

**核心框架**：
- **Java 21**（必须启用虚拟线程）
- **Spring Boot 3.5+**（Spring MVC、Spring Transaction）
- **MyBatis 3.5+**（企业级 ORM，SQL 定义在 XML 文件中，非注解方式）
- **Lombok 1.18.30+**（代码简化：@Data、@Slf4j、@Builder）

**RAG 编排与 AI 集成**：
- **LangChain4j 0.35+**（RAG 编排，支持 Spring Boot 3.5 和 Java 21）
- **智谱 AI API**（Embedding: embedding-v2, Chat: glm-4-flash）

**数据存储**：
- **MySQL 8**：结构化数据（用户、资源元数据、chunk 文本、行为记录、评测数据）
- **Redis Stack**：向量索引（RediSearch KNN）+ 缓存（Embedding、TopK 结果）

**文档处理**：
- **Apache PDFBox 3.0+**（PDF 解析与页码追踪）
- **CommonMark Java**（Markdown 解析与章节定位）

**工具库**：
- **Guava 33.0+**（Google 核心库：集合、缓存、并发）
- **Apache Commons Lang3 3.17+**
- **HikariCP**（数据库连接池）
- **Lettuce 6.3+**（Redis 客户端）

**测试框架**：
- **JUnit 5**、**Mockito**、**Spring Boot Test**
- **Testcontainers**（MySQL、Redis 集成测试）
- **RestAssured**（API 测试）

### 前端技术栈

- **Vue 3.3+**（Composition API、TypeScript）
- **Ant Design Vue 4.x**（企业级 UI 组件库）
- **Vite 5.x**（构建工具）
- **Pinia**（状态管理）
- **Vue Router 4.x**（路由管理）
- **Axios**（HTTP 客户端）
- **Day.js**（日期处理）
- **Lodash-es**（工具函数）

## 关键设计决策与约束

### ⚠️ 重要约束（必须遵守）

1. **虚拟线程必须启用**：
   ```yaml
   spring:
     threads:
       virtual:
         enabled: true  # 必须启用，不可关闭
   ```
   - 理由：提升并发性能，支持 10-20 并发用户

2. **不使用 Java 21 Record**：
   - 使用 **Lombok @Data、@Builder、@AllArgsConstructor** 注解代替
   - 理由：保持与现有代码风格一致，简化序列化

3. **MyBatis SQL 定义方式**：
   - SQL 语句统一定义在 **XML 文件**中（`src/main/resources/mapper/`）
   - **不使用** `@Select`、`@Insert` 等注解方式
   - 理由：企业级最佳实践，便于维护和优化

4. **Redis Stack 是唯一向量存储**：
   - **禁止**引入第二套向量数据库（如 Pinecone、Weaviate、Qdrant）
   - 理由：统一存储架构，降低运维复杂度

5. **ModelGateway 是唯一模型调用出口**：
   - **禁止**业务代码直连智谱 AI API
   - 所有模型调用必须通过 `ModelGateway`
   - 理由：统一的超时/重试/限流/降级策略、成本控制、安全脱敏

6. **所有答案/推荐必须包含引用来源**：
   - citations 必须包含：resourceId、resourceTitle、chunkId、chunkIndex、location、snippet
   - **禁止**输出无法溯源的结论性内容
   - 理由：可验证性、用户信任、调试检索质量

7. **LLM 降级策略**：
   - 当 LLM 不可用或超时时，**不得返回空白失败**
   - 必须返回：检索到的 Top-5 切片结果 + 证据摘要 + 候选资源列表

### 存储分离原则

**MySQL 存储**：
- 用户（users）
- 资源元数据（resources）
- Chunk 文本与定位信息（resource_chunks）
- 用户行为记录（user_behaviors：QUERY/CLICK/FAVORITE）
- 用户画像（user_profiles）
- 评测数据（eval_queries、eval_runs）

**Redis 存储**：
- 向量索引（chunk_idx：RediSearch VECTOR KNN）
- Embedding 缓存（`embedding:cache:{textHash}`，TTL 30 天）
- TopK 结果缓存（`search:result:{queryHash}:{topK}`，TTL 1 小时）
- 用户画像缓存（`profile:user:{userId}`，TTL 7 天）

### 缓存与幂等策略

**Embedding 缓存（必须实现）**：
- 缓存 key：基于**规范化文本**的 SHA-256 哈希
- 规范化：去除冗余空白、统一换行符、去除首尾空白
- TTL：30 天
- **调用 Embedding API 前必须先查缓存，命中则禁止重复调用**

**资源入库幂等（必须实现）**：
- 基于内容哈希（SHA-256）去重
- 相同内容不得产生重复记录或重复计费

## 性能要求

### 性能预算阈值（P95/P99）

- **资源入库**：P95 ≤ 30秒，P99 ≤ 45秒，平均 20秒
- **问答响应**：P95 ≤ 5秒，P99 ≤ 8秒，平均 3秒
- **语义检索**：P95 ≤ 1秒，P99 ≤ 2秒，平均 0.5秒
- **推荐生成**：P95 ≤ 2秒，P99 ≤ 3秒，平均 1.5秒

### 性能监控要求

- 必须记录每个请求的总耗时和分段耗时（解析、Embedding、检索、生成）
- 性能指标必须输出到日志和监控系统
- 当性能超出预算阈值时，必须记录警告日志

### 并发性能

- 系统应支持 **10-20 并发用户**同时进行问答请求
- 响应时间不应超过 P95 阈值的 150%

## 安全要求

- **密码策略**：至少 8 位，包含字母和数字，使用 BCrypt 算法哈希存储
- **JWT token**：有效期 24 小时
- **API 安全**：所有需要身份验证的 API 端点必须验证 JWT token
- **输入验证**：查询文本长度限制 500 字符
- **日志脱敏**：不得记录 API key、密码、token 完整内容，用户输入截断至 200 字符

## 项目目录结构

```
Aetheris/
├── backend/                          # Spring Boot 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aetheris/rag/
│   │   │   │   ├── controller/       # REST API 控制器
│   │   │   │   ├── service/          # 业务逻辑服务接口
│   │   │   │   │   └── impl/         # 业务逻辑服务实现
│   │   │   │   ├── mapper/           # MyBatis Mapper 接口
│   │   │   │   ├── model/            # 数据模型（@Data 注解）
│   │   │   │   ├── dto/              # 数据传输对象
│   │   │   │   │   ├── request/      # 请求 DTO
│   │   │   │   │   └── response/     # 响应 DTO（含 Citation）
│   │   │   │   ├── gateway/          # ModelGateway 统一模型调用
│   │   │   │   │   ├── cache/        # 缓存实现
│   │   │   │   │   ├── retry/        # 重试策略
│   │   │   │   │   └── sanitize/     # 日志脱敏
│   │   │   │   ├── config/           # Spring 配置类
│   │   │   │   ├── util/             # 工具类
│   │   │   │   └── AetherisRagApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml           # 主配置文件
│   │   │       ├── application-dev.yml       # 开发环境配置
│   │   │       ├── db/
│   │   │       │   └── migration/            # Flyway 数据库迁移脚本
│   │   │       │       └── V1__init_schema.sql
│   │   │       └── mapper/                   # MyBatis XML 文件
│   │   │           ├── UserMapper.xml
│   │   │           ├── ResourceMapper.xml
│   │   │           └── ...
│   │   └── test/                     # 测试代码
│   └── pom.xml
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 调用封装
│   │   ├── assets/                   # 静态资源
│   │   ├── components/               # Vue 组件
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # Pinia 状态管理
│   │   ├── types/                    # TypeScript 类型定义
│   │   ├── utils/                    # 工具函数
│   │   ├── views/                    # 页面视图
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── specs/                            # 功能规格与设计文档
│   └── 001-rag-recommendation-system/
│       ├── spec.md                   # 功能规格说明
│       ├── plan.md                   # 实施计划
│       ├── tasks.md                  # 任务清单（92 个任务）
│       ├── research.md               # 技术研究与决策
│       └── quickstart.md             # 快速开始指南
│
├── .specify/                         # SpecKit 工具配置
│   ├── memory/
│   │   └── constitution.md           # 项目宪章（7 大原则）
│   └── templates/                    # 文档模板
│
├── docker-compose.yml                # MySQL + Redis Stack
├── .editorconfig                     # 统一编码规范（2 空格缩进）
└── CLAUDE.md                         # 本文件
```

## 常用命令

### 基础设施

```bash
# 启动 MySQL 和 Redis Stack
docker-compose up -d

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f redis-stack

# 停止服务
docker-compose down

# 验证连接
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123
docker exec -it aetheris-redis redis-cli
```

### 后端开发

```bash
# 进入后端目录
cd backend

# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

### 前端开发

```bash
# 进入前端目录
cd frontend

# 安装依赖（使用 pnpm）
pnpm install

# 启动开发服务器
pnpm dev

# 构建生产版本
pnpm build

# 预览生产构建
pnpm preview
```

### SpecKit 命令

```bash
# 查看任务清单
cat specs/001-rag-recommendation-system/tasks.md

# 分析规格文档
/speckit.analyze

# 生成任务清单
/speckit.tasks

# 执行实施
/speckit.implement
```

## 代码规范

### Java 代码规范（遵循 Google Java Style Guide）

- **缩进**：2 空格（不使用 Tab）
- **命名规范**：
  - 类名：`PascalCase`（如 `UserServiceImpl`）
  - 方法名：`camelCase`（如 `getUserById`）
  - 常量：`UPPER_SNAKE_CASE`（如 `MAX_RETRIES`）
  - 变量：`camelCase`（如 `userName`）
- **导入顺序**：标准库 → 第三方库 → 项目内部
- **Javadoc**：所有公共方法必须包含完整的 Javadoc（@param、@return、@throws）
- **注解使用**：
  - 实体类使用 `@Data`、`@Builder`、`@AllArgsConstructor`、`@NoArgsConstructor`
  - Service 接口和实现分离（`AuthService`、`AuthServiceImpl`）
  - Controller 使用 `@RestController`、`@RequestMapping`、`@PostMapping` 等
- **异常处理**：使用 `@RestControllerAdvice` 统一异常处理
- **日志**：使用 `@Slf4j` 注解（Lombok 提供），日志级别：ERROR < WARN < INFO < DEBUG

### TypeScript/Vue 代码规范（遵循 Google TypeScript Style Guide）

- **缩进**：2 空格
- **命名规范**：
  - 类/接口/枚举：`PascalCase`（如 `UserService`）
  - 变量/函数：`camelCase`（如 `userName`、`getUserById`）
  - 常量：`UPPER_SNAKE_CASE`（如 `API_BASE_URL`）
  - 组件文件名：`PascalCase`（如 `UserList.vue`）
- **字符串**：优先使用单引号
- **类型定义**：严格的 TypeScript 类型检查（strict mode enabled）
- **组件结构**：使用 Composition API（`<script setup lang="ts">`）

### MyBatis XML 规范

- XML 文件位于 `src/main/resources/mapper/`
- 文件名与 Mapper 接口同名（如 `UserMapper.xml`）
- namespace 对应 Mapper 接口全限定名
- SQL 语句 id 对接接口方法名
- 使用 `<resultMap>` 定义结果映射
- 参数使用 `#{paramName}`（预编译），避免 `${paramName}`（字符串拼接）

## 重要文件位置

### 配置文件

- **后端主配置**：`backend/src/main/resources/application.yml`
- **开发环境配置**：`backend/src/main/resources/application-dev.yml`
- **Docker Compose**：`docker-compose.yml`
- **编码规范**：`.editorconfig`

### 数据库

- **Flyway 迁移脚本**：`backend/src/main/resources/db/migration/`
- **初始化脚本**：`backend/src/main/resources/db/migration/V1__init_schema.sql`

### 设计文档

- **功能规格**：`specs/001-rag-recommendation-system/spec.md`
- **实施计划**：`specs/001-rag-recommendation-system/plan.md`
- **任务清单**：`specs/001-rag-recommendation-system/tasks.md`
- **技术研究**：`specs/001-rag-recommendation-system/research.md`
- **快速开始**：`specs/001-rag-recommendation-system/quickstart.md`

### 核心代码

- **ModelGateway**：`backend/src/main/java/com/aetheris/rag/gateway/`
- **Citation 结构**：`backend/src/main/java/com/aetheris/rag/dto/response/Citation.java`
- **认证授权**：`backend/src/main/java/com/aetheris/rag/service/auth/`
- **工具类**：`backend/src/main/java/com/aetheris/rag/util/`

## 宪章核心原则（必须遵守）

详见完整宪章：`.specify/memory/constitution.md`

### 1. 性能优先原则
- 核心链路必须可度量（解析、Embedding、检索、生成）
- 必须定义并强制执行性能预算
- 任何新增功能不得显著恶化 P95 时延

### 2. 存储与检索原则
- Redis Stack 统一承担向量检索与缓存职责
- 不得引入第二套向量数据库
- MySQL 仅用于结构化数据与文本/元数据存储

### 3. 缓存与幂等原则
- Embedding 结果必须按文本哈希进行缓存
- 向量化入库必须幂等，防止重复计费

### 4. 模型接入原则
- ModelGateway 是唯一模型调用出口
- 业务代码不得直连智谱 AI API
- 必须统一实现超时、重试、限流、降级、日志脱敏

### 5. 可追溯原则
- 所有问答与推荐结果必须提供引用来源与证据片段
- 不得输出无法指向证据的结论性内容

### 6. MVP 迭代原则
- 优先交付最小可用闭环
- 用户画像采用"轻量可落地"策略（最近 N 次查询滑动平均）
- 每个迭代必须独立可部署、可测试、可演示

### 7. 测试与验收原则
- 核心链路具备自动化测试
- 检索与推荐具备离线评测（Precision@K、Recall@K）
- 关键参数必须可配置并可复现

## Recent Changes

- 001-rag-recommendation-system: Added (2025-12-25)
- Phase 1 设计已完成（数据模型、API 契约、Citations 结构）
- 92 个实施任务已定义（T001-T092）
- 虚拟线程必须启用（T008）
- 性能预算已定义（P95/P99 阈值）
- 安全策略已完善（密码、JWT、输入验证）

<!-- MANUAL ADDITIONS START -->

### 开发注意事项

1. **启动顺序**：先启动 `docker-compose up -d`（MySQL + Redis），再启动后端 `mvn spring-boot:run`，最后启动前端 `pnpm dev`
2. **API 密钥**：智谱 AI API key 需配置在环境变量或 `application-dev.yml` 中（不要提交到 Git）
3. **首次运行**：Flyway 会自动执行数据库迁移脚本，创建表结构
4. **测试数据**：准备一些测试 PDF 和 Markdown 文档用于入库测试
5. **性能监控**：开发时关注日志中的性能指标（分段耗时），确保符合性能预算
6. **日志级别**：开发环境使用 DEBUG 级别，生产环境使用 INFO 级别
7. **代码审查**：提交代码前确保符合 Google Java Style Guide 和 TypeScript Style Guide
8. **缓存调试**：开发时可临时关闭 Embedding 缓存（配置开关）以便测试

<!-- MANUAL ADDITIONS END -->
