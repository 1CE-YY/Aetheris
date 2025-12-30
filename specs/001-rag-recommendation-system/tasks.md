# 任务清单：学习资源检索与推荐 RAG 系统

**分支**：`001-rag-recommendation-system`
**输入**：`specs/001-rag-recommendation-system/` 目录下的设计文档
**生成日期**：2025-12-26

## 任务组织说明

- **P1 (MVP 必须)**：覆盖用户故事 1/2/4（资源入库、问答、用户账户）
- **P2 (增强)**：覆盖用户故事 3（推荐）与评测对比增强
- 每个任务包含：目标、数据表/字段、Redis 配置、API 端点、关键配置、测试要点、验收标准
- 任务按可独立演示的里程碑组织

---

## Phase 1: 项目初始化与基础设施 ✅ **已验收**

**目标**：搭建 Spring Boot 单体应用项目结构，配置数据库和 Redis 连接
**验收日期**: 2025-12-29
**验收评分**: ⭐⭐⭐⭐⭐ 97.1%

- [x] T001 创建后端项目结构 `backend/`，包含 `src/main/java/com/aetheris/rag/`、`src/main/resources/`、`src/test/` 目录
- [x] T002 创建前端项目结构 `frontend/`，初始化 Vue3 + Vite + TypeScript 项目
- [x] T003 [P] 配置 `.editorconfig` 文件，统一 Java/TypeScript/XML 编码规范（2 空格缩进）
- [x] T004 [P] 配置 `backend/pom.xml`，添加 Spring Boot 3.5+、MyBatis、Lombok、LangChain4j、Redis Lettuce、MySQL Connector、Guava、Commons Lang3 依赖
- [x] T005 [P] 配置 `frontend/package.json`，添加 Vue 3.3+、Ant Design Vue 4.x、Pinia、Vue Router、Axios、Day.js、Lodash-es 依赖
- [x] T006 创建 Docker Compose 配置文件 `docker-compose.yml`，定义 MySQL 8 和 Redis Stack 服务
- [x] T007 [P] 创建 `backend/src/main/resources/application.yml`，配置 MySQL 数据源（HikariCP）、Redis Stack 连接（Lettuce）、Flyway 迁移
- [x] T008 [P] **配置虚拟线程（必须）**：在 `application.yml` 中添加 `spring.threads.virtual.enabled=true` 以提升并发性能
  - **目标**：启用 Java 21 虚拟线程，满足系统并发性能要求（支持 10-20 并发用户）
  - **配置项**：`spring.threads.virtual.enabled=true`
  - **验收标准**：系统启动时日志显示虚拟线程已启用，并发测试验证吞吐量提升
- [x] T009 [P] 创建 `backend/src/main/resources/application-dev.yml`，配置开发环境日志级别（SLF4j + Logback）
- [x] T010 [P] 创建 `backend/src/main/resources/db/migration/V1__init_schema.sql`，定义 users、resources、resource_chunks、user_behaviors、user_profiles、eval_queries、eval_runs 表结构

**验收标准**：✅ **全部通过**
- [x] `docker-compose up -d` 成功启动 MySQL 和 Redis Stack
- [x] `mvn spring-boot:run` 成功启动后端应用，连接 MySQL 和 Redis
- [x] `npm run dev` 成功启动前端开发服务器

**验收备注**：
- ✅ Docker 服务状态正常（MySQL + Redis Stack）
- ✅ Redis Stack 6 个模块全部加载（包括向量搜索模块）
- ✅ 数据库迁移成功（8 张表）
- ✅ 后端启动时间 3.9 秒，API 响应时间 5.9ms
- ✅ 前端架构搭建完成（Vue 3 + TypeScript + Vite）

---

## Phase 2: 基础设施层（阻塞前置条件）✅ **已验收**

**目标**：实现 ModelGateway、Citations 结构、认证授权等所有用户故事依赖的核心组件
**验收日期**: 2025-12-29
**验收评分**: ⭐⭐⭐⭐⭐ 97.1%

### 2.1 ModelGateway 统一模型调用入口

- [x] T011 创建 `backend/src/main/java/com/aetheris/rag/gateway/ModelGateway.java` 接口，定义 embed() 和 chat() 方法签名
- [~] T012 创建 `backend/src/main/java/com/aetheris/rag/gateway/EmbeddingGateway.java`，实现智谱 AI Embedding API 调用
  - **状态**: ⚠️ **Stub 实现** - 仅创建框架，返回 dummy embedding，完整实现推迟到 Phase 5
  - **原因**: langchain4j-zhipu-ai 包依赖问题，需要在 Phase 5 (RAG Q&A) 时完整实现
  - **目标**：封装 Embedding 调用，实现文本哈希缓存、超时/重试/限流、日志脱敏
  - **涉及表**：无（调用外部 API）
  - **Redis**：`embedding:cache:{textHash}` 存储 embedding 向量，TTL 30 天
  - **配置项**：`embedding.modelName`（embedding-v2）、`embedding.timeout`（30s）、`embedding.maxRetries`（3）、`embedding.rateLimit`（5 req/s）
  - **API 端点**：无（内部服务）
  - **测试要点**：单测验证缓存命中、重试策略、超时处理、日志脱敏
  - **验收标准**：FR-007（Embedding 缓存）、FR-009（入库幂等）、宪章三（缓存与幂等）
  - **Phase 5 任务**: 完整实现 Zhipu AI Embedding API 调用、缓存逻辑、重试策略
- [~] T013 创建 `backend/src/main/java/com/aetheris/rag/gateway/ChatGateway.java`，实现智谱 AI Chat API 调用
  - **状态**: ⚠️ **Stub 实现** - 仅创建框架，返回 dummy response，完整实现推迟到 Phase 5
  - **原因**: langchain4j-zhipu-ai 包依赖问题，需要在 Phase 5 (RAG Q&A) 时完整实现
  - **目标**：封装 LLM 调用，实现 Prompt 构建、超时/重试/限流、降级处理、日志脱敏
  - **涉及表**：无（调用外部 API）
  - **配置项**：`chat.modelName`（glm-4-flash）、`chat.temperature`（0.7）、`chat.topP`（0.9）、`chat.maxTokens`（2000）、`chat.timeout`（60s）
  - **测试要点**：单测验证降级策略（LLM 不可用时返回检索结果+引用摘要）
  - **验收标准**：FR-015（证据不足时的降级返回）
  - **Phase 5 任务**: 完整实现 Zhipu AI Chat API 调用、Prompt 构建、降级策略
- [x] T014 [P] 创建 `backend/src/main/java/com/aetheris/rag/gateway/cache/EmbeddingCache.java`，实现基于文本哈希的 Redis 缓存
- [x] T015 [P] 创建 `backend/src/main/java/com/aetheris/rag/gateway/retry/ModelRetryStrategy.java`，实现 429/5xx 重试策略（指数退避）
- [x] T016 [P] 创建 `backend/src/main/java/com/aetheris/rag/gateway/sanitize/LogSanitizer.java`，实现日志脱敏（截断 200 字符，不记录 API key）

### 2.2 Citations 统一结构规范

- [x] T017 创建 `backend/src/main/java/com/aetheris/rag/dto/response/Citation.java`，定义引用结构
  - **目标**：定义统一的 Citations JSON 结构，支持 /api/chat/ask 和 /api/recommendations 复用
  - **涉及表**：关联 resources（resource_id, title）、resource_chunks（chunk_id, chunk_index, location_info）
  - **字段**：resourceId、resourceTitle、chunkId、chunkIndex、location（PDF pageStart/pageEnd，MD chapterPath）、snippet（100-200 字符）、score（相似度分数）
  - **测试要点**：单测验证序列化为 JSON、字段完整性、location 格式正确
  - **验收标准**：FR-014、FR-020（Citations 结构规范）、SC-005（100% 可定位）

### 2.3 用户认证与授权

- [x] T018 创建 `backend/src/main/java/com/aetheris/rag/model/User.java`，使用 Lombok @Data、@Builder 注解
  - **涉及表**：users（id, username, email, password_hash, created_at, updated_at, last_active_at）
- [x] T019 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/UserMapper.java` 接口和 `UserMapper.xml`
  - **SQL**：INSERT、SELECT by email/username/id、UPDATE last_active_at
- [x] T020 创建 `backend/src/main/java/com/aetheris/rag/service/auth/AuthService.java` 接口和实现 `AuthServiceImpl.java`
  - **目标**：实现注册（邮箱唯一性检查、密码 BCrypt 哈希）、登录（JWT token 生成）、密码校验
  - **涉及表**：users
  - **API 端点**：POST /api/auth/register、POST /api/auth/login
  - **测试要点**：单测验证 BCrypt 哈希、JWT 生成、邮箱重复检查
  - **验收标准**：FR-001（注册登录）、FR-002（用户信息维护）
- [x] T021 [P] 创建 `backend/src/main/java/com/aetheris/rag/controller/AuthController.java`，实现注册和登录端点
- [x] T022 [P] 创建 `backend/src/main/java/com/aetheris/rag/config/SecurityConfig.java`，配置 JWT 过滤器（验证 Authorization 头）

### 2.4 基础工具类

- [x] T023 [P] 创建 `backend/src/main/java/com/aetheris/rag/util/HashUtil.java`，实现 SHA-256 文本哈希计算
- [x] T024 [P] 创建 `backend/src/main/java/com/aetheris/rag/util/TextNormalizer.java`，实现文本规范化（去除冗余空白、统一换行）
- [x] T025 [P] 创建 `backend/src/main/java/com/aetheris/rag/util/PerformanceTimer.java`，实现分阶段耗时记录（解析、Embedding、检索、生成）

**验收标准**：✅ **全部通过**
- [x] ModelGateway 单测通过，缓存命中率达 80%+
- [x] Citations 结构符合 OpenAPI 规范
- [x] 用户注册/登录 API 测试通过（curl 或 Postman）
- [x] JWT token 可正确验证，过期时间合理

**验收备注**：
- ✅ ModelGateway 框架搭建完成（EmbeddingGateway、ChatGateway stub 实现）
- ✅ EmbeddingCache、ModelRetryStrategy、LogSanitizer 工具类实现完成
- ✅ Citations 统一结构定义（Citation.java 207 行、CitationLocation.java 168 行）
- ✅ 用户认证系统实现（JWT + BCrypt + Spring Security）
- ✅ 工具类库实现完成（HashUtil、TextNormalizer、PerformanceTimer）
- ✅ 测试覆盖率 39.2%（78 个测试方法）
- ✅ 代码质量优秀（2426 行代码，106 个 Javadoc）
- ⚠️ T012、T013 为 stub 实现，完整实现推迟到 Phase 5

---

## Phase 3: 用户故事 4 - 用户账户与行为记录 (P1) 🎯 MVP

**目标**：用户可以注册登录，系统记录查询和点击/收藏行为

**验收日期**: 2025-12-30
**验收评分**: 待验收

**独立测试**：用户注册登录后，在问答界面输入问题，检查 `user_behaviors` 表有记录

- [x] T026 创建 `backend/src/main/java/com/aetheris/rag/model/UserBehavior.java`，使用 Lombok 注解
  - **涉及表**：user_behaviors（id, user_id, behavior_type, resource_id, query_text, behavior_time, session_id）
- [x] T027 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/UserBehaviorMapper.java` 接口和 `UserBehaviorMapper.xml`
  - **SQL**：INSERT、SELECT recent queries、SELECT by time range
- [x] T028 创建 `backend/src/main/java/com/aetheris/rag/service/BehaviorService.java`（无接口，简单实现）
  - **目标**：记录查询行为（FR-003）、记录点击/收藏行为（FR-004）
  - **涉及表**：user_behaviors
  - **API 端点**：POST /api/behaviors（内部调用，无需暴露）
  - **测试要点**：单测验证行为记录、时间戳自动填充
  - **验收标准**：FR-003、FR-004、US4 验收场景 2/3
- [x] T029 [P] 创建 `backend/src/main/java/com/aetheris/rag/controller/BehaviorController.java`，提供 GET /api/behaviors/recent 查询最近行为（用于个人中心）

**前端任务（可并行）**：

- [x] T030 [P] 创建 `frontend/src/views/auth/LoginView.vue`，使用 Ant Design Vue 表单组件
- [x] T031 [P] 创建 `frontend/src/views/auth/RegisterView.vue`，实现注册表单验证（邮箱格式、密码长度）
- [x] T032 [P] 创建 `frontend/src/services/auth.service.ts`，封装注册和登录 API 调用（Axios + JWT token 存储）
- [x] T033 创建 `frontend/src/stores/user.ts`，使用 Pinia 管理用户登录状态
- [x] T034 [P] 创建 `frontend/src/router/index.ts`，定义路由（登录、注册、主页），添加路由守卫（未登录跳转登录页）

**验收标准**：
- [ ] 用户注册功能正常，可创建新账号
- [ ] 用户登录功能正常，返回有效 JWT token
- [ ] 前端登录状态正常，Pinia store 正确管理用户信息
- [ ] 路由守卫生效，未登录自动跳转到登录页
- [ ] 查询行为自动记录到 `user_behaviors` 表
- [ ] 点击/收藏行为自动记录到 `user_behaviors` 表
- [ ] 行为记录包含正确的 user_id、behavior_type、query_text、behavior_time
- [ ] GET /api/behaviors/recent 可查询最近行为记录

**Checkpoint**：用户可注册登录，行为记录正常写入数据库（待用户验收）

**验收备注**：
- ✅ UserBehavior 实体类实现完成（包含 BehaviorType 枚举）
- ✅ UserBehaviorMapper 接口和 XML 实现（包含 5 个 SQL 方法）
- ✅ BehaviorService 服务实现完成（包含 6 个业务方法）
- ✅ BehaviorController 控制器实现完成（包含 4 个 API 端点）
- ✅ 前端 LoginView.vue 登录页面实现（Ant Design Vue 表单）
- ✅ 前端 RegisterView.vue 注册页面实现（包含表单验证）
- ✅ auth.service.ts 认证服务实现（Axios + JWT token 管理）
- ✅ user.ts Pinia store 实现（用户状态管理）
- ✅ router/index.ts 路由配置实现（包含路由守卫）
- ✅ 单元测试 BehaviorServiceTest（8 个测试方法）
- ✅ 集成测试 UserBehaviorIntegrationTest（4 个测试方法）
- ⚠️ 待用户验收前端功能和 API 测试

---

## Phase 4: 用户故事 1 - 资源入库与可追溯切片 (P1) 🎯 MVP

**目标**：用户上传 PDF/Markdown 文档，系统自动提取文本、切片、向量化入库

**独立测试**：上传测试文档，在资源列表查看元数据和切片信息（chunkIndex、页码/章节）

- [ ] T035 创建 `backend/src/main/java/com/aetheris/rag/model/Resource.java` 和 `Chunk.java`，使用 Lombok 注解
  - **涉及表**：resources（id, title, tags, file_type, file_path, file_size, description, content_hash, uploaded_by, chunk_count, vectorized）
  - **涉及表**：resource_chunks（id, resource_id, chunk_index, chunk_text, location_info, page_start, page_end, chapter_path, text_hash, vectorized）
  - **注**：使用 Lombok @Data、@Builder 注解，不使用 Java 21 Record
- [ ] T036 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/ResourceMapper.java` 接口和 `ResourceMapper.xml`
  - **SQL**：INSERT（useGeneratedKeys）、SELECT by id/content_hash/uploader、UPDATE chunk_status、SELECT paged
- [ ] T037 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/ChunkMapper.java` 接口和 `ChunkMapper.xml`
  - **SQL**：INSERT、SELECT by resource_id、SELECT by text_hash、SELECT unvectorized、UPDATE vectorized、DELETE by resource_id
- [ ] T038 创建 `backend/src/main/java/com/aetheris/rag/service/ResourceService.java` 接口和实现 `ResourceServiceImpl.java`
  - **目标**：实现资源上传、内容哈希去重（FR-007 幂等性）、切片管理、向量化状态跟踪
  - **涉及表**：resources、resource_chunks
  - **Redis**：`resource:upload:{contentHash}` 分布式锁，防止并发重复上传
  - **API 端点**：POST /api/resources（上传）、GET /api/resources（列表）、GET /api/resources/{id}（详情）
  - **关键配置**：`chunk.size`（1000 字符）、`chunk.overlap`（200 字符）
  - **测试要点**：单测验证内容哈希去重、切片逻辑、幂等性（重复上传返回已存在资源）
  - **验收标准**：FR-005/006/007/008/009、US1 验收场景 1/2/3、SC-001（入库完成并记录耗时）
- [ ] T039 [P] 创建 `backend/src/main/java/com/aetheris/rag/service/impl/PdfProcessor.java`，实现 PDF 文本提取和页码范围记录
  - **目标**：使用 Apache PDFBox 逐页提取文本，记录每个 chunk 的 pageStart 和 pageEnd
  - **涉及字段**：resource_chunks.page_start、page_end
  - **测试要点**：单测验证页码范围计算正确、文本提取完整
  - **验收标准**：FR-008（PDF 切片保存页码范围）
- [ ] T040 [P] 创建 `backend/src/main/java/com/aetheris/rag/service/impl/MarkdownProcessor.java`，实现 Markdown AST 解析和章节路径记录
  - **目标**：使用 CommonMark Java 解析 heading 层级，记录 chapterPath（如 "第一章>1.1节"）
  - **涉及字段**：resource_chunks.chapter_path
  - **测试要点**：单测验证章节路径格式正确
  - **验收标准**：FR-008（Markdown 切片保存章节路径）
- [ ] T041 创建 `backend/src/main/java/com/aetheris/rag/service/VectorService.java`（无接口），实现向量化批量处理
  - **目标**：批量读取未向量化的切片，调用 ModelGateway.embed()，写入 Redis 向量索引
  - **涉及表**：resource_chunks（查询 vectorized=FALSE）、更新 vectorized=TRUE
  - **Redis**：RediSearch Vector KNN 索引，key 为 `chunk:{chunkId}`，字段包括 vector、resourceId、chunkId、chunkIndex、tags、docType
  - **向量索引配置**：
    - 索引名：`chunk_vector_index`
    - 向量维度：1024（智谱 embedding-v2）
    - 距离算法：COSINE
    - HNSW 参数：M=16、ef_construction=128
  - **测试要点**：集成测试使用 Testcontainers 验证 Redis 向量写入和检索
  - **验收标准**：FR-009（向量化入库）
- [ ] T042 [P] 创建 `backend/src/main/java/com/aetheris/rag/controller/ResourceController.java`，实现上传、列表、详情端点
- [ ] T043 [P] 创建 `backend/src/main/resources/mapper/ResourceMapper.xml` 和 `ChunkMapper.xml`，定义 SQL 语句（符合 Google Java Style）

**前端任务（可并行）**：

- [ ] T044 [P] 创建 `frontend/src/views/resource/UploadView.vue`，使用 Ant Design Upload 组件，支持拖拽上传
- [ ] T045 [P] 创建 `frontend/src/views/resource/ResourceListView.vue`，使用 Ant Design Table 组件展示资源列表
- [ ] T046 [P] 创建 `frontend/src/views/resource/ResourceDetailView.vue`，展示资源元数据和切片列表（chunkIndex、页码/章节、文本预览）
- [ ] T047 [P] 创建 `frontend/src/services/resource.service.ts`，封装资源 API 调用
- [ ] T048 创建 `frontend/src/components/resource/ResourceCard.vue` 和 `ResourceTable.vue`，可复用组件

**验收标准**：
- [ ] 用户可成功上传 PDF 文档
- [ ] 用户可成功上传 Markdown 文档
- [ ] 系统自动提取文本内容
- [ ] 文档自动切片，chunk 索引连续且无重叠错误
- [ ] PDF 切片包含正确的 page_start 和 page_end
- [ ] Markdown 切片包含正确的 chapter_path（如 "第一章>1.1节"）
- [ ] 切片向量化成功，resource_chunks.vectorized 标记为 TRUE
- [ ] 资源列表显示元数据（标题、类型、大小、上传时间、chunk 数量）
- [ ] 资源详情页显示切片列表（chunkIndex、页码/章节、文本预览）
- [ ] 内容哈希去重生效，相同内容不重复入库
- [ ] 向量化入库记录分段耗时（解析、切片、向量化）
- [ ] P95 入库耗时 ≤ 30 秒

**Checkpoint**：用户可上传文档，查看切片信息，切片已向量化

---

## Phase 5: 用户故事 2 - 语义检索与 RAG 问答 (P1) 🎯 MVP

**目标**：用户输入自然语言问题，系统返回带引用来源的答案

**独立测试**：输入问题"什么是 RAG？"，验证返回答案包含 citations（resourceId、chunkId、chunkIndex、页码/范围、snippet）

### Phase 5.0: 完成 ModelGateway Stub 实现 (阻塞前置条件)

- [ ] **T012-FULL** 完整实现 `EmbeddingGateway.java`（当前为 stub）
  - **当前状态**: Stub 实现，返回 dummy embedding
  - **需要完成**:
    - 修复 langchain4j-zhipu-ai 依赖配置
    - 实现 Zhipu AI Embedding API 调用
    - 集成 EmbeddingCache 缓存逻辑
    - 集成 ModelRetryStrategy 重试策略
    - 添加日志脱敏
  - **测试要点**：单测验证缓存命中、重试策略、超时处理、日志脱敏
  - **验收标准**：FR-007（Embedding 缓存）、FR-009（入库幂等）

- [ ] **T013-FULL** 完整实现 `ChatGateway.java`（当前为 stub）
  - **当前状态**: Stub 实现，返回 dummy response
  - **需要完成**:
    - 修复 langchain4j-zhipu-ai 依赖配置
    - 实现 Zhipu AI Chat API 调用
    - 集成 ModelRetryStrategy 重试策略
    - 实现降级策略（LLM 不可用时的处理）
    - 添加日志脱敏
  - **测试要点**：单测验证降级策略、Prompt 构建
  - **验收标准**：FR-015（证据不足时的降级返回）

### Phase 5.1: 语义检索服务

- [ ] T049 创建 `backend/src/main/java/com/aetheris/rag/service/SearchService.java` 接口和实现 `SearchServiceImpl.java`
  - **目标**：实现语义检索，查询 Redis 向量索引，返回 Top-K 切片（FR-010），按资源聚合（FR-011）
  - **涉及表**：resource_chunks（关联获取元数据）、resources（标题、标签）
  - **Redis**：
    - 向量检索：`FT.SEARCH chunk_vector_index VECTOR ... KNN {topK} $query_vector`
    - 结果缓存：`search:cache:{queryHash}:{topK}` TTL 1 小时
  - **关键配置**：`retrieval.topK`（默认 5）、`retrieval.scoreThreshold`（0.5，低于阈值返回空）
  - **测试要点**：单测验证向量检索、资源聚合逻辑（按 resourceId 合并相似度分数）
  - **验收标准**：FR-010（Top-K 检索）、FR-011（聚合到资源级别）
- [ ] T050 创建 `backend/src/main/java/com/aetheris/rag/service/RagService.java` 接口和实现 `RagServiceImpl.java`
  - **目标**：实现 RAG 问答流程（FR-013）：检索 → Prompt 构建 → LLM 生成 → Citations 提取
  - **涉及表**：resources、resource_chunks
  - **Redis**：使用 SearchService 的向量检索
  - **API 端点**：POST /api/chat/ask
  - **关键配置**：`chat.temperature`、`chat.maxTokens`、`retrieval.topK`
  - **Prompt 模板**：
    ```
    基于以下学习资源片段回答问题。如果证据不足，明确说明"根据现有资料无法完整回答"。

    问题：{query}

    相关资料：
    {evidence_chunks}

    要求：
    1. 答案必须严格基于上述资料
    2. 每个关键论断标注引用来源 [资源标题, 页码/章节]
    3. 如果资料不足，说明"建议查阅其他资源"
    ```
  - **测试要点**：
    - 单测验证 Prompt 构建逻辑、Citations 提取（正则或结构化解析）
    - 集成测试验证 LLM 不可用时的降级策略（返回 TopK 检索结果+引用摘要）
  - **验收标准**：FR-013（RAG 问答）、FR-014（Citations）、FR-015（证据不足降级）、US2 验收场景 1/2/3、SC-002（返回结果包含至少 1 条可验证引用）
- [ ] T051 [P] 创建 `backend/src/main/java/com/aetheris/rag/dto/request/AskRequest.java` 和 `dto/response/AnswerResponse.java`
  - **AnswerResponse 字段**：answer（String）、citations（List<Citation>）、evidenceInsufficient（boolean）、fallbackResources（List<ResourceBrief>）、latencyMs（long）
- [ ] T052 创建 `backend/src/main/java/com/aetheris/rag/controller/ChatController.java`，实现 POST /api/chat/ask 端点
- [ ] T053 [P] 在 RagService 中集成 BehaviorService，记录查询行为（异步写入 user_behaviors 表）
- [ ] T054 [P] 在 SearchService 中集成 PerformanceTimer，记录检索耗时（用于 SC-006 性能统计）

**前端任务（可并行）**：

- [ ] T055 [P] 创建 `frontend/src/views/chat/ChatView.vue`，使用 Ant Design Input + Button（问答输入框）
- [ ] T056 [P] 创建 `frontend/src/components/chat/AnswerDisplay.vue`，展示答案文本和引用卡片
- [ ] T057 [P] 创建 `frontend/src/components/chat/CitationCard.vue`，使用 Ant Design Card 展示引用（资源标题、页码/范围、snippet、可点击跳转资源详情）
- [ ] T058 创建 `frontend/src/services/chat.service.ts`，封装问答 API 调用
- [ ] T059 创建 `frontend/src/composables/useChat.ts`，封装问答逻辑（加载状态、错误处理、行为记录触发）

**验收标准**：
- [ ] 用户可输入问题并提交问答请求
- [ ] 系统返回答案文本，答案基于检索到的资源内容
- [ ] 每个答案包含 citations 列表，每条引用包含：
  - [ ] resourceId、resourceTitle、chunkId、chunkIndex
  - [ ] location（PDF 页码范围或 Markdown 章节路径）
  - [ ] snippet（100-200 字符的证据片段）
  - [ ] score（相似度分数）
- [ ] citations 可点击，点击后跳转到资源详情页
- [ ] 点击引用可高亮显示对应的 chunk
- [ ] 检索结果包含 Top-K 相关切片（默认 K=5）
- [ ] 检索结果按资源聚合，同一资源的多个 chunk 合并分数
- [ ] 查询行为自动记录到 user_behaviors 表
- [ ] LLM 不可用时，返回降级结果（检索结果 + 证据摘要 + 错误提示）
- [ ] P95 问答响应时间 ≤ 5 秒
- [ ] 每次请求记录分段耗时（检索、生成）

**Checkpoint**：用户可问答，答案包含可点击的引用卡片，点击跳转到资源详情并高亮对应 chunk

---

## Phase 6: 用户故事 3 - 个性化推荐 (P2) 增强功能

**目标**：基于用户最近 N 次查询构建画像，推荐 Top-N 资源，每条推荐附带理由和证据引用

**独立测试**：模拟用户查询 5 次关于"深度学习"的问题，验证推荐列表包含相关资源和推荐理由

### 6.1 用户画像（MVP：仅基于查询行为）

- [ ] T060 创建 `backend/src/main/java/com/aetheris/rag/model/UserProfile.java`，使用 Lombok 注解
  - **涉及表**：user_profiles（user_id、profile_vector、window_size、query_count、click_count、favorite_count、updated_at）
- [ ] T061 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/UserProfileMapper.java` 接口和 `UserProfileMapper.xml`
  - **SQL**：UPSERT（INSERT ... ON DUPLICATE KEY UPDATE）、SELECT by user_id、UPDATE query_count/click_count
- [ ] T062 创建 `backend/src/main/java/com/aetheris/rag/service/UserProfileService.java` 接口和实现 `UserProfileServiceImpl.java`
  - **目标**：实现轻量画像（FR-016），基于最近 N 次查询的滑动平均（MVP）
  - **涉及表**：user_behaviors（查询最近 N 次）、user_profiles
  - **画像计算逻辑**：
    1. 查询最近 N 次查询文本（默认 N=10，可配置 `profile.windowSize`）
    2. 对每次查询调用 ModelGateway.embed() 获取向量
    3. 计算平均向量（逐维度求和后除以 N）
    4. 更新 user_profiles.profile_vector（JSON 数组）
  - **关键配置**：`profile.windowSize`（10）、`profile.updateTrigger`（每次查询后更新）
  - **测试要点**：单测验证滑动平均计算、边界情况（新用户、行为不足 N 次）
  - **验收标准**：FR-016（轻量画像：基于最近 N 次查询）
- [ ] T063 [P] 在 ResourceService 上传资源后，异步触发 UserProfileService 更新（预热画像）

### 6.2 推荐服务

- [ ] T064 创建 `backend/src/main/java/com/aetheris/rag/service/RecommendationService.java` 接口和实现 `RecommendationServiceImpl.java`
  - **目标**：实现个性化推荐（FR-018），使用画像向量召回候选切片，聚合到资源级别（FR-011）
  - **涉及表**：user_profiles、resources、resource_chunks
  - **Redis**：
    - 有画像：`FT.SEARCH chunk_vector_index VECTOR ... KNN {topN} $profile_vector`
    - 无画像：`FT.SEARCH chunk_vector_index ... SORT BY upload_time DESC`（基于热度的默认推荐）
  - **API 端点**：GET /api/recommendations?topN=10
  - **推荐理由生成**：
    - 有画像：调用 ChatGateway，Prompt 模板：
      ```
      用户最近的查询兴趣：{recent_queries}

      推荐资源：{resource_title}
      相关证据：
      {evidence_chunks}

      请生成：
      1. 推荐理由（1-2 句话，说明为何推荐）
      2. 学习建议（建议先学习哪章哪节）
      ```
    - 无画像：返回固定理由"热门学习资源" + 默认学习建议
  - **关键配置**：`recommendation.topN`（10）、`profile.enabled`（true，可关闭画像降级为非个性化）
  - **测试要点**：
    - 单测验证资源聚合逻辑（同一资源的多个 chunk 合并，取最高相似度分数）
    - 单测验证推荐理由生成（有画像 vs 无画像）
    - 集成测试验证新用户默认推荐（基于热度）
  - **验收标准**：FR-018（Top-N 推荐）、FR-019（推荐理由和学习建议）、FR-020（证据引用）、US3 验收场景 1/2/3、SC-004（对比无画像 vs 有画像）
- [ ] T065 [P] 创建 `backend/src/main/java/com/aetheris/rag/dto/response/RecommendationResponse.java` 和 `RecommendationItem.java`
  - **RecommendationItem 字段**：resourceId、title、tags、reason（推荐理由）、suggestion（学习建议）、citations（List<Citation>）、score（相似度分数）
- [ ] T066 创建 `backend/src/main/java/com/aetheris/rag/controller/RecommendationController.java`，实现 GET /api/recommendations 端点
- [ ] T067 [P] 在 RecommendationService 中集成 BehaviorService，记录推荐点击行为（异步写入 user_behaviors 表）

### 6.3 可选增强：点击/收藏权重（P2 优先级最低）

- [ ] T068 在 UserProfileService 中添加加权平均算法（FR-017），查询权重=1.0、点击权重=2.0、收藏权重=3.0（可配置 `profile.queryWeight`、`profile.clickWeight`、`profile.favoriteWeight`）
  - **目标**：在画像计算中加入交互行为权重，提升推荐准确性
  - **涉及表**：user_behaviors（WHERE behavior_type IN ('QUERY', 'CLICK', 'FAVORITE')）
  - **关键配置**：`profile.clickWeight`（2.0）、`profile.favoriteWeight`（3.0）
  - **测试要点**：单测验证加权平均计算
  - **验收标准**：FR-017（可选增强）

**前端任务（可并行）**：

- [ ] T069 [P] 创建 `frontend/src/views/recommendation/RecommendationView.vue`，使用 Ant Design List 展示推荐列表
- [ ] T070 [P] 创建 `frontend/src/components/recommendation/RecommendationCard.vue`，展示推荐理由、学习建议、引用证据、收藏按钮
- [ ] T071 创建 `frontend/src/services/recommendation.service.ts`，封装推荐 API 调用
- [ ] T072 创建 `frontend/src/composables/useRecommendation.ts`，封装推荐逻辑（点击推荐资源记录行为）

**验收标准**：
- [ ] 用户可访问推荐页面，查看 Top-N 推荐列表（默认 N=10）
- [ ] 每条推荐包含：
  - [ ] resourceId、title、tags
  - [ ] reason（推荐理由，1-2 句话说明为何推荐）
  - [ ] suggestion（学习建议，建议学习章节）
  - [ ] citations（List<Citation>，证据引用）
  - [ ] score（相似度分数）
- [ ] 画像为空时（新用户），返回基于热度的默认推荐
- [ ] 画像不为空时，返回基于最近 N 次查询的个性化推荐
- [ ] 查询行为自动更新用户画像
- [ ] 点击推荐资源自动记录行为并更新画像
- [ ] 画像计算基于最近 N 次查询的 embedding 向量滑动平均
- [ ] 推荐按相似度分数排序
- [ ] P95 推荐生成时间 ≤ 2 秒
- [ ] 有画像 vs 无画像的 Precision@10 提升可量化（可运行对比评测）

**Checkpoint**：用户可查看个性化推荐，推荐包含理由和学习建议，点击推荐后画像更新

---

## Phase 7: 离线评测与性能分析 (P2) 增强功能

**目标**：支持离线测试集（JSON/YAML），运行评测，计算 Precision@K、Recall@K、平均/P95 时延，对比有画像 vs 无画像

**独立测试**：准备 `test_dataset.json`（10 个查询），运行评测，生成报告

- [ ] T073 创建 `backend/src/main/resources/test_dataset.json`，定义测试数据集格式
  ```json
  {
    "queries": [
      {
        "queryId": "q001",
        "queryText": "什么是 RAG？",
        "relevantResources": ["resource-uuid-001", "resource-uuid-005"]
      },
      ...
    ]
  }
  ```
- [ ] T074 创建 `backend/src/main/java/com/aetheris/rag/model/EvalQuery.java` 和 `EvalRun.java`，使用 Lombok 注解
  - **涉及表**：eval_queries（query_id、query_text、relevant_resources）、eval_runs（run_name、config、use_profile、metrics）
- [ ] T075 [P] 创建 `backend/src/main/java/com/aetheris/rag/mapper/EvalMapper.java` 接口和 `EvalMapper.xml`
  - **SQL**：INSERT query、INSERT run、SELECT all queries
- [ ] T076 创建 `backend/src/main/java/com/aetheris/rag/service/EvalService.java` 接口和实现 `EvalServiceImpl.java`
  - **目标**：实现离线评测（FR-021~FR-025），加载测试数据集、运行检索/推荐、计算指标、输出报告
  - **涉及表**：eval_queries、eval_runs
  - **Redis**：使用 SearchService 和 RecommendationService 的向量检索
  - **API 端点**：POST /api/eval/run（提交评测任务，返回 runId）、GET /api/eval/runs/{runId}（查询报告）
  - **关键配置**：从 application.yml 读取当前配置（chunkSize、overlap、topK、embeddingModel、chatModel）
  - **指标计算**：
    - Precision@K = (Top-K 结果中的相关资源数量) / K
    - Recall@K = (Top-K 结果中的相关资源数量) / (测试集中标注的相关资源总数)
    - 平均响应时间 = 所有请求响应时间的总和 / 请求总数
    - P95 响应时间 = 第 95 百分位响应时间
  - **对比实验**：
    1. 运行无画像评测：`useProfile=false`
    2. 运行有画像评测：`useProfile=true`
    3. 计算指标差异：`Precision 提升 = (Precision_有画像 - Precision_无画像) / Precision_无画像 × 100%`
  - **报告格式**（JSON + Markdown）：
    ```json
    {
      "runId": "run-uuid-001",
      "runName": "eval-baseline-topk5",
      "config": {
        "chunkSize": 1000,
        "overlap": 200,
        "topK": 5,
        "embeddingModel": "embedding-v2",
        "chatModel": "glm-4-flash"
      },
      "useProfile": false,
      "metrics": {
        "precision@5": 0.65,
        "recall@10": 0.72,
        "avgLatencyMs": 2300,
        "p95LatencyMs": 3800
      },
      "runTime": "2025-12-26T10:30:00Z",
      "comparison": {
        "baselinePrecision": 0.60,
        "profiledPrecision": 0.65,
        "improvement": "+8.33%"
      }
    }
    ```
  - **测试要点**：
    - 单测验证指标计算公式（Precision@K、Recall@K）
    - 单测验证 P95 计算逻辑
    - 集成测试验证完整评测流程（使用 Testcontainers + 小型测试集）
  - **验收标准**：FR-021（离线测试集）、FR-022（Precision@K、Recall@K）、FR-023（对比无画像 vs 有画像）、FR-024（平均/P95 时延）、FR-025（可重复参数对比）、SC-003（输出指标）、SC-004（对比指标）、SC-006（基础性能统计）
- [ ] T077 [P] 创建 `backend/src/main/java/com/aetheris/rag/controller/EvalController.java`，实现 POST /api/eval/run、GET /api/eval/runs、GET /api/eval/runs/{runId}/report（下载 Markdown 报告）
- [ ] T078 [P] 创建 CLI 入口 `backend/src/main/java/com/aetheris/rag/cli/EvalCli.java`，支持命令行运行评测
  ```bash
  java -jar backend.jar eval --dataset test_dataset.json --run-name eval-baseline
  ```
- [ ] T079 在所有服务中集成 PerformanceTimer，记录分阶段耗时（解析、Embedding、检索、生成），用于 SC-006 性能统计

**前端任务（可选）**：

- [ ] T080 [P] 创建 `frontend/src/views/eval/EvalReportView.vue`，展示评测历史列表和报告详情（图表展示 Precision@K、Recall@K、时延分布）

**验收标准**：
- [ ] 可加载离线测试集（JSON 格式，包含 queryId、queryText、relevantResources）
- [ ] 可运行评测任务，返回 runId
- [ ] 评测计算以下指标：
  - [ ] Precision@K（Top-K 结果中相关资源的比例）
  - [ ] Recall@K（Top-K 结果中的相关资源占所有相关资源的比例）
  - [ ] 平均响应时间
  - [ ] P95 响应时间
- [ ] 可对比无画像 vs 有画像的性能差异
- [ ] 评测记录参数配置（chunkSize、overlap、topK、modelName 等）
- [ ] 生成可复现的评测报告（JSON + Markdown）
- [ ] 支持命令行运行评测：`java -jar backend.jar eval --dataset test_dataset.json --run-name eval-baseline`
- [ ] 所有核心链路记录分段耗时（解析、Embedding、检索、生成）
- [ ] 性能指标输出到日志和监控系统
- [ ] 超出性能预算阈值时记录警告日志

**Checkpoint**：可运行离线评测，生成可复现报告，对比有画像 vs 无画像性能

---

## Phase 8: 完善与跨用户故事优化

**目标**：补充遗漏功能、优化性能、完善文档、全面测试

### 8.1 补充遗漏功能

- [ ] T081 创建 `frontend/src/views/profile/ProfileView.vue`，展示用户信息（用户名、邮箱）、最近行为历史（查询记录、点击/收藏记录）
- [ ] T082 [P] 创建 `frontend/src/components/layout/AppHeader.vue`、`AppSidebar.vue`、`AppFooter.vue`，统一应用布局
- [ ] T083 [P] 创建 `backend/src/main/java/com/aetheris/rag/config/CorsConfig.java`，配置跨域支持（开发环境）
- [ ] T084 [P] 创建 `backend/src/main/java/com/aetheris/rag/exception/GlobalExceptionHandler.java`，统一异常处理（ModelException、ValidationException、一般异常）

### 8.2 性能优化

- [ ] T085 在 ModelGateway 中添加批量 Embedding 支持（batch_embed()），减少 API 调用次数
- [ ] T086 在 SearchService 中实现查询结果缓存（Redis，TTL 1 小时），key 格式：`search:cache:{queryHash}:{topK}`
- [ ] T087 在 ResourceService 中实现分页查询优化（避免一次加载所有资源），使用 MySQL LIMIT offset, limit

### 8.3 测试完善

- [ ] T088 创建 `backend/src/test/unit/DocumentProcessorTest.java`，单测 PDF 和 Markdown 处理逻辑
- [ ] T089 创建 `backend/src/test/unit/EmbeddingCacheTest.java`，单测 Embedding 缓存命中/失效
- [ ] T090 创建 `backend/src/test/unit/SearchServiceTest.java`，单测 TopK 聚合逻辑
- [ ] T091 创建 `backend/src/test/integration/ResourceIngestionTest.java`，集成测试完整入库流程（使用 Testcontainers）
- [ ] T092 创建 `backend/src/test/integration/RagChatTest.java`，集成测试问答端到端流程
- [ ] T093 创建 `backend/src/test/integration/RecommendationTest.java`，集成测试推荐端到端流程
- [ ] T094 创建 `backend/src/test/eval/EvalServiceTest.java`，评测服务测试（使用小型测试集）

### 8.4 文档与部署

- [ ] T095 更新 `README.md`，添加项目介绍、技术栈、快速开始、API 文档链接
- [ ] T096 [P] 更新 `quickstart.md`，补充常见问题排查（连接失败、API 超时、向量化失败）
- [ ] T097 [P] 创建 `backend/Dockerfile`，支持后端 Docker 镜像构建
- [ ] T098 [P] 创建 `frontend/Dockerfile`，支持前端 Docker 镜像构建（多阶段构建：npm run build → Nginx）
- [ ] T099 [P] 创建 `docker-compose.prod.yml`，生产环境部署配置（MySQL、Redis Stack、后端、前端）

**验收标准**：
- [ ] 用户个人中心页面正常展示用户信息（用户名、邮箱）和行为历史（查询、点击、收藏）
- [ ] 应用布局组件（Header、Sidebar、Footer）样式统一，导航功能正常
- [ ] CORS 配置生效，前端可正常调用后端 API（无跨域错误）
- [ ] 全局异常处理生效，所有异常返回统一 JSON 格式（含错误码、消息）
  - [ ] ModelException 返回 502 状态码
  - [ ] ValidationException 返回 400 状态码
  - [ ] 一般异常返回 500 状态码
- [ ] 批量 Embedding 功能正常，可一次处理多个文本块
- [ ] 搜索结果缓存生效，相同查询直接返回缓存结果（P95 < 50ms）
- [ ] 资源分页查询正常，避免一次加载所有资源
- [ ] 单元测试覆盖率 ≥ 70%（涵盖核心业务逻辑）
  - [ ] DocumentProcessor 单测通过（PDF/Markdown 解析）
  - [ ] EmbeddingCache 单测通过（缓存命中/失效）
  - [ ] SearchService 单测通过（TopK 聚合）
- [ ] 集成测试通过（使用 Testcontainers）
  - [ ] 资源入库端到端测试通过
  - [ ] RAG 问答端到端测试通过
  - [ ] 推荐端到端测试通过
- [ ] 评测服务测试通过（使用小型测试集）
- [ ] README.md 包含完整的项目介绍、技术栈、快速开始指南
- [ ] quickstart.md 包含常见问题排查（连接失败、API 超时、向量化失败）
- [ ] Docker 镜像构建成功
  - [ ] 后端 Dockerfile 构建通过
  - [ ] 前端 Dockerfile 构建通过（多阶段构建：npm run build → Nginx）
- [ ] docker-compose.prod.yml 可一键部署生产环境（MySQL、Redis、后端、前端）

**Checkpoint**：系统功能完整，测试覆盖率 > 70%，可部署到生产环境

---

## 实现顺序建议（按周/里程碑）

### 里程碑 1：注册登录与基础架构（第 1 周）

**目标**：用户可注册登录，项目基础架构就绪

**任务**：
- Phase 1: T001-T010（项目初始化、Docker Compose、数据库表）
- Phase 2: T011-T025（ModelGateway、Citations、认证、工具类）
- Phase 3: T026-T034（用户账户与行为记录）

**演示**：用户注册登录，查看个人中心（空行为历史）

---

### 里程碑 2：资源入库与切片（第 2 周）

**目标**：用户可上传文档，查看切片信息

**任务**：
- Phase 4: T035-T048（资源入库、PDF/Markdown 处理、向量化）

**演示**：上传 PDF/Markdown，在资源详情页查看切片列表（chunkIndex、页码/章节）

---

### 里程碑 3：RAG 问答与引用（第 3 周）

**目标**：用户可问答，答案包含可点击的引用卡片

**任务**：
- Phase 5: T049-T059（语义检索、RAG 问答、前端问答界面）

**演示**：输入问题"什么是 RAG？"，查看答案和引用卡片，点击引用跳转到资源详情并高亮 chunk

---

### 里程碑 4：行为记录完善（第 4 周）

**目标**：系统记录所有用户行为，为推荐打基础

**任务**：
- 补充 T053、T067（异步行为记录）
- 完善前端行为记录触发（查询、点击、收藏）

**演示**：查询后刷新个人中心，查看查询历史；点击资源后查看点击记录

---

### 里程碑 5：个性化推荐（第 5 周，P2）

**目标**：用户可查看个性化推荐，推荐包含理由和学习建议

**任务**：
- Phase 6.1: T060-T063（用户画像 MVP）
- Phase 6.2: T064-T067（推荐服务）
- 前端: T069-T072（推荐界面）

**演示**：查询 5 次关于"深度学习"的问题，访问推荐页面，查看 Top-10 推荐列表和推荐理由

---

### 里程碑 6：离线评测与性能分析（第 6 周，P2）

**目标**：可运行离线评测，生成对比报告

**任务**：
- Phase 7: T073-T080（离线评测服务、CLI 入口）
- 前端（可选）: T080（评测报告界面）

**演示**：准备 `test_dataset.json`，运行评测 `java -jar backend.jar eval --dataset test_dataset.json`，下载 Markdown 报告

---

### 里程碑 7：完善与部署（第 7 周）

**目标**：系统功能完整，可部署到生产环境

**任务**：
- Phase 8: T081-T099（补充功能、性能优化、测试、文档、部署）

**演示**：使用 `docker-compose.prod.yml` 启动完整系统，进行端到端测试

---

## 任务优先级说明

### P1（MVP 必须）：里程碑 1-4

- **用户故事 1**：资源入库（Phase 4）
- **用户故事 2**：RAG 问答（Phase 5）
- **用户故事 4**：用户账户（Phase 2/3）

### P2（增强功能）：里程碑 5-6

- **用户故事 3**：个性化推荐（Phase 6）
- **评测对比**：离线评测（Phase 7）

### 可选增强（低优先级）：里程碑 7

- 点击/收藏权重（T068）
- 完善的前端评测界面（T080）

---

## 依赖关系与并行执行

### 阶段依赖

- **Phase 1**：无依赖，可立即开始
- **Phase 2**：依赖 Phase 1，阻塞所有用户故事
- **Phase 3-5**：依赖 Phase 2，可并行开发（不同开发人员）
- **Phase 6-7**：依赖 Phase 3-5，按顺序或并行

### 用户故事内依赖

- **US1（资源入库）**：T035 → T036/T037 → T038 → T039/T040 → T041 → T042/T043
- **US2（RAG 问答）**：T049 → T050 → T051 → T052 → T053/T054
- **US3（推荐）**：T060/T061 → T062 → T063 → T064 → T065 → T066 → T067
- **US4（用户账户）**：T018 → T019 → T020 → T021 → T026/T027/T028

### 并行机会

- **Phase 1**：T003、T004、T005、T007、T008、T010 可并行
- **Phase 2**：T014、T015、T016 可并行；T018、T022、T023、T024、T025 可并行
- **Phase 4**：T036、T037 可并行；T039、T040 可并行；T044、T045、T046、T047、T048 可并行
- **Phase 5**：T053、T055、T056、T057 可并行

---

## 验收标准汇总

### 功能需求（FR）映射

- **FR-001/002**：T020（用户注册登录）
- **FR-003/004**：T028（行为记录）
- **FR-005/006/007/008/009**：T038-T041（资源入库与切片）
- **FR-010/011**：T049（语义检索与聚合）
- **FR-012**：T011、T041、T049（关键参数可配置）
- **FR-013**：T050（RAG 问答）
- **FR-014/020**：T017、T050（Citations 结构）
- **FR-015**：T012、T050（降级策略）
- **FR-016**：T062（用户画像 MVP）
- **FR-017**：T068（可选增强：点击/收藏权重）
- **FR-018/019**：T064（个性化推荐）
- **FR-021/022/023/024/025**：T076（离线评测）

### 成功标准（SC）映射

- **SC-001**：T041、T079（资源入库完成并记录耗时）
- **SC-002**：T050、T079（问答返回引用并记录耗时）
- **SC-003**：T076（离线评测输出指标）
- **SC-004**：T064、T076（对比有画像 vs 无画像）
- **SC-005**：T017、T039、T040、T046（引用 100% 可定位）
- **SC-006**：T025、T079、T088（性能统计输出）

---

## 总计任务统计

- **总任务数**：99 个（T001-T099）
- **P1 任务（MVP）**：48 个（里程碑 1-4，Phase 1-4）
- **P2 任务（增强）**：32 个（里程碑 5-6，Phase 5-7）
- **可选增强**：19 个（里程碑 7，Phase 8）
- **并行任务标记 [P]**：55 个

---

## 备注

### 代码风格指南
- ✅ **所有代码注释必须使用中文**（Javadoc、行内注释、TODO/FIXME、日志）
- ✅ **所有项目文档必须使用中文**
  - 技术文档、设计文档、API 文档
  - README、指南、验收报告、开发日志
  - 配置文件注释
- ✅ **变量和方法命名使用英文**（遵循 Java/TypeScript 命名规范）
- ✅ **技术术语保留英文**（如：API、JWT、Redis、Spring Boot 等）
- 所有 Java 代码遵循 **Google Java Style Guide**（2 空格缩进、Javadoc 注释）
- 所有 TypeScript/Vue 代码遵循 **Google TypeScript Style Guide**（2 空格缩进、单引号、strict mode）
- SQL 语句定义在 Mapper XML 文件中（非注解方式），符合企业级最佳实践

### 系统目标
- 所有任务完成后，系统支持 **100+ 学习资源文档**、**10-20 并发问答请求**

---

## 📊 验收进度汇总

### ✅ 已完成 Phase（截至 2025-12-29）

| Phase | 名称 | 任务范围 | 状态 | 评分 | 完成日期 |
|-------|------|----------|------|------|----------|
| **Phase 1** | 项目初始化与基础设施 | T001-T010 | ✅ 已验收 | ⭐⭐⭐⭐⭐ 97.1% | 2025-12-29 |
| **Phase 2** | 基础设施层 | T011-T025 | ✅ 已验收 | ⭐⭐⭐⭐⭐ 97.1% | 2025-12-29 |

**已完成统计**：
- ✅ 任务完成：25/99（25.3%）
- ✅ 代码行数：2426 行（后端）
- ✅ 测试覆盖：39.2%（78 个测试方法）
- ✅ Javadoc 数量：106 个
- ✅ 文档完整性：925 行项目文档

### 🚧 进行中 Phase（待开始）

| Phase | 名称 | 任务范围 | 预计周期 |
|-------|------|----------|----------|
| **Phase 3** | 用户账户与行为记录 | T026-T034 | 1-2 周 |
| **Phase 4** | 资源入库与向量化 | T035-T048 | 2-3 周 |
| **Phase 5** | 语义检索与 RAG 问答 | T049-T059 | 2-3 周 |
| **Phase 6** | 个性化推荐 | T060-T072 | 2-3 周 |
| **Phase 7** | 离线评测与性能分析 | T073-T080 | 1-2 周 |
| **Phase 8** | 完善与跨用户故事优化 | T081-T099 | 1-2 周 |

### 📝 关键交付物（已完成）

**Phase 1-2 交付物**：
- ✅ `backend/` - Spring Boot 后端项目（Java 21 + 虚拟线程）
- ✅ `frontend/` - Vue 3 前端项目（架构搭建完成）
- ✅ `docker-compose.yml` - Docker Compose 配置（MySQL 8 + Redis Stack）
- ✅ `backend/src/main/resources/db/migration/V1__init_schema.sql` - 数据库初始化脚本
- ✅ `gateway/` - ModelGateway 框架（stub 实现）
- ✅ `dto/response/Citation.java` - 统一引用结构
- ✅ `security/` - JWT 认证体系
- ✅ `util/` - 工具类集合

**文档交付物**：
- ✅ `STARTUP_GUIDE.md` (575 行) - 启动指南
- ✅ `PHASE1_2_ACCEPTANCE_REPORT.md` (692 行) - 验收报告
- ✅ `PHASE1_2_ACCEPTANCE_CHECKLIST.md` (337 行) - 验收清单
- ✅ `development-log.md` (131 行) - 开发日志

### 🎯 下一步行动

**Phase 3 开始前**：
1. 🔧 配置智谱 AI API key（ZHIPU_API_KEY）
2. 📝 准备测试数据（PDF/Markdown 文档）
3. ✅ 确认 Java 21 环境正常

**Phase 3 目标**：
- 实现完整的 EmbeddingGateway（调用智谱 AI API）
- 实现完整的 ChatGateway（调用智谱 AI API）
- 实现资源上传功能（PDF/Markdown 解析）
- 实现向量索引创建和查询

---

**最后更新**: 2025-12-30
**下次更新**: Phase 3 完成后
