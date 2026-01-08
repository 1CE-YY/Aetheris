# Aetheris RAG 系统启动指南

**最后更新**: 2025-12-30

---

## 📑 目录

- [快速启动](#快速启动推荐) - 一键启动脚本
- [完整启动步骤](#完整启动步骤) - 详细分步指南
- [验收测试](#验收测试) - Phase 1-2 功能验证
- [故障排查](#故障排查) - 常见问题解决
- [相关文档](#相关文档) - 更多资源

---

## 🚀 快速启动（推荐）

### 一键启动

```bash
cd /Users/hubin5/app/Aetheris
./start.sh
```

启动脚本会自动：
1. ✅ 检查环境（Java 21、Maven、Node.js、Docker）
2. ✅ 创建 .env 配置文件（如不存在）
3. ✅ 启动 MySQL + Redis（Docker）
4. ✅ 启动后端（Spring Boot）
5. ✅ 启动前端（Vite）

### 访问地址

启动成功后，访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| 🌐 **前端** | http://localhost:5173 | Vue 3 开发服务器 |
| 🔧 **后端 API** | http://localhost:8080 | Spring Boot 应用 |
| 📊 **健康检查** | http://localhost:8080/actuator/health | Actuator 端点 |
| 🗄️ **MySQL** | localhost:3306 | 数据库 |
| 🔴 **Redis** | localhost:6379 | Redis Stack |

### 停止服务

```bash
./stop.sh
```

### 快速验证

```bash
# 测试用户注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Password123!"}'

# 测试用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123!"}'
```

---

## 📋 完整启动步骤

如果一键启动脚本遇到问题，可以按照以下步骤手动启动。

### 步骤 1: 环境准备

#### 1.1 设置 Java 21（必须！）

```bash
# 检查当前 Java 版本
java -version  # 需要是 Java 21

# 如果不是 Java 21，设置环境变量
export JAVA_HOME=/Users/hubin5/app/Aetheris/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 验证
java -version  # 应显示 "java version "21.0.8""
```

**⚠️ 重要**: 项目使用 Java 21 虚拟线程特性，必须使用 Java 21！

#### 1.2 创建环境配置文件

```bash
# 在项目根目录执行
cd /Users/hubin5/app/Aetheris
cp .env.example .env
```

编辑 `.env` 文件，至少需要配置以下关键项：

```bash
# 智谱 AI API Key（必须配置！）
ZHIPU_API_KEY=your-actual-api-key-here

# JWT Secret（生产环境必须更换）
JWT_SECRET=change-this-to-a-strong-random-key-in-production

# 数据库密码（使用默认值即可）
MYSQL_ROOT_PASSWORD=root123
MYSQL_DATABASE=aetheris_rag
MYSQL_USERNAME=aetheris
MYSQL_PASSWORD=aetheris123

# Redis 密码（默认为空）
REDIS_PASSWORD=
```

#### 1.3 安装前端依赖

```bash
cd frontend

# 检查 Node.js 版本（需要 Node.js 16+）
node -v
npm -v

# 安装依赖（如果未安装）
pnpm install
# 或使用 npm install
```

---

### 步骤 2: 启动基础设施（MySQL + Redis）

```bash
# 在项目根目录执行
cd /Users/hubin5/app/Aetheris

# 启动 Docker Compose 服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志（可选）
docker-compose logs -f mysql
docker-compose logs -f redis
```

**预期输出**：
```
NAME                IMAGE                      STATUS
aetheris-mysql      mysql:8.0                 Up (healthy)
aetheris-redis      redis/redis-stack-server  Up (healthy)
```

**验证连接**：
```bash
# 测试 MySQL 连接
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123 -e "SHOW DATABASES;"

# 测试 Redis 连接
docker exec -it aetheris-redis redis-cli -a aetheris123 ping
# 输出: PONG

# 验证 Redis Stack 模块加载
docker exec -it aetheris-redis redis-cli -a aetheris123 FT._LIST
# 应显示向量索引模块已加载
```

---

### 步骤 3: 启动后端服务

```bash
# 在 backend 目录执行
cd /Users/hubin5/app/Aetheris/backend

# 设置 Java 21（如果尚未设置）
export JAVA_HOME=/Users/hubin5/app/Aetheris/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 清理并编译
mvn clean compile

# 启动 Spring Boot 应用
mvn spring-boot:run
```

**预期输出**：
```
...
Started AetherisRagApplication in X.XXX seconds (JVM running for X.XXX)
```

**关键日志检查**：
```
✅ 虚拟线程已启用 (Virtual threads enabled)
✅ MySQL 连接成功 (MySQL connection established)
✅ Redis 连接成功 (Redis connection established)
✅ Flyway 迁移完成 (Flyway migration completed)
✅ JWT 过滤器已注册 (JWT filter registered)
```

**验证后端 API**：
```bash
# 新终端窗口执行
curl http://localhost:8080/actuator/health

# 预期输出（如果配置了 Actuator）:
# {"status":"UP"}

# 测试注册 API
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 预期输出: 返回 JWT token
```

---

### 步骤 4: 启动前端服务

```bash
# 新终端窗口，在 frontend 目录执行
cd /Users/hubin5/app/Aetheris/frontend

# 启动开发服务器
pnpm dev
# 或使用 npm run dev
```

**预期输出**：
```
  VITE v5.x.x  ready in XXX ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

**访问前端**：
- 打开浏览器访问: `http://localhost:5173`
- 应看到登录页面

---

## ✅ 验收测试

### Phase 1-2 功能验收

根据 `tasks.md`，Phase 1-2 已完成以下任务：

✅ **Phase 1: 项目初始化** (T001-T010)
- 项目结构搭建（Spring Boot 3.5 + Vue 3）
- Docker Compose 配置（MySQL 8 + Redis Stack）
- 数据库表结构定义（Flyway migrations）

✅ **Phase 2: 基础设施层** (T011-T025)
- ModelGateway 框架（EmbeddingGateway、ChatGateway stub）
- Citations 统一结构
- 用户认证系统（JWT + BCrypt）
- 工具类（HashUtil、TextNormalizer、PerformanceTimer）

### 测试 1: 项目结构验证

```bash
# 检查后端项目结构
cd /Users/hubin5/app/Aetheris/backend
find src/main/java/com/aetheris/rag -type d | sort

# 预期输出:
# src/main/java/com/aetheris/rag
# src/main/java/com/aetheris/rag/config
# src/main/java/com/aetheris/rag/controller
# src/main/java/com/aetheris/rag/dto
# src/main/java/com/aetheris/rag/gateway
# src/main/java/com/aetheris/rag/mapper
# src/main/java/com/aetheris/rag/model
# src/main/java/com/aetheris/rag/service
# src/main/java/com/aetheris/rag/util
```

### 测试 2: 数据库表验证

```bash
# 连接 MySQL
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag

# 查看所有表
SHOW TABLES;

# 预期输出:
# tables_in_aetheris_rag
# flyway_schema_history
# resources
# resource_chunks
# user_behaviors
# user_profiles
# users
# eval_queries
# eval_runs

# 查看表结构
DESCRIBE users;
DESCRIBE resources;
```

### 测试 3: 用户注册登录功能

**3.1 测试注册**：
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

**预期输出**：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "createdAt": "2025-12-30T...",
    "lastActiveAt": "2025-12-30T..."
  }
}
```

**3.2 测试登录**：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

**预期输出**：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "createdAt": "2025-12-30T...",
    "lastActiveAt": "2025-12-30T..."
  }
}
```

**3.3 测试 JWT 验证**：
```bash
# 使用返回的 token 访问受保护资源
TOKEN="your-token-here"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile
```

### 测试 4: ModelGateway Stub 验证

**检查 EmbeddingGateway stub**：
```bash
# 查看后端日志，应该看到 EmbeddingGateway stub 的日志
# 当前为 stub 实现，返回 dummy embedding
```

**检查 ChatGateway stub**：
```bash
# ChatGateway 当前为 stub 实现
# 完整实现将在 Phase 5 完成
```

### 测试 5: Citations 结构验证

```bash
# 检查 Citations.java 文件
cat backend/src/main/java/com/aetheris/rag/dto/response/Citation.java

# 预期包含字段:
# - resourceId
# - resourceTitle
# - chunkId
# - chunkIndex
# - location
# - snippet
# - score
```

---

## 🔍 故障排查

### 问题 1: Java 版本错误

**症状**: `Unsupported class file major version 65`

**解决方案**:
```bash
export JAVA_HOME=/Users/hubin5/app/Aetheris/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version  # 确认是 Java 21
```

### 问题 2: MySQL 连接失败

**症状**: `Communications link failure`

**检查**:
```bash
# 确认 Docker 容器运行中
docker-compose ps

# 确认端口未被占用
lsof -i :3306

# 查看 MySQL 日志
docker-compose logs mysql
```

### 问题 3: Redis 连接失败

**症状**: `Unable to connect to Redis` 或 `DENIED Redis is running in protected mode`

**检查**:
```bash
# 确认 Redis 运行中
docker-compose ps redis-stack

# 测试连接（使用密码）
docker exec -it aetheris-redis redis-cli -a aetheris123 ping

# 检查 Redis Stack 模块
docker exec -it aetheris-redis redis-cli -a aetheris123 FT._LIST
```

### 问题 4: Flyway 迁移失败

**症状**: `Flyway migration failed`

**解决方案**:
```bash
# 清理数据库重新迁移
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123 -e "DROP DATABASE aetheris_rag; CREATE DATABASE aetheris_rag;"
docker-compose restart mysql

# 重新启动后端
cd backend
mvn clean spring-boot:run
```

### 问题 5: 前端无法连接后端

**症状**: `Network Error` 或 CORS 错误

**检查**:
```bash
# 确认后端运行在 8080
curl http://localhost:8080/actuator/health

# 检查前端代理配置
cat frontend/vite.config.ts | grep proxy
```

### 问题 6: 端口被占用

**症状**: `Address already in use`

**解决方案**:
```bash
# 查看占用端口的进程
lsof -i :8080  # 后端
lsof -i :5173  # 前端
lsof -i :3306  # MySQL
lsof -i :6379  # Redis

# 杀死进程
kill -9 <PID>
```

### 查看日志

```bash
# 后端日志
tail -f logs/backend.log

# 前端日志
tail -f logs/frontend.log

# Docker 日志
docker-compose logs -f mysql
docker-compose logs -f redis
```

---

## 📊 验收检查清单

### Phase 1: 项目初始化 ✅

- [x] Docker Compose 成功启动 MySQL 和 Redis
- [x] 数据库表结构正确创建（8 张表）
- [x] 后端项目可编译成功
- [x] 前端项目可启动

### Phase 2: 基础设施层 ✅

- [x] ModelGateway 接口和实现类存在（stub）
- [x] Citations 结构定义正确
- [x] 用户注册 API 测试通过
- [x] 用户登录 API 测试通过
- [x] JWT token 生成和验证正常
- [x] 工具类实现正确（HashUtil、TextNormalizer、PerformanceTimer）
- [x] 虚拟线程已启用（查看启动日志）

---

## 🎯 下一步

完成 Phase 1-2 验收后，可以继续：

**Phase 3: 资源入库与向量化** (T026-T038)
- ModelGateway 完整实现（调用智谱 AI API）
- PDF 文档解析（Apache PDFBox）
- Markdown 文档解析（CommonMark）
- 文本分段与向量化
- Redis Stack 向量索引创建
- Embedding 缓存机制

**Phase 4: RAG 问答系统** (T039-T050)
- 向量检索（RediSearch）
- Prompt 模板设计
- LLM 调用与响应解析
- 引用来源生成
- 问答 API 实现

**Phase 5: 推荐系统与评测** (T051-T060)
- 用户行为记录
- 用户画像更新
- 个性化推荐算法
- 离线评测系统
- 性能优化

---

## 📝 相关文档

- **项目总览**: `CLAUDE.md`
- **任务清单**: `specs/001-rag-recommendation-system/tasks.md`
- **技术规范**: `specs/001-rag-recommendation-system/spec.md`
- **架构设计**: `specs/001-rag-recommendation-system/plan.md`
- **验收报告**: `docs/PHASE1_2_ACCEPTANCE_REPORT.md`
- **开发日志**: `docs/dev-logs/development-log.md`

---

## 🆘 获取帮助

如遇问题：
1. 查看本文档的"故障排查"部分
2. 检查 `logs/backend.log` 日志文件
3. 查看 Docker 容器日志: `docker-compose logs [service-name]`
4. 查看开发日志: `docs/dev-logs/development-log.md`
5. 查看验收报告: `docs/PHASE1_2_ACCEPTANCE_REPORT.md`

**祝使用顺利！** 🎉
