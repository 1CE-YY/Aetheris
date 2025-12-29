# Phase 1-2 验收检查清单

**分支**: `001-rag-recommendation-system`
**验收日期**: ___________
**验收人**: ___________

---

## 📋 验收前准备

### 环境检查

- [ ] Java 21 已设置（`java -version` 显示 21.0.8）
- [ ] Maven 3.6+ 已安装（`mvn -version`）
- [ ] Node.js 16+ 已安装（`node -v`）
- [ ] Docker 已安装并运行（`docker ps`）
- [ ] Docker Compose 已安装（`docker-compose --version`）

### 配置文件检查

- [ ] `.env` 文件已创建（从 `.env.example` 复制）
- [ ] `ZHIPU_API_KEY` 已配置（或保留 stub 值）
- [ ] `JWT_SECRET` 已设置（开发环境可用默认值）
- [ ] MySQL 密码配置正确（`aetheris123`）

---

## 🐳 基础设施验收（Phase 1）

### 1.1 Docker 服务启动

**执行命令**:
```bash
docker-compose up -d
```

验收项：
- [ ] MySQL 容器启动成功（`docker-compose ps` 显示 `Up (healthy)`）
- [ ] Redis Stack 容器启动成功（`docker-compose ps` 显示 `Up (healthy)`）
- [ ] 端口正确暴露（MySQL: 3306, Redis: 6379）

**验证命令**:
```bash
# 测试 MySQL
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123 -e "SELECT 1"

# 测试 Redis
docker exec -it aetheris-redis redis-cli -a "" ping
# 预期输出: PONG
```

- [ ] MySQL 连接测试通过
- [ ] Redis 连接测试通过

### 1.2 数据库表结构

**执行命令**:
```bash
docker exec -it aetheris-mysql mysql -u aetheris -paetheris123 aetheris_rag
mysql> SHOW TABLES;
```

验收项：
- [ ] `users` 表存在
- [ ] `resources` 表存在
- [ ] `resource_chunks` 表存在
- [ ] `user_behaviors` 表存在
- [ ] `user_profiles` 表存在
- [ ] `eval_queries` 表存在
- [ ] `eval_runs` 表存在
- [ ] `flyway_schema_history` 表存在

**验证表结构**:
```sql
DESCRIBE users;
DESCRIBE resources;
DESCRIBE resource_chunks;
```

- [ ] `users` 表字段正确（id, username, email, password_hash, created_at, updated_at, last_active_at）
- [ ] `resources` 表字段正确（id, title, tags, file_type, file_path, file_size, description, content_hash, uploaded_by, chunk_count, vectorized）
- [ ] `resource_chunks` 表字段正确（id, resource_id, chunk_index, chunk_text, location_info, page_start, page_end, chapter_path, text_hash, vectorized）

### 1.3 项目结构

**后端项目结构**:
```bash
tree backend/src/main/java/com/aetheris/rag -L 1
```

验收项：
- [ ] `config/` 目录存在
- [ ] `controller/` 目录存在
- [ ] `dto/` 目录存在
- [ ] `gateway/` 目录存在
- [ ] `mapper/` 目录存在
- [ ] `model/` 目录存在
- [ ] `service/` 目录存在
- [ ] `util/` 目录存在

**前端项目结构**:
```bash
ls frontend/src/
```

验收项：
- [ ] `api/` 目录存在
- [ ] `components/` 目录存在
- [ ] `views/` 目录存在
- [ ] `stores/` 目录存在
- [ ] `router/` 目录存在

---

## 🔧 基础设施层验收（Phase 2）

### 2.1 后端启动

**执行命令**:
```bash
cd backend
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn spring-boot:run
```

验收项：
- [ ] 编译成功，无错误
- [ ] Spring Boot 启动成功
- [ ] 启动日志显示虚拟线程已启用
- [ ] MySQL 连接成功
- [ ] Redis 连接成功
- [ ] Flyway 迁移成功（无错误）

**关键日志检查**:
```
✅ Virtual threads enabled
✅ MySQL connection established
✅ Redis connection established
✅ Flyway migration completed
✅ JWT filter registered
```

### 2.2 ModelGateway 框架

**文件检查**:
- [ ] `ModelGateway.java` 接口存在（`gateway/ModelGateway.java`）
- [ ] `EmbeddingGateway.java` 存在（`gateway/EmbeddingGateway.java`）
  - [ ] 当前为 stub 实现（返回 dummy embedding）
  - [ ] 包含缓存接口（`EmbeddingCache`）
  - [ ] 包含重试策略（`ModelRetryStrategy`）
  - [ ] 包含日志脱敏（`LogSanitizer`）
- [ ] `ChatGateway.java` 存在（`gateway/ChatGateway.java`）
  - [ ] 当前为 stub 实现（返回 dummy response）
  - [ ] 包含降级策略说明

**验证**:
```bash
curl http://localhost:8080/actuator/health
```

- [ ] 后端健康检查通过

### 2.3 Citations 结构

**文件检查**:
- [ ] `Citation.java` 存在（`dto/response/Citation.java`）

**字段验证**:
查看文件内容，确认包含以下字段：
- [ ] `resourceId` (Long 或 UUID)
- [ ] `resourceTitle` (String)
- [ ] `chunkId` (Long 或 UUID)
- [ ] `chunkIndex` (Integer)
- [ ] `location` (Location 对象，包含 pageStart/pageEnd 或 chapterPath)
- [ ] `snippet` (String, 100-200 字符)
- [ ] `score` (Double)

### 2.4 用户认证系统

**文件检查**:
- [ ] `User.java` 存在（`model/User.java`）
  - [ ] 使用 Lombok `@Data` 注解
  - [ ] 使用 Lombok `@Builder` 注解
- [ ] `UserMapper.java` 存在（`mapper/UserMapper.java`）
- [ ] `UserMapper.xml` 存在（`resources/mapper/UserMapper.xml`）
- [ ] `AuthService.java` 接口存在（`service/auth/AuthService.java`）
- [ ] `AuthServiceImpl.java` 实现存在（`service/auth/impl/AuthServiceImpl.java` 或 `service/auth/AuthService.java`）
- [ ] `AuthController.java` 存在（`controller/AuthController.java`）
- [ ] `SecurityConfig.java` 存在（`config/SecurityConfig.java`）
- [ ] `JwtUtil.java` 存在（`util/JwtUtil.java`）

### 2.5 工具类

**文件检查**:
- [ ] `HashUtil.java` 存在（`util/HashUtil.java`）
  - [ ] 实现 SHA-256 哈希计算
- [ ] `TextNormalizer.java` 存在（`util/TextNormalizer.java`）
  - [ ] 实现文本规范化（去除冗余空白）
- [ ] `PerformanceTimer.java` 存在（`util/PerformanceTimer.java`）
  - [ ] 实现分段耗时记录

---

## ✅ 功能测试验收

### 测试 1: 用户注册

**命令**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

验收项：
- [ ] HTTP 状态码 200
- [ ] 返回 JWT token
- [ ] 返回用户信息（username, email）
- [ ] 数据库 `users` 表中成功插入记录

**验证数据库**:
```sql
SELECT id, username, email FROM users WHERE email = 'test@example.com';
```

### 测试 2: 用户登录

**命令**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

验收项：
- [ ] HTTP 状态码 200
- [ ] 返回 JWT token
- [ ] 密码错误时返回 401

### 测试 3: JWT 验证

**命令**:
```bash
TOKEN="从注册/登录响应中复制的token"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile
```

验收项：
- [ ] 有效 token 返回用户信息
- [ ] 无效/过期 token 返回 401

### 测试 4: 密码安全性

**验证**:
```sql
SELECT password_hash FROM users WHERE email = 'test@example.com';
```

验收项：
- [ ] 密码已哈希存储（BCrypt，不以明文存储）
- [ ] 密码哈希以 `$2a$` 或 `$2b$` 开头

---

## 📊 代码质量检查

### 编码规范

- [ ] Java 代码遵循 Google Java Style（2 空格缩进）
- [ ] 使用 Lombok 注解（`@Data`, `@Builder`）而非 Java Record
- [ ] MyBatis SQL 定义在 XML 文件中，非注解
- [ ] Service 接口和实现分离
- [ ] 使用 `@RequiredArgsConstructor` 构造器注入，非 `@Autowired`

### 架构约束

- [ ] Redis Stack 是唯一向量存储方案
- [ ] ModelGateway 是唯一模型调用出口
- [ ] 虚拟线程已启用（`spring.threads.virtual.enabled=true`）
- [ ] 所有答案/推荐包含 Citations 结构
- [ ] Embedding 结果按文本哈希缓存（SHA-256）

---

## 🎯 Phase 1-2 验收结论

### 完成情况

- Phase 1（项目初始化）: [ ] 通过 / [ ] 不通过
- Phase 2（基础设施层）: [ ] 通过 / [ ] 不通过

### 不通过原因

（如验收不通过，请记录原因）

1. ___________________________________________________________
2. ___________________________________________________________
3. ___________________________________________________________

### 问题修复跟踪

| 问题编号 | 问题描述 | 负责人 | 状态 | 预计完成日期 |
|---------|---------|--------|------|------------|
| P1-001  |         |        |      |            |
| P1-002  |         |        |      |            |

### 下一步计划

完成 Phase 1-2 验收后，继续：

- [ ] Phase 3: 用户账户与行为记录 (T026-T034)
- [ ] Phase 4: 资源入库与切片 (T035-T048)
- [ ] Phase 5: RAG 问答与引用 (T049-T059)

---

## 📝 备注

（验收过程中的其他观察和建议）

___________________________________________________________
___________________________________________________________
___________________________________________________________

---

**验收人签字**: ___________  **日期**: ___________
**审核人签字**: ___________  **日期**: ___________
