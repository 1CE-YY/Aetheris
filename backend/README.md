# Aetheris RAG Backend

Spring Boot 3.5.9 后端服务，提供 RESTful API 和 RAG 核心功能。

---

## 📋 技术栈

- **Spring Boot** 3.5.9
- **Java** 21
- **MyBatis**
- **LangChain4j**
- **Redis Stack**
- **MySQL**
- **JWT**
- **Lombok**

---

## 🚀 快速开始

运行后

- **API 地址**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health
- **API 文档**: http://localhost:8080/swagger-ui.html（待集成）

---

## 📂 项目结构

```
backend/src/main/java/com/aetheris/rag/
├── controller/           # REST API 控制器
├── service/              # 业务接口
├── common/               # 公共组件
│   └── response/         # 统一响应格式
├── service/impl/         # 业务实现
├── mapper/               # MyBatis 接口
├── exception/            # 自定义异常
├── entity/               # 数据模型
├── dto/                  # 请求/响应 DTO
│   ├── request/          # 请求 DTO
│   └── response/         # 响应 DTO
├── gateway/              # ModelGateway 框架
│   ├── cache/                   # 缓存
│   ├── retry/                   # 重试
│   └── sanitize/                # 日志脱敏
├── config/               # Spring 配置
├── util/                 # 工具类
├── validation/           # 自定义校验
├── constants/            # 常量定义
└── schedule/             # 定时任务
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
- **所有注释必须使用中文**（包括日志）
