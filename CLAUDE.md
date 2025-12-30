# Aetheris RAG 系统 - AI 上下文记忆

**项目**: Aetheris RAG 系统
**版本**: Phase 1-2 已完成 (v1.0.0)
**验收**: 97.1% (34/35 项通过)
**最后更新**: 2025-12-29
**下次审查**: Phase 3 完成后

---

## 项目概述

**用途**: 面向高校的 RAG 检索与推荐系统
**架构**: Spring Boot 3.5 + Vue 3 + Redis Stack (向量数据库)
**AI 提供商**: 智谱 AI (GLM-4)
**项目路径**: `/Users/hubin5/app/Aetheris`

**核心功能**:
- 学习资源入库（PDF/Markdown）
- 语义检索 + RAG 问答（带引用来源）
- 个性化推荐（基于用户画像）
- 离线评测（Precision@K、Recall@K）

---

## 当前状态

### 已完成（Phase 1-2）- 100%

**交付内容**:
- ✅ 项目结构搭建（Spring Boot 3.5 + Java 21 + Vue 3）
- ✅ Docker Compose 基础设施（MySQL 8 + Redis Stack）
- ✅ 数据库表结构设计（8 张表，Flyway 迁移）
- ✅ ModelGateway 框架（stub 实现）
- ✅ 用户认证系统（JWT + BCrypt + Spring Security）
- ✅ 工具类库（HashUtil、TextNormalizer、PerformanceTimer、LogSanitizer）
- ✅ Citations 统一结构（引用格式）
- ✅ 完整的单元测试（78 个测试方法，39.2% 覆盖率）
- ✅ 文档体系（925 行项目文档 + 782 行 API 规范）

**验收报告**: `docs/PHASE1_2_ACCEPTANCE_REPORT.md`
**评分**: ⭐⭐⭐⭐⭐ 97.1%

### 已修复问题（2025-12-29）

1. **Redis Stack 模块未加载**
   - 在 docker-compose.yml 中添加 `--loadmodule` 参数
   - 现在全部 6 个模块已加载（redisearch、rejson 等）

2. **Redis AOF 持久化不工作**
   - 修复挂载路径为：`./data/redis:/data`
   - 在 Redis 命令中添加 `--dir /data`

3. **进程管理文件分散**
   - 统一为 `.pids.json`，包含状态和元数据

### 已知问题（非阻塞）

1. API 测试受 CORS 限制（通过前端或 Postman 测试）
2. Swagger UI 未集成（计划在 Phase 3 实施）
3. JWT_SECRET 使用占位符（生产环境必须更换）
4. 前端页面未实现（Phase 1-2 仅搭建架构）

### 下一阶段（Phase 3）- 预计 4-6 周

**计划内容**:
- 完成 ModelGateway 实现（调用智谱 AI API）
- PDF/Markdown 文档解析
- 文本分段与向量化
- Redis Stack 向量索引创建
- Embedding 缓存机制

---

## 关键约束（必须遵守）

### 1. 虚拟线程必须启用
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 不可关闭
```
**状态**: ✅ 已验证启用

### 2. 代码规范

#### 2.1 注释和文档语言
- ✅ **所有代码注释必须使用中文**
  - Javadoc 类注释：中文
  - Javadoc 方法注释：中文
  - 行内注释：中文
  - TODO/FIXME 标记：中文
  - 日志输出：中文（但保留技术术语英文，如 API、JWT、Redis）
- ✅ **所有项目文档必须使用中文**
  - 技术文档（spec.md、plan.md、research.md 等）
  - 设计文档（data-model.md、API 文档等）
  - README 和指南文档
  - 验收报告和开发日志
  - 配置文件注释
- ✅ **变量和方法命名使用英文**（遵循 Java 命名规范）
- ✅ **Git commit 消息使用中文**

**示例**：
```java
/**
 * 用户认证服务实现。
 *
 * <p>提供用户注册、登录、密码验证等功能。
 *
 * @author Aetheris Team
 * @version 1.0.0
 * @since 2025-12-30
 */
public class AuthServiceImpl implements AuthService {

    /**
     * 注册新用户。
     *
     * @param request 注册请求，包含用户名、邮箱和密码
     * @return 认证响应，包含 JWT token 和用户信息
     * @throws RuntimeException 如果邮箱或用户名已存在
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        // TODO: 添加邮箱验证码功能
        ...
    }
}
```

#### 2.2 架构规范
- ❌ **不使用 Java Record** - 使用 Lombok `@Data`、`@Builder`
- ❌ **不使用 MyBatis 注解** - SQL 必须写在 XML 文件中
- ✅ Service 接口与实现分离
- ✅ 使用 `@RequiredArgsConstructor` 进行依赖注入（不使用 `@Autowired`）

**状态**: ✅ 全部已验证

### 3. 架构约束
- Redis Stack 是唯一的向量存储（禁止使用其他向量数据库）
- ModelGateway 是唯一的模型调用入口（禁止直连智谱 AI API）
- 所有答案/推荐必须包含引用来源（resourceId、chunkId、location、snippet）
- LLM 不可用时必须降级，返回检索结果 + 证据摘要

**状态**: ✅ 架构已就绪，待 Phase 3-5 完整实现

### 4. 缓存与幂等
- Embedding 结果必须按文本哈希缓存（SHA-256），TTL 30 天
- 资源入库必须幂等，基于内容哈希去重

**状态**: ✅ EmbeddingCache 已实现

### 5. 性能要求
- 问答响应 P95 ≤ 5秒
- 资源入库 P95 ≤ 30秒
- 必须记录分段耗时（解析、Embedding、检索、生成）

**当前性能**（Phase 1-2）:
- ✅ 启动时间：3.9 秒（目标 <5秒）
- ✅ API 响应：5.9ms（目标 <100ms）
- ✅ 内存使用：275MB（目标 <500MB）

---

## 技术栈

### 后端
- **Java**: 21（虚拟线程）
- **框架**: Spring Boot 3.5.9
- **数据库**: MyBatis 3.5（SQL 在 XML）
- **RAG**: LangChain4j 0.35
- **向量数据库**: Redis Stack（6 个模块已加载）
- **关系数据库**: MySQL 8
- **安全**: JWT (jjwt 0.12.3) + BCrypt
- **类库**: Lombok、Guava、Commons Lang3

### 前端
- **框架**: Vue 3.3 + TypeScript
- **UI 库**: Ant Design Vue 4.x
- **构建工具**: Vite 5.x
- **状态管理**: Pinia 2.1.7
- **路由**: Vue Router 4.2.5

### 基础设施
- **容器**: Docker Compose
- **MySQL**: 端口 3306，用户：`aetheris`，密码：`aetheris123`，数据库：`aetheris_rag`
- **Redis**: 端口 6379，密码：`aetheris123`

---

## 项目结构

```
Aetheris/
├── backend/src/main/java/com/aetheris/rag/
│   ├── controller/           # REST API（AuthController）
│   ├── service/              # 业务接口（AuthService）
│   ├── service/impl/         # 业务实现（AuthServiceImpl）
│   ├── mapper/               # MyBatis 接口（UserMapper）
│   ├── model/                # 实体类（@Data，不使用 Record）
│   ├── dto/                  # 请求/响应 DTO
│   │   ├── request/          # LoginRequest、RegisterRequest
│   │   └── response/         # AuthResponse、UserResponse、Citation
│   ├── gateway/              # ModelGateway 框架
│   │   ├── EmbeddingGateway.java    # STUB（Phase 1-2）
│   │   ├── ChatGateway.java         # STUB（Phase 1-2）
│   │   ├── cache/EmbeddingCache.java
│   │   ├── retry/ModelRetryStrategy.java
│   │   └── sanitize/LogSanitizer.java
│   ├── config/               # Spring 配置（SecurityConfig、RedisConfig）
│   ├── util/                 # 工具类（JwtUtil、HashUtil、TextNormalizer、PerformanceTimer）
│   └── validation/           # 自定义校验（PasswordComplexity、PasswordComplexityValidator）
│
├── backend/src/main/resources/
│   ├── application.yml       # 主配置（192 行）
│   ├── db/migration/         # Flyway 迁移脚本
│   └── mapper/               # MyBatis XML（UserMapper.xml）
│
├── frontend/src/
│   ├── api/                  # API 调用（待实现）
│   ├── components/           # Vue 组件（待实现）
│   ├── views/                # 页面组件（目录已创建，待实现）
│   ├── router/               # 路由配置（待实现）
│   ├── stores/               # Pinia 状态（待实现）
│   └── utils/                # 工具函数（待实现）
│
├── docs/                    # 项目文档
│   ├── STARTUP_GUIDE.md     # 启动指南（一键启动 + 完整步骤）
│   ├── PHASE1_2_ACCEPTANCE_REPORT.md  # 验收报告
│   ├── PHASE1_2_ACCEPTANCE_CHECKLIST.md  # 验收清单
│   └── dev-logs/development-log.md  # 开发日志
│
├── specs/001-rag-recommendation-system/
│   ├── spec.md              # 需求规格
│   ├── plan.md              # 实施计划
│   ├── tasks.md             # 任务清单
│   └── contracts/openapi.yaml  # API 规范（782 行）
│
├── data/                   # 持久化数据（gitignore）
│   ├── mysql/              # 106MB
│   └── redis/              # AOF 文件
│
├── docker-compose.yml       # 基础设施（84 行）
├── start.sh                # 一键启动（7.2KB）
├── stop.sh                 # 一键停止（3.8KB）
├── .env.example            # 配置模板（142 行）
├── .pids.json              # 进程管理
└── README.md               # 项目主页
```

---

## 关键命令

### 环境设置
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 启动服务
```bash
cd /Users/hubin5/app/Aetheris
./start.sh                    # 启动所有服务
docker-compose up -d          # 启动基础设施
```

### 后端开发
```bash
cd backend
mvn clean compile            # 编译
mvn spring-boot:run          # 运行
mvn test                     # 运行测试（78 个方法，39.2% 覆盖率）
mvn clean package            # 构建 JAR
```

### 数据库操作
```bash
# MySQL 连接
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123

# Redis 连接（带密码）
docker exec -it aetheris-redis redis-cli -a aetheris123

# 查看数据库表
docker exec -i aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag -e "SHOW TABLES;"
```

---

## 数据库 Schema

### 表（共 8 张）

1. **users** - 用户账户（3 条记录）
2. **resources** - 学习资源
3. **resource_chunks** - 文档分段
4. **user_behaviors** - 用户行为追踪
5. **user_profiles** - 用户画像
6. **eval_queries** - 评测查询
7. **eval_runs** - 评测运行
8. **flyway_schema_history** - 迁移历史

### 外键关系

- `resource_chunks.resource_id` → `resources.id`
- `resources.uploaded_by` → `users.id`
- `user_behaviors.resource_id` → `resources.id`
- `user_behaviors.user_id` → `users.id`
- `user_profiles.user_id` → `users.id`
- `eval_runs.comparison_with_run_id` → `eval_runs.id`（自引用）

### 默认管理员账户
- 用户名：`admin`
- 邮箱：`admin@aetheris.com`
- 密码：`admin123`

---

## Redis Stack 配置

### 已加载模块（6/6）

1. `rediscompat.so` - 兼容层
2. **`redisearch.so`** - 向量搜索（核心依赖）
3. `redistimeseries.so` - 时间序列
4. `rejson.so` - JSON 支持
5. `redisbloom.so` - 概率数据结构
6. `redisgears.so` + V8 引擎 - 可编程性

### 向量索引

- **名称**: `chunk_vector_index`
- **维度**: 1024（Embedding-v2）
- **距离度量**: COSINE
- **HNSW**: M=16, EF_CONSTRUCTION=128

### 连接信息

- 主机：localhost
- 端口：6379
- 密码：`aetheris123`

---

## 服务端点

### 应用地址
- **前端**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health

### 认证 API

#### 注册
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "pass123"
}
```

**响应 201**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "createdAt": "2025-12-29T13:33:08Z",
    "lastActiveAt": "2025-12-29T13:33:08Z"
  }
}
```

#### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "pass123"
}
```

**校验规则**:
- 邮箱不能已存在
- 用户名不能已存在
- 密码：至少 1 个字母 + 1 个数字

---

## 代码质量指标

- **代码行数**: 2426（后端）
- **Javadoc 注释**: 106 个
- **测试类**: 11 个
- **测试方法**: 78 个
- **覆盖率**: 39.2%
- **Java 文件**: 28 个

### 规范遵守情况
- ✅ 不使用 Java Record
- ✅ MyBatis SQL 全部在 XML
- ✅ Service 接口与实现分离
- ✅ 依赖注入使用 `@RequiredArgsConstructor`
- ✅ 虚拟线程已启用

---

## 关键文件

### 配置文件
- `backend/src/main/resources/application.yml`（192 行）
- `docker-compose.yml`（84 行）
- `.env.example`（142 行）

### 文档
- `README.md` - 项目主页
- `docs/PHASE1_2_ACCEPTANCE_REPORT.md` - 验收报告
- `specs/001-rag-recommendation-system/contracts/openapi.yaml`（782 行）

### 脚本
- `start.sh`（7.2KB）- 启动所有服务
- `stop.sh`（3.8KB）- 停止所有服务

---

## 故障排除

### 问题：Java 版本不匹配
**错误**: Maven 使用了错误的 Java 版本
**解决**:
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 问题：Redis 连接被拒绝
**错误**: `DENIED Redis is running in protected mode`
**解决**: 检查 `.env` 中的 `REDIS_PASSWORD` 并重启 Redis

### 问题：端口已被占用
**错误**: `Address already in use`
**解决**:
```bash
lsof -i :8080  # 或 :5173
kill -9 <PID>
```

### 问题：Flyway 迁移失败
**解决**: 清空数据库并重新迁移
```bash
docker exec -i aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag -e "
DROP TABLE IF EXISTS flyway_schema_history;
"
```

---

## Stub 实现（Phase 1-2）

### EmbeddingGateway
**当前**: 返回 dummy 向量 `[0.1f, 0.2f, ...]`
**Phase 5**: 完整实现智谱 AI Embedding API

### ChatGateway
**当前**: 返回 dummy 响应 `"This is a stub response"`
**Phase 5**: 完整实现智谱 AI Chat API

---

## Phase 3 规划

### 第 1-2 周：ModelGateway 实现
- 完成 EmbeddingGateway（智谱 AI API）
- 完成 ChatGateway（智谱 AI API）
- 添加重试逻辑和错误处理
- 实现 Embedding 缓存

### 第 3-4 周：资源入库
- PDF 解析（Apache PDFBox）
- Markdown 解析（CommonMark）
- 文本分段逻辑
- 向量化和存储

### 第 5-6 周：前端页面
- 认证页面（登录/注册）
- 资源管理页面
- 基础 API 集成

---

## 联系方式

- **问题反馈**: GitHub Issues
- **邮箱**: 1307792296@qq.com
- **项目文档**: 见 `docs/` 目录

---

**记忆版本**: v2.0.0
**最后更新**: 2025-12-29
**验收状态**: 通过 97.1%
