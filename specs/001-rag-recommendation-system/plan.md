# 实施计划：学习资源检索与推荐 RAG 系统

**分支**：`001-rag-recommendation-system` | **日期**：2025-12-25 | **规格**：[功能规格说明](./spec.md)
**输入**：来自 `/specs/001-rag-recommendation-system/spec.md` 的功能规格说明

**注意**：本模板由 `/speckit.plan` 命令填写。参见 `.specify/templates/commands/plan.md` 了解执行工作流。

## 摘要

本系统是一个面向高校学习场景的**性能优先 RAG 检索与推荐系统**，目标是让学生能够"找得到、找得准、说得清"。系统提供学习资源（PDF、Markdown）入库管理、基于向量相似度的语义检索、带引用来源的 RAG 智能问答、以及基于轻量用户画像的 Top-N 个性化推荐。

**技术方法**：采用 Spring Boot 单体应用（前后端分离 B/S 架构），后端提供 RESTful API，前端使用 Vue3。核心 RAG 流程通过 LangChain4j 编排，先通过 Redis Stack 向量检索召回 Top-K 证据，再使用智谱 AI ChatModel 生成答案。所有答案和推荐必须包含完整引用来源（resourceId、chunkId、chunkIndex、页码/范围、snippet）。系统通过统一的 ModelGateway 封装模型调用，实现 Embedding 缓存、幂等入库、日志脱敏和统一的超时/重试/限流策略。

**关键技术约束**：
- Redis Stack 作为唯一向量存储与缓存层（符合宪章二、三原则）
- ModelGateway 作为唯一模型调用出口（符合宪章四原则）
- 所有答案/推荐必须包含可追溯引用（符合宪章五原则）
- 核心链路性能可度量（解析、Embedding、检索、生成）（符合宪章一原则）
- 参数可配置以支持可复现评测（符合宪章七原则）

## 技术上下文

**语言/版本**：Java 21
**主要依赖**：
- **核心框架**：Spring Boot 3.5+ (Spring MVC、Spring Transaction)
  - Spring Boot 3.5.0 GA 发布于 2025年5月22日，基于 Spring Framework 6.2.x
  - **必须启用虚拟线程**（Virtual Threads）以提升并发性能（在 application.yml 中配置 `spring.threads.virtual.enabled=true`）
- **持久层**：MyBatis 3.5+（标准 MyBatis，企业级 ORM 框架）、MyBatis Spring Boot Starter 3.0.3+
  - SQL 语句统一定义在 XML 文件中（非注解方式），便于维护和优化
  - Mapper 接口与 XML 分离，符合企业级最佳实践
- **代码简化**：Lombok 1.18.30+（减少样板代码：@Data、@Slf4j、@Builder 等）
- **日志**：Slf4j + Logback（Spring Boot 默认日志）
- **RAG 编排**：LangChain4j 0.35+（支持 Spring Boot 3.5 和 Java 21）
- **文档处理**：Apache PDFBox 3.0+、CommonMark Java
- **数据存储**：Redis Stack (Lettuce 6.3+ 客户端)、MySQL Connector/J 8.4+、HikariCP（连接池）
- **参数校验**：Jakarta Validation (Spring Boot 内置)
- **工具类**：Guava 33.0+（Google 核心库，提供集合、缓存、并发等工具）、Apache Commons Lang3 3.17+
**存储**：
- MySQL 8：结构化数据（用户、资源元数据、chunk 文本与定位信息、行为记录、评测数据）
- Redis Stack：向量索引（RediSearch Vector KNN）+ 缓存（Embedding 缓存、TopK 结果、热点元数据）
**测试**：JUnit 5、Mockito、Spring Boot Test、Testcontainers (MySQL、Redis)、RestAssured (API 测试)
**前端技术栈**：
- **核心框架**：Vue 3.3+（Composition API、TypeScript）
- **UI 组件库**：Ant Design Vue 4.x（企业级UI组件库）
- **构建工具**：Vite 5.x（快速开发服务器）
- **路由管理**：Vue Router 4.x
- **状态管理**：Pinia（Vue 3 官方推荐）
- **HTTP 客户端**：Axios（Promise based）
- **工具库**：Day.js（日期处理）、Lodash-es（工具函数）
**代码规范**：
- **统一配置**：项目根目录包含 `.editorconfig` 文件，统一所有文件类型的编码风格
- **Java**：遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
  - 2 空格缩进
  - 命名规范：类名（PascalCase）、方法名（camelCase）、常量（UPPER_SNAKE_CASE）
  - 版权声明在每个文件顶部
  - 导入顺序：标准库 → 第三方库 → 项目内部
  - Javadoc 注释：所有公共方法必须包含完整的 Javadoc（@param、@return）
- **TypeScript/Vue**：遵循 [Google TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)
  - 2 空格缩进
  - 命名规范：类/接口/枚举（PascalCase）、变量/函数（camelCase）、常量（UPPER_SNAKE_CASE）
  - 字符串优先使用单引号
  - 组件文件名：PascalCase（如 `UserList.vue`）
  - 严格的类型检查：strict mode enabled
**目标平台**：Linux 服务器（开发环境支持 macOS/Windows + Docker Compose）
**项目类型**：Web 应用（前后端分离）
**性能目标**：
- 资源入库：30 秒内完成（含文本提取、切片、向量化）
- 问答响应：P95 < 3 秒，平均 < 5 秒
- 语义检索：Precision@5 ≥ 0.6，Recall@10 ≥ 0.7
- 推荐效果：有画像比无画像 Precision@10 提升 ≥ 10%
**约束**：
- 单体应用（不使用 Spring Cloud、微服务、消息队列）
- Redis 为唯一向量库（不得引入第二套向量数据库）
- 所有模型调用必须通过 ModelGateway（禁止直连智谱 AI API）
- 所有答案/推荐必须包含 citations（引用来源）
- 关键参数可配置（chunkSize、overlap、topK、topN、模型名称等）
**规模/范围**：
- MVP 支持 100+ 学习资源文档
- 单个文档大小 ≤ 50MB
- 用户规模：高校小规模场景（100-1000 用户）
- 并发：支持 10-20 并发问答请求

## 宪章检查

*关卡：必须在 Phase 0 研究前通过。Phase 1 设计后重新检查。*

### 原则一：性能优先原则 ✅

**要求**：核心链路必须可度量，记录关键阶段耗时（解析、Embedding、向量检索、LLM 生成）

**设计方案**：
- 在 ModelGateway 中记录每次模型调用的耗时（Embedding、Chat 分开记录）
- 在 RAG 服务中记录检索耗时和生成耗时
- 在资源入库服务中记录解析、切片、向量化各阶段耗时
- 使用 Micrometer + Slf4j 记录性能指标到日志和监控系统
- 在 application.yml 中定义性能预算阈值，超出时记录警告日志

**通过**：✅ 设计符合要求，可量化各阶段性能

### 原则二：存储与检索原则 ✅

**要求**：Redis Stack 统一承担向量检索与缓存职责；MySQL 仅用于结构化数据存储

**设计方案**：
- **Redis Stack 存储**：
  - 向量索引：使用 RediSearch Vector KNN 存储 chunk embeddings + 元数据（resourceId、chunkId、chunkIndex、docType、tags）
  - 缓存：Embedding 结果缓存（key=文本哈希）、TopK 检索结果缓存（key=queryHash+filters+topK）、热点资源元数据缓存
- **MySQL 存储**：
  - users（用户表）
  - resources（资源元数据：标题、标签、上传时间、文件类型、描述、内容哈希）
  - resource_chunks（chunk 文本内容、chunkIndex、定位信息（PDF 页码范围/MD 章节/段落）、向量化状态）
  - user_behaviors（查询、点击、收藏行为记录）
  - user_profiles（用户画像向量、更新时间、行为窗口 N）
  - recommendations（推荐结果快照，用于评测）
  - eval_queries（测试查询集）
  - eval_runs（实验运行记录：参数配置、性能指标）

**通过**：✅ 严格遵守原则，无第二套向量库

### 原则三：缓存与幂等原则 ✅

**要求**：Embedding 结果必须按文本哈希缓存；向量化入库必须幂等

**设计方案**：
- **Embedding 缓存**：
  - 在 ModelGateway 中实现缓存层，缓存 key 基于规范化文本（去除冗余空白、统一换行）的 SHA-256 哈希
  - 调用 Embedding API 前先查缓存，命中则直接返回
  - 缓存存储在 Redis，设置合理 TTL（如 30 天）
- **幂等入库**：
  - 资源表使用 `content_hash` 唯一约束，重复上传时检测并拒绝
  - chunk 表使用 `(resource_id, chunk_index)` 唯一约束，防止重复切片
  - Redis 向量索引使用 `chunkId` 作为文档 key，重复写入会覆盖而非创建重复记录
  - 在 ResourceService 中实现幂等检查逻辑：上传前先计算文件哈希，已存在则返回错误

**通过**：✅ 设计符合缓存和幂等要求

### 原则四：模型接入原则 ✅

**要求**：必须设置统一 ModelGateway 作为唯一模型调用出口

**设计方案**：
- **ModelGateway 组件**：
  - `EmbeddingGateway`：封装智谱 AI Embedding API 调用
    - 配置：modelName、timeout、maxTokens、retryPolicy、rateLimit
    - 功能：缓存查找（文本哈希）、API 调用、超时/重试/限流、日志脱敏
  - `ChatGateway`：封装智谱 AI Chat API 调用
    - 配置：modelName、temperature、topP、maxTokens、timeout、retryPolicy、rateLimit
    - 功能：Prompt 构建、API 调用、超时/重试/限流、降级处理（LLM 不可用时返回检索结果+引用摘要）、日志脱敏
- **日志脱敏**：
  - 不记录 API key、Authorization 头
  - 用户输入和上下文片段截断到前 200 字符
  - 记录最小审计信息：模型名、耗时、是否重试、返回状态（无敏感内容）
- **降级策略**：
  - LLM 超时或不可用时，返回 TopK 检索结果 + 引用摘要 + 错误提示，不返回空白失败

**通过**：✅ ModelGateway 是唯一模型调用入口，符合所有要求

### 原则五：可追溯原则 ✅

**要求**：所有问答与推荐结果必须提供引用来源与证据片段

**设计方案**：
- **Citation 结构**（机器可解析的 JSON）：
  ```json
  {
    "resourceId": "uuid",
    "resourceTitle": "资源标题",
    "chunkId": "uuid",
    "chunkIndex": 5,
    "location": "PDF 第 12-14 页 / MD 第三章第一节",
    "snippet": "支持该结论的文本摘录（100-200 字符）",
    "score": 0.85
  }
  ```
- **RAG 问答输出**：
  - `answer`：AI 生成的答案文本
  - `citations[]`：答案引用的证据列表（每个关键论断至少 1 个引用）
  - `evidenceInsufficient`：布尔值，标识证据是否不足
  - `fallbackResources[]`：降级时返回的候选资源列表
- **推荐输出**：
  - `recommendations[]`：Top-N 资源列表
    - 每条包含：`resourceId`、`title`、`reason`（推荐理由）、`suggestion`（学习建议）、`citations[]`（证据引用）

**通过**：✅ 所有输出包含完整引用来源，符合可追溯原则

### 原则六：MVP 迭代原则 ✅

**要求**：优先交付最小可用闭环（Markdown 入库→语义检索→带引用问答→行为记录）

**设计方案**：
- **MVP Phase 1（P1 用户故事）**：
  1. 用户账户与行为记录（注册、登录、查询行为记录）
  2. 资源入库（支持 Markdown、PDF）
  3. 语义检索与 RAG 问答（带引用）
- **MVP Phase 2（P2 用户故事）**：
  4. 个性化推荐（基于最近 N 次查询的轻量画像）
- **迭代计划**：
  - 每个阶段独立可测试、可部署
  - 新功能不显著恶化 p95 时延（性能回归测试）
  - 参数可配置以支持调优（chunkSize、overlap、topK 等）

**通过**：✅ 遵循 MVP 迭代原则，分阶段交付

### 原则七：测试与验收原则 ✅

**要求**：核心链路至少具备单元测试与一条端到端集成测试；关键参数必须可配置并可复现

**设计方案**：
- **单元测试**：
  - 文档处理：PDF/Markdown 解析、切片逻辑、chunkIndex 计算
  - 缓存：文本哈希计算、Embedding 缓存命中/失效
  - 检索：TopK 聚合（按 resource 聚合 max/avg score）
  - 画像：滑动/加权平均计算
- **集成测试**：
  - 完整入库→问答流程（使用 Testcontainers + mock 智谱 AI 或测试 key）
  - 验证 citations 完整性（resourceId、chunkId、chunkIndex、location、snippet）
- **离线评测**：
  - 支持加载标注测试集（JSON/YAML 或 DB）
  - 计算 Precision@K、Recall@K
  - 对比无画像 vs 有画像
  - 记录平均/p95 时延与参数配置
  - 输出可复现报告（JSON/Markdown）
- **可配置参数**（application.yml）：
  - `chunk.size`、`chunk.overlap`、`retrieval.topK`、`recommendation.topN`
  - `embedding.modelName`、`chat.modelName`、`chat.temperature`、`chat.maxTokens`
  - `cache.embedding.ttl`、`cache.search.ttl`
  - `profile.windowSize`、`profile.queryWeight`、`profile.clickWeight`

**通过**：✅ 测试覆盖完整，参数可配置

### 宪章检查总结

✅ **全部通过**：设计方案严格遵守 Aetheris 项目宪章的 7 项核心原则，无违规项，无需复杂度追踪。

## 项目结构

### 文档结构（本功能）

```text
specs/001-rag-recommendation-system/
├── spec.md              # 功能规格说明
├── plan.md              # 本文件（实施计划）
├── research.md          # Phase 0 输出（技术研究）
├── data-model.md        # Phase 1 输出（数据模型）
├── quickstart.md        # Phase 1 输出（快速开始指南）
├── contracts/           # Phase 1 输出（API 契约）
│   ├── openapi.yaml     # OpenAPI 3.0 规范
│   └── api-examples.json # API 请求/响应示例
└── tasks.md             # Phase 2 输出（任务分解，由 /speckit.tasks 生成）
```

### 源代码结构（仓库根目录）

```text
# Web 应用：前后端分离
backend/                 # Spring Boot 后端
├── src/
│   ├── main/
│   │   ├── java/com/aetheris/rag/
│   │   │   ├── AetherisRagApplication.java      # Spring Boot 启动类
│   │   │   ├── config/                          # 配置
│   │   │   │   ├── MySQLConfig.java             # MySQL 数据源配置
│   │   │   │   ├── RedisConfig.java             # Redis Stack 配置
│   │   │   │   ├── ModelGatewayConfig.java      # ModelGateway 配置
│   │   │   │   └── ValidationConfig.java         # 参数校验配置
│   │   │   ├── controller/                      # REST API 控制器
│   │   │   │   ├── AuthController.java          # /api/auth/*
│   │   │   │   ├── ResourceController.java      # /api/resources/*
│   │   │   │   ├── ChatController.java          # /api/chat/*
│   │   │   │   ├── RecommendationController.java # /api/recommendations/*
│   │   │   │   ├── BehaviorController.java      # /api/behaviors/*
│   │   │   │   └── EvalController.java          # /api/eval/*
│   │   │   ├── service/                         # 业务服务层
│   │   │   │   ├── auth/                         # 认证服务
│   │   │   │   │   ├── AuthService.java           # 认证服务接口
│   │   │   │   │   ├── ResourceService.java       # 资源服务接口
│   │   │   │   │   ├── SearchService.java         # 检索服务接口
│   │   │   │   │   ├── RagService.java            # RAG 服务接口
│   │   │   │   │   ├── UserProfileService.java   # 画像服务接口
│   │   │   │   │   ├── RecommendationService.java # 推荐服务接口
│   │   │   │   │   └── EvalService.java           # 评测服务接口
│   │   │   │   ├── impl/                         # 服务实现类（统一放置）
│   │   │   │   │   ├── AuthServiceImpl.java       # 认证服务实现
│   │   │   │   │   ├── ResourceServiceImpl.java   # 资源服务实现
│   │   │   │   │   ├── PdfProcessor.java          # PDF 处理器
│   │   │   │   │   ├── MarkdownProcessor.java    # Markdown 处理器
│   │   │   │   │   ├── VectorServiceImpl.java     # 向量化服务实现
│   │   │   │   │   ├── SearchServiceImpl.java     # 检索服务实现
│   │   │   │   │   ├── RagServiceImpl.java        # RAG 服务实现
│   │   │   │   │   ├── UserProfileServiceImpl.java # 画像服务实现
│   │   │   │   │   ├── RecommendationServiceImpl.java # 推荐服务实现
│   │   │   │   │   └── EvalServiceImpl.java       # 评测服务实现
│   │   │   ├── gateway/                         # 模型网关（核心）
│   │   │   │   ├── ModelGateway.java            # ModelGateway 接口
│   │   │   │   ├── EmbeddingGateway.java        # Embedding 网关实现
│   │   │   │   ├── ChatGateway.java             # Chat 网关实现
│   │   │   │   ├── cache/                       # 缓存实现
│   │   │   │   │   ├── EmbeddingCache.java      # Embedding 缓存
│   │   │   │   │   └── SearchCache.java         # TopK 结果缓存
│   │   │   │   ├── retry/                       # 重试策略
│   │   │   │   │   └── ModelRetryStrategy.java  # 模型调用重试
│   │   │   │   └── sanitize/                    # 日志脱敏
│   │   │   │       └── LogSanitizer.java        # 日志脱敏工具
│   │   │   ├── mapper/                          # MyBatis Mapper 接口
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── ResourceMapper.java
│   │   │   │   ├── ChunkMapper.java
│   │   │   │   ├── BehaviorMapper.java
│   │   │   │   ├── UserProfileMapper.java
│   │   │   │   └── EvalMapper.java
│   │   │   ├── model/                           # 实体模型（@Data、@TableName 等）
│   │   │   │   ├── User.java
│   │   │   │   ├── Resource.java
│   │   │   │   ├── Chunk.java
│   │   │   │   ├── UserBehavior.java
│   │   │   │   ├── UserProfile.java
│   │   │   │   └── EvalRun.java
│   │   │   ├── dto/                             # 数据传输对象
│   │   │   │   ├── request/                     # 请求 DTO
│   │   │   │   │   ├── UploadResourceRequest.java
│   │   │   │   │   ├── AskRequest.java
│   │   │   │   │   └── RegisterRequest.java
│   │   │   │   └── response/                    # 响应 DTO
│   │   │   │       ├── AnswerResponse.java      # 问答响应（含 citations）
│   │   │   │       ├── RecommendationResponse.java
│   │   │   │       └── Citation.java            # 引用结构
│   │   │   ├── exception/                       # 异常处理
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ModelException.java
│   │   │   └── util/                            # 工具类
│   │   │       ├── HashUtil.java                # 文本哈希计算
│   │   │       ├── TextNormalizer.java          # 文本规范化
│   │   │       └── PerformanceTimer.java        # 性能计时器
│   │   └── resources/
│   │       ├── application.yml                  # 主配置文件
│   │       ├── application-dev.yml              # 开发环境配置
│   │       ├── mapper/                          # MyBatis Mapper XML
│   │       │   ├── UserMapper.xml
│   │       │   ├── ResourceMapper.xml
│   │       │   ├── ChunkMapper.xml
│   │       │   ├── BehaviorMapper.xml
│   │       │   ├── UserProfileMapper.xml
│   │       │   └── EvalMapper.xml
│   │       ├── db/migration/                    # 数据库迁移脚本（Flyway）
│   │       │   └── V1__init_schema.sql
│   │       └── logback-spring.xml               # 日志配置
│   └── test/
│       ├── unit/                                # 单元测试
│       │   ├── DocumentProcessorTest.java
│       │   ├── EmbeddingCacheTest.java
│       │   ├── UserProfileServiceTest.java
│       │   └── SearchServiceTest.java
│       ├── integration/                         # 集成测试
│       │   ├── ResourceIngestionTest.java       # 完整入库流程测试
│       │   ├── RagChatTest.java                 # RAG 问答端到端测试
│       │   └── RecommendationTest.java          # 推荐端到端测试
│       └── eval/                                # 离线评测
│           ├── test_dataset.json                # 测试数据集
│           └── EvalServiceTest.java             # 评测服务测试
├── pom.xml                                       # Maven 配置
└── Dockerfile                                    # 后端 Docker 镜像（可选）

frontend/                                       # Vue3 前端（Ant Design Vue）
├── src/
│   ├── main.ts                                 # 应用入口
│   ├── App.vue                                 # 根组件
│   ├── router/                                 # 路由配置
│   │   └── index.ts
│   ├── stores/                                 # Pinia 状态管理
│   │   ├── user.ts                             # 用户状态
│   │   ├── resource.ts                         # 资源状态
│   │   └── chat.ts                             # 问答状态
│   ├── views/                                  # 页面组件
│   │   ├── auth/
│   │   │   ├── LoginView.vue                   # 登录页面
│   │   │   └── RegisterView.vue                # 注册页面
│   │   ├── resource/
│   │   │   ├── ResourceListView.vue            # 资源列表
│   │   │   ├── ResourceDetailView.vue          # 资源详情
│   │   │   └── UploadView.vue                  # 资源上传
│   │   ├── chat/
│   │   │   └── ChatView.vue                    # 问答界面
│   │   ├── recommendation/
│   │   │   └── RecommendationView.vue          # 推荐列表
│   │   └── profile/
│   │       └── ProfileView.vue                 # 个人中心
│   ├── components/                             # 可复用组件
│   │   ├── layout/
│   │   │   ├── AppHeader.vue                   # 应用头部
│   │   │   ├── AppSidebar.vue                  # 应用侧边栏
│   │   │   └── AppFooter.vue                   # 应用底部
│   │   ├── resource/
│   │   │   ├── ResourceCard.vue                # 资源卡片（Ant Design Card）
│   │   │   ├── ResourceTable.vue               # 资源表格（Ant Design Table）
│   │   │   └── UploadArea.vue                  # 上传区域（Ant Design Upload）
│   │   ├── chat/
│   │   │   ├── ChatInput.vue                   # 问答输入框（Ant Design Input）
│   │   │   ├── AnswerDisplay.vue               # 答案展示组件
│   │   │   └── CitationCard.vue                # 引用证据卡片（Ant Design Card）
│   │   ├── recommendation/
│   │   │   └── RecommendationCard.vue          # 推荐卡片（Ant Design Card）
│   │   └── common/
│   │       ├── LoadingSpinner.vue              # 加载动画（Ant Design Spin）
│   │       └── ErrorMessage.vue                # 错误提示（Ant Design Alert）
│   ├── services/                               # API 服务层
│   │   ├── api.ts                              # Axios 实例配置
│   │   ├── auth.service.ts                     # 认证服务
│   │   ├── resource.service.ts                 # 资源服务
│   │   ├── chat.service.ts                     # 问答服务
│   │   ├── recommendation.service.ts           # 推荐服务
│   │   └── behavior.service.ts                 # 行为服务
│   ├── composables/                            # Composition API 可组合函数
│   │   ├── useAuth.ts                          # 认证逻辑
│   │   ├── useResource.ts                      # 资源操作
│   │   └── useChat.ts                          # 问答逻辑
│   ├── utils/                                  # 工具函数
│   │   ├── format.ts                           # 格式化工具（日期、文件大小等）
│   │   └── validation.ts                       # 表单验证规则
│   ├── types/                                  # TypeScript 类型定义
│   │   ├── api.ts                              # API 接口类型
│   │   ├── resource.ts                         # 资源类型
│   │   └── user.ts                             # 用户类型
│   └── assets/                                 # 静态资源
│       ├── styles/                             # 全局样式
│       │   ├── main.scss                        # 主样式文件
│       │   └── variables.scss                   # SCSS 变量
│       └── images/                             # 图片资源
├── public/
│   ├── favicon.ico
│   └── index.html
├── package.json
├── vite.config.ts                              # Vite 配置
├── tsconfig.json                               # TypeScript 配置
└── README.md

docker-compose.yml                               # Docker Compose（MySQL + Redis Stack）
README.md                                        # 项目说明
```

**结构决策**：采用前后端分离架构（Option 2），符合 B/S 架构要求。后端使用 Spring Boot 单体应用，不使用微服务。前端使用 Vue3，通过 RESTful API 与后端通信。这种结构独立可测试、可部署，符合 MVP 迭代原则。

## 复杂度追踪

> **仅当宪章检查有违规且必须论证时填写**

本项目的宪章检查全部通过，无违规项，因此无需复杂度追踪表。

## Phase 0: 研究与决策

### 研究任务

基于技术上下文中的"NEEDS CLARIFICATION"项和依赖项，生成以下研究任务：

1. **LangChain4j 与 Redis Stack 集成最佳实践**
   - 任务：研究 LangChain4j 的 Redis EmbeddingStore 实现
   - 目标：确定如何配置 RediSearch Vector KNN 索引、如何定义 schema、如何执行相似度搜索
   - 输出：Redis 向量索引 schema 定义、索引创建代码示例

2. **智谱 AI API 集成方案**
   - 任务：研究智谱 AI Embedding API 和 Chat API 的调用方式、限流策略、错误处理
   - 目标：确定 ModelGateway 的重试策略（429/5xx）、超时配置、并发限流方案
   - 输出：ModelGateway 配置参数清单、重试策略代码框架

3. **PDF 文本提取与分页信息保留**
   - 任务：研究 Apache PDFBox 如何提取文本并保留页码/范围信息
   - 目标：确定如何按页提取文本、如何记录 chunk 对应的页码范围
   - 输出：PDF 解析服务设计、chunk 页码范围记录方案

4. **Markdown 解析与章节定位**
   - 任务：研究如何解析 Markdown 的 heading/section 结构
   - 目标：确定如何按章节切片、如何记录 chunk 对应的章节/段落信息
   - 输出：Markdown 解析服务设计、chunk 章节定位记录方案

5. **文本哈希与缓存 Key 设计**
   - 任务：研究文本规范化（去空白、统一换行）和哈希计算的最佳实践
   - 目标：确定如何生成唯一且稳定的缓存 key、如何处理文本变体
   - 输出：HashUtil 工具类设计、文本规范化算法

6. **用户画像：滑动平均 vs 加权平均**
   - 任务：研究轻量用户画像的计算方法（滑动平均、加权平均、指数衰减）
   - 目标：确定 MVP 阶段的画像算法（最近 N 次查询的简单滑动平均）
   - 输出：UserProfileService 计算逻辑设计、画像更新触发条件

7. **离线评测数据集格式与评估指标**
   - 任务：研究 Precision@K、Recall@K 的计算方法和测试数据集格式
   - 目标：确定测试数据集的 JSON/YAML 格式、评估指标计算公式
   - 输出：测试数据集 schema、EvalService 评估逻辑设计

### 研究输出

所有研究结果将整理到 `research.md` 文件中，每个研究任务包含：
- **决策**：选择的技术方案或工具
- **理由**：为何选择该方案（性能、成本、可维护性）
- **替代方案**：考虑过的其他方案及拒绝原因

## Phase 1: 设计与契约

### 前提条件

`research.md` 已完成，所有技术决策已确定。

### 交付物

1. **数据模型** (`data-model.md`)
   - 从功能规格说明提取关键实体（User、Resource、Chunk、UserBehavior、UserProfile、Citation 等）
   - 定义实体属性、关系、验证规则
   - 定义 MySQL 表结构（字段、类型、索引、约束）
   - 定义 Redis 向量索引 schema（字段、向量维度、距离算法）
   - MyBatis Mapper 接口定义（符合 Google Java Style Guide）
   - MyBatis Mapper XML 配置（SQL 语句与接口分离）

2. **API 契约** (`contracts/`)
   - 从功能需求生成 RESTful API 端点
   - 定义请求/响应格式（JSON schema）
   - 生成 OpenAPI 3.0 规范 (`openapi.yaml`)
   - 提供 API 请求/响应示例 (`api-examples.json`)

3. **快速开始指南** (`quickstart.md`)
   - 本地开发环境搭建（Docker Compose 启动 MySQL + Redis Stack）
   - 后端 Spring Boot 启动步骤
   - 前端 Vue3 + TypeScript + Ant Design Vue 启动步骤
   - 示例 API 调用（curl 或 Postman）
   - 常见问题排查

4. **代码规范配置** (`.editorconfig`)
   - 统一 Java、TypeScript、XML、JSON、YAML 等文件类型的编码风格
   - 符合 Google Java Style Guide 和 Google TypeScript Style Guide
   - 配置缩进、换行符、字符编码等基础规范

5. **Agent 上下文更新**
   - 运行 `.specify/scripts/bash/update-agent-context.sh claude`
   - 更新 Claude Code 的项目上下文文件，添加新技术栈（Java 17、Spring Boot、LangChain4j、Redis Stack、智谱 AI）

## Phase 2: 任务分解

### 前提条件

Phase 1 的所有交付物已完成（data-model.md、contracts/、quickstart.md）。

### 执行

使用 `/speckit.tasks` 命令生成任务分解文件 (`tasks.md`)，任务组织方式：
- 按 Phase 1（基础设施）、Phase 2-N（用户故事）组织
- 每个用户故事可独立实施和测试
- 任务标记为可并行执行 `[P]` 或串行依赖
- 包含测试任务（单元测试、集成测试）

### 输出

`tasks.md` 文件，包含详细的任务列表、依赖关系、执行顺序。

## 下一步行动

1. ✅ 完成本计划文档 (`plan.md`)
2. ⏭️ 执行 Phase 0 研究，生成 `research.md`
3. ⏭️ 执行 Phase 1 设计，生成 `data-model.md`、`contracts/`、`quickstart.md`
4. ⏭️ 更新 agent 上下文
5. ⏭️ 重新评估宪章检查（Phase 1 后）
6. ⏭️ 执行 `/speckit.tasks` 生成任务分解
