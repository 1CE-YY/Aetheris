# Aetheris 项目开发指南

**最后更新**: 2025-12-26

## 项目概述

**Aetheris** 是一个面向高校的 RAG 检索与推荐系统，核心功能：
- 学习资源入库（PDF/Markdown）
- 语义检索与 RAG 问答（带引用来源）
- 个性化推荐（基于用户画像）
- 离线评测与性能度量

**架构特点**：
- Spring Boot 单体应用 + Vue 3 前端
- Redis Stack 作为唯一向量存储
- ModelGateway 统一模型调用（智谱 AI）
- 所有答案必须包含可追溯引用

---

## 当前状态

**进度**:
- ✅ Phase 1-2 完成（项目初始化 + 基础设施层）
  - 项目结构搭建（Spring Boot 3.5 + Java 21 + Vue 3）
  - ModelGateway 框架（stub 实现）
  - 用户认证系统（JWT）
  - 工具类和基础组件
- 🚧 Phase 3-5 待实施（资源入库、RAG 问答、推荐）

**文档位置**:
- `specs/` - 需求与任务规范
- `specs/001-rag-recommendation-system/tasks.md` - 任务清单
- `specs/001-rag-recommendation-system/plan.md` - 实施计划
- `docs/dev-logs/development-log.md` - 开发日志（问题修复记录）

---

## 关键约束（必须遵守）

### 1. 虚拟线程必须启用
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 不可关闭
```

### 2. 代码规范
- **不使用 Java Record**，使用 Lombok `@Data`、`@Builder`
- **MyBatis SQL 必须写在 XML 文件**，禁止使用 `@Select` 等注解
- **Service 接口和实现分离**：`service/XXXService.java` + `service/impl/XXXServiceImpl.java`
- **依赖注入统一使用 `@RequiredArgsConstructor`**（构造器注入），不使用 `@Autowired`

### 3. 架构约束
- **Redis Stack 是唯一向量存储**，禁止引入其他向量数据库
- **ModelGateway 是唯一模型调用出口**，禁止直连智谱 AI API
- **所有答案/推荐必须包含引用来源**（resourceId、chunkId、location、snippet）
- **LLM 不可用时必须降级**，返回检索结果 + 证据摘要，不得返回空白失败

### 4. 缓存与幂等
- **Embedding 结果必须按文本哈希缓存**（SHA-256），TTL 30 天
- **资源入库必须幂等**，基于内容哈希去重，防止重复计费

### 5. 性能要求
- 问答响应 P95 ≤ 5秒
- 资源入库 P95 ≤ 30秒
- 必须记录分段耗时（解析、Embedding、检索、生成）

---

## 技术栈

### 后端
- Java 21（虚拟线程）+ Spring Boot 3.5.9
- MyBatis 3.5（SQL 在 XML）
- LangChain4j 0.35（RAG 编排）
- Redis Stack（向量 + 缓存）
- MySQL 8（结构化数据）
- Lombok、Guava、Commons Lang3
- JWT (jjwt 0.12.3)

### 前端
- Vue 3.3 + TypeScript
- Ant Design Vue 4.x
- Vite 5.x + Pinia

### 测试
- JUnit 5 + Mockito
- Testcontainers（集成测试）

---

## 常用命令

### 环境准备
```bash
# 1. 启动基础设施（MySQL + Redis）
docker-compose up -d

# 2. 设置 Java 21（必须！）
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 后端开发
```bash
cd backend

# 编译
mvn clean compile

# 启动
mvn spring-boot:run

# 测试
mvn test

# 打包
mvn clean package
```

### 前端开发
```bash
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 构建
pnpm build
```

### 数据库
```bash
# 连接 MySQL
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123

# 连接 Redis
docker exec -it aetheris-redis redis-cli
```

---

## 项目结构（简化版）

```
backend/src/main/java/com/aetheris/rag/
├── controller/           # REST API
├── service/              # 业务接口
│   └── impl/             # 业务实现
├── mapper/               # MyBatis 接口
├── model/                # 数据模型（@Data）
├── dto/                  # 请求/响应 DTO
│   ├── request/
│   └── response/         # 包含 Citation.java
├── gateway/              # ModelGateway
│   ├── cache/            # EmbeddingCache
│   ├── retry/            # 重试策略
│   └── sanitize/         # 日志脱敏
├── config/               # Spring 配置
│   ├── RedisConfig.java
│   └── SecurityConfig.java
└── util/                 # 工具类

backend/src/main/resources/
├── application.yml       # 主配置
├── db/migration/         # Flyway 迁移脚本
└── mapper/               # MyBatis XML

frontend/src/
├── api/                  # API 调用
├── components/           # Vue 组件
├── views/                # 页面
├── stores/               # Pinia
└── router/               # 路由
```

---

## 已知问题

- **编译需 Java 21**：设置 `JAVA_HOME` 后再编译
- **EmbeddingGateway/ChatGateway**：当前为 stub 实现（返回 dummy 值），Phase 5 完整实现
- **Service 包结构**：接口在 `service/`，实现在 `service/impl/`

