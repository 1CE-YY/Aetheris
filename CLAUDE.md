# Aetheris RAG 系统

**项目**: Aetheris RAG 系统
**版本**: Phase 1-4 已完成，Phase 5 进行中
**项目路径**: `/Users/hubin5/app/Aetheris`
**最后更新**: 2026-01-08

---

## 项目概述

**用途**: 面向高校的 RAG 检索与推荐系统
**架构**: Spring Boot 3.5.x + Vue 3 + Redis Stack (向量数据库)
**AI 提供商**: 智谱 AI (GLM-4)

**核心功能**:
- 学习资源入库（PDF/Markdown）
- 语义检索 + RAG 问答（带引用来源）
- 个性化推荐（基于用户画像）
- 离线评测（Precision@K、Recall@K）

---

## 当前状态

### 已完成阶段
- ✅ Phase 1-2: 项目初始化 + 基础设施（97.1% 验收通过）
- ✅ Phase 3: 用户认证 + 前端页面
- ✅ Phase 4: 资源入库 + 向量化
- 🚧 Phase 5: RAG 问答系统（进行中）


一些接口测试时显示没有权限403，需要先登陆获取token再测试
---

## 关键约束（必须遵守）

### 1. 服务管理规范

**⚠️ 启动和停止服务必须使用根目录下的脚本**

#### 启动服务（start.sh）

**脚本特性**：
- ✅ 自动检查并配置 Java 21 环境（无需手动 export）
- ✅ 支持交互式菜单和命令行参数两种模式
- ✅ 支持选择性启动（前端/后端/Docker）
- ✅ 选择后自动退出（无需选择 0 退出）

```bash
# 交互模式（弹出菜单，选择后自动执行并退出）
./start.sh

# 命令行模式
./start.sh --frontend-only    # 仅启动前端
./start.sh --backend-only     # 仅启动后端
./start.sh --docker-only      # 仅启动 Docker（MySQL + Redis）
./start.sh --all              # 启动所有服务
./start.sh --help             # 显示帮助信息

# 查看状态
cat .pids.json | jq           # 查看服务状态
tail -f logs/backend.log      # 查看后端日志
tail -f logs/frontend.log     # 查看前端日志
```

#### 停止服务（stop.sh）

**脚本特性**：
- ✅ 支持交互式菜单和命令行参数两种模式
- ✅ 支持选择性停止（前端/后端/Docker）
- ✅ 选择后自动退出（无需选择 0 退出）

```bash
# 交互模式（弹出菜单，选择后自动执行并退出）
./stop.sh

# 命令行模式
./stop.sh --frontend-only     # 仅停止前端
./stop.sh --backend-only      # 仅停止后端
./stop.sh --docker-only       # 仅停止 Docker（MySQL + Redis）
./stop.sh --all               # 停止所有服务（前端 + 后端）
./stop.sh --help              # 显示帮助信息
```

**❌ 禁止**：
- 直接使用 `mvn spring-boot:run` 启动后端
- 直接使用 `npm run dev` 启动前端
- 手动使用 `kill` 命令杀进程
- 手动 export JAVA_HOME（脚本会自动处理）

### 2. 虚拟线程必须启用
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 不可关闭
```

### 3. 代码规范

#### 注释和文档语言
- ✅ **所有代码注释必须使用中文**（Javadoc、行内注释、日志）
- ✅ **所有项目文档必须使用中文**
- ✅ **变量和方法命名使用英文**
- ✅ **Git commit 消息使用中文**

#### 架构规范
- ❌ **不使用 Java Record** - 使用 Lombok `@Data`、`@Builder`
- ❌ **不使用 MyBatis 注解** - SQL 必须写在 XML 文件中
- ✅ Service 接口与实现分离
- ✅ 使用 `@RequiredArgsConstructor` 进行依赖注入

### 4. 架构约束
- Redis Stack 是唯一的向量存储
- ModelGateway 是唯一的模型调用入口
- 所有答案/推荐必须包含引用来源
- LLM 不可用时必须降级

### 5. 缓存与幂等
- Embedding 结果必须按文本哈希缓存（SHA-256），TTL 30 天
- 资源入库必须幂等，基于内容哈希去重

### 6. 性能要求
- 问答响应 P95 ≤ 5秒
- 资源入库 P95 ≤ 30秒
- 必须记录分段耗时（解析、Embedding、检索、生成）

---

## 技术栈

### 后端
- **Java**: 21
- **框架**: Spring Boot 3.5.9
- **数据库**: MyBatis
- **RAG**: LangChain4j
- **向量数据库**: Redis Stack
- **关系数据库**: MySQL 8
- **安全**: JWT + BCrypt
- **类库**: Lombok、Guava、Commons Lang3

### 前端
- **框架**: Vue 3.3 + TypeScript
- **UI 库**: Ant Design Vue 4.x
- **构建工具**: Vite 5.x
- **状态管理**: Pinia 2.1.7
- **路由**: Vue Router 4.2.5

### 基础设施
- **容器**: Docker Compose
- **MySQL**: 端口 3306，用户：`aetheris`，密码：`040316`，数据库：`aetheris_rag`
- **Redis**: 端口 6379，密码：`040316`

---

## 关键文件位置

### 配置文件
- `backend/src/main/resources/application.yml` - Spring Boot 主配置
- `docker-compose.yml` - Docker 编排配置
- `.env` - 环境变量文件
- `.pids.json` - 进程管理文件（由脚本自动管理）

### 核心代码
- `backend/src/main/java/com/aetheris/rag/controller/` - REST API
- `backend/src/main/java/com/aetheris/rag/service/` - 业务接口
- `backend/src/main/java/com/aetheris/rag/gateway/` - ModelGateway 框架
- `backend/src/main/java/com/aetheris/rag/entity/` - 实体类
- `backend/src/main/resources/mapper/` - MyBatis XML

### 文档
- `README.md` - 项目主页
- `specs` - 需求规格，需要严格遵守
- `specs/001-rag-recommendation-system/tasks.md` - 任务清单
- `specs/001-rag-recommendation-system/contracts/openapi.yaml` - API 规范

---

## 服务端点

- **前端**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health

---
