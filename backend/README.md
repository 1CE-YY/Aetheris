# Aetheris RAG Backend

Spring Boot 3.5.9 后端服务，提供 RESTful API 和 RAG 核心功能。

---

## 📋 技术栈

- **Spring Boot** 3.5.9
- **Java** 21（虚拟线程）
- **MyBatis** 3.5
- **LangChain4j** 0.35
- **Redis Stack**（向量存储）
- **MySQL** 8（结构化数据）
- **JWT**（jjwt 0.12.3）
- **Lombok**（代码简化）

---

## 🚀 快速开始

### 1. 设置 Java 21（必须！）

```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. 编译项目

```bash
mvn clean compile
```

### 3. 运行应用

```bash
# 开发模式（热重载）
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/rag-backend-1.0.0.jar
```

### 4. 访问应用

- **API 地址**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health
- **API 文档**: http://localhost:8080/swagger-ui.html（待集成）

---

## 📂 项目结构

```
backend/src/main/java/com/aetheris/rag/
├── controller/           # REST API 控制器
│   └── AuthController.java
├── service/              # 业务接口
│   └── AuthService.java
├── service/impl/         # 业务实现
│   └── AuthServiceImpl.java
├── mapper/               # MyBatis 接口
│   └── UserMapper.java
├── model/                # 数据模型
│   └── User.java
├── dto/                  # 请求/响应 DTO
│   ├── request/          # 请求 DTO
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/         # 响应 DTO
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       └── Citation.java
├── gateway/              # ModelGateway 框架
│   ├── EmbeddingGateway.java    # Embedding stub
│   ├── ChatGateway.java         # Chat stub
│   ├── cache/                   # 缓存
│   │   └── EmbeddingCache.java
│   ├── retry/                   # 重试
│   │   └── ModelRetryStrategy.java
│   └── sanitize/                # 日志脱敏
│       └── LogSanitizer.java
├── config/               # Spring 配置
│   ├── SecurityConfig.java       # 安全配置
│   └── RedisConfig.java         # Redis 配置
├── util/                 # 工具类
│   ├── JwtUtil.java
│   ├── HashUtil.java
│   ├── TextNormalizer.java
│   └── PerformanceTimer.java
└── validation/           # 自定义校验
    ├── PasswordComplexity.java
    └── PasswordComplexityValidator.java
```

---

## 🔧 配置文件

### application.yml

主配置文件，包含：

- **数据源配置**：MySQL 连接信息
- **Redis 配置**：Redis Stack 连接和向量索引
- **JWT 配置**：密钥和过期时间
- **MyBatis 配置**：Mapper XML 位置
- **Actuator 配置**：监控端点
- **日志配置**：日志级别和输出

### application-dev.yml

开发环境专用配置（可覆盖 application.yml）。

---

## 🧪 测试

### 运行所有测试

```bash
mvn test
```

### 运行特定测试

```bash
mvn test -Dtest=AuthServiceTest
```

### 测试覆盖率

**当前覆盖率**: 39.2%（78 个测试方法）

已测试组件：
- Service 层：AuthService
- DTO 层：Citation, CitationLocation
- Util 层：HashUtil, JwtUtil, TextNormalizer, 等
- Gateway 层：EmbeddingCache, ModelRetryStrategy
- Validation 层：PasswordComplexityValidator

---

## 📊 API 端点

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

Response 201:
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

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { ... }
}
```

### 受保护的 API

需要 JWT Token：

```
Authorization: Bearer <token>
```

---

## 🔒 安全机制

### JWT 认证

- **算法**: HS256
- **密钥**: 配置在 `JWT_SECRET` 环境变量
- **过期时间**: 86400 秒（24 小时）
- **Token 生成**: `JwtUtil.generateToken(userId)`
- **Token 验证**: `JwtUtil.validateToken(token)`

### 密码加密

- **算法**: BCrypt
- **强度**: 10 rounds
- **实现**: `BCryptPasswordEncoder`

### 权限控制

- **Spring Security** 配置在 `SecurityConfig.java`
- **无状态**: `SessionCreationPolicy.STATELESS`
- **CSRF**: 已禁用（API 不需要）
- **CORS**: 允许 `localhost:5173` 和 `localhost:3000`

---

## 🗄️ 数据库

### Flyway 迁移

迁移脚本位置：`src/main/resources/db/migration/`

- `V1__init_schema.sql` - 初始化数据库表结构

### MyBatis Mapper

Mapper XML 位置：`src/main/resources/mapper/`

- `UserMapper.xml` - 用户相关 SQL

**重要**：
- ✅ 所有 SQL 必须写在 XML 文件
- ❌ 禁止使用 `@Select`、`@Insert` 等注解
- ✅ 使用 `ResultMap` 映射结果

---

## 🔴 Redis Stack

### 连接配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 向量索引

**索引名称**: `chunk_vector_index`

**配置**：
- 维度：1024（Embedding-v2）
- 距离度量：COSINE
- HNSW 参数：M=16, EF_CONSTRUCTION=128

### 使用场景

1. **Embedding 缓存**：文本哈希 → 向量（减少 API 调用）
2. **向量检索**：语义搜索（RediSearch）
3. **会话缓存**：可选（Spring Session）

---

## 🧩 ModelGateway 框架

### EmbeddingGateway

**状态**: Stub 实现（Phase 1-2）

**用途**：将文本转换为向量嵌入

**Phase 5 完整实现**：
- 调用智谱 AI Embedding API
- 重试机制（3 次）
- 缓存机制（SHA-256 哈希）
- 超时控制（30 秒）

**当前返回**：Dummy 向量 `[0.1f, 0.2f, ...]`

### ChatGateway

**状态**: Stub 实现（Phase 1-2）

**用途**：调用大语言模型进行对话

**Phase 5 完整实现**：
- 调用智谱 AI Chat API
- RAG Prompt 模板
- 流式响应（SSE）
- 引用来源注入

**当前返回**：Dummy 响应 `"This is a stub response"`

---

## 📝 代码规范

### 必须遵守

1. **不使用 Java Record**，使用 Lombok `@Data`、`@Builder`
2. **MyBatis SQL 必须在 XML**，禁止使用注解
3. **Service 接口和实现分离**：`service/XXXService.java` + `service/impl/XXXServiceImpl.java`
4. **依赖注入使用 `@RequiredArgsConstructor`**，禁止使用 `@Autowired`
5. **虚拟线程必须启用**：`spring.threads.virtual.enabled: true`

### 命名规范

- **类名**: PascalCase（如 `UserService`）
- **方法名**: camelCase（如 `getUserById`）
- **常量**: UPPER_SNAKE_CASE（如 `MAX_RETRIES`）
- **包名**: 全小写（如 `service.impl`）

### 注释规范

- **类注释**: Javadoc `/** ... */`
- **方法注释**: Javadoc `/** ... */`
- **字段注释**: Javadoc `/** ... */`
- **行内注释**: `// ...`

---

## 🐛 常见问题

### Q: Maven 编译失败，提示 Java 版本错误？

**A**: 设置 JAVA_HOME 环境变量：

```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### Q: Redis 连接被拒绝？

**A**: 检查密码配置并重启 Redis：

```bash
# 检查 .env
grep REDIS_PASSWORD .env

# 重启 Redis
docker-compose restart redis-stack
```

### Q: 数据库迁移失败？

**A**: 清空数据库重新迁移：

```bash
docker exec -i aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag -e "
DROP TABLE IF EXISTS flyway_schema_history;
"
```

---

## 📚 相关文档

- [项目根 README](../README.md)
- [API 规范（OpenAPI）](../specs/001-rag-recommendation-system/contracts/openapi.yaml)
- [数据模型设计](../specs/001-rag-recommendation-system/data-model.md)
- [验收报告](../docs/PHASE1_2_ACCEPTANCE_REPORT.md)

---

## 📮 联系方式

- 问题反馈：提交 GitHub Issue
- 技术支持：1307792296@qq.com

---

**最后更新**: 2025-12-29
**版本**: v1.0.0
