# Aetheris RAG 系统 - AI 上下文记忆

**项目**: Aetheris RAG 系统
**版本**: Phase 1-4 已完成，Phase 5 进行中
**项目路径**: `/Users/hubin5/app/Aetheris`
**最后更新**: 2026-01-07

---

## 项目概述

**用途**: 面向高校的 RAG 检索与推荐系统
**架构**: Spring Boot 3.5 + Vue 3 + Redis Stack (向量数据库)
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

### 默认账户
- 用户名：`admin`
- 邮箱：`admin@aetheris.com`
- 密码：`admin123`

---

## 关键约束（必须遵守）

### 1. 服务管理规范

**⚠️ 启动和停止服务必须使用根目录下的脚本**

```bash
# 启动服务
./start.sh                    # 一键启动所有服务

# 停止服务
./stop.sh                     # 停止所有服务（交互式）
./stop.sh all                 # 停止所有服务
./stop.sh backend             # 仅停止后端
./stop.sh frontend            # 仅停止前端
./stop.sh docker              # 仅停止 Docker 服务

# 查看状态
cat .pids.json | jq           # 查看服务状态
tail -f logs/backend.log      # 查看后端日志
tail -f logs/application.log  # 查看应用日志
```

**❌ 禁止**：
- 直接使用 `mvn spring-boot:run` 启动后端
- 直接使用 `npm run dev` 启动前端
- 手动使用 `kill` 命令杀进程

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

## 关键文件位置

### 配置文件
- `backend/src/main/resources/application.yml` - Spring Boot 主配置
- `docker-compose.yml` - Docker 编排配置
- `.env.example` - 环境变量模板
- `.pids.json` - 进程管理文件（由脚本自动管理）

### 核心代码
- `backend/src/main/java/com/aetheris/rag/controller/` - REST API
- `backend/src/main/java/com/aetheris/rag/service/` - 业务接口
- `backend/src/main/java/com/aetheris/rag/gateway/` - ModelGateway 框架
- `backend/src/main/java/com/aetheris/rag/entity/` - 实体类（原 model 包）
- `backend/src/main/resources/mapper/` - MyBatis XML

### 文档
- `README.md` - 项目主页
- `specs/001-rag-recommendation-system/tasks.md` - 任务清单
- `specs/001-rag-recommendation-system/contracts/openapi.yaml` - API 规范

---

## 常用命令

### 环境设置
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 后端开发
```bash
cd backend
mvn clean compile            # 编译
mvn test                     # 运行测试
mvn clean package            # 构建 JAR
```

### 数据库操作
```bash
# MySQL 连接
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123

# Redis 连接
docker exec -it aetheris-redis redis-cli -a aetheris123

# 查看数据库表
docker exec -i aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag -e "SHOW TABLES;"
```

---

## 服务端点

- **前端**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health

---

## 故障排除

### 问题：Java 版本不匹配
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 问题：端口被占用
```bash
lsof -i :8080  # 或 :5173
kill -9 <PID>
```

### 问题：Flyway 迁移失败
```bash
docker exec -i aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag -e "
DROP TABLE IF EXISTS flyway_schema_history;
"
```

---

**记忆版本**: v3.1.0（精简版）
