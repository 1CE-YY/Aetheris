# 开发日志

**更新日期**: 2025-12-26

## 编译问题修复记录

### 问题汇总
修复了 6 个编译错误，项目现已可成功编译。

### 主要问题及解决方案

#### 1. Java 版本不匹配
- **问题**: Maven 使用 Java 8，项目需要 Java 21
- **解决**: 编译前设置环境变量
  ```bash
  export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH
  ```

#### 2. 依赖包导入错误
- **ModelRetryStrategy.java**: 改用自定义 `ModelException`，删除不存在的 `ModelUnauthorizedException`
- **EmbeddingGateway & ChatGateway**: 暂时使用 stub 实现，返回 dummy 值，完整实现推迟到 Phase 5

#### 3. 内部类引用错误
- **CitationLocation.java**: 修复 `@JsonSubTypes` 注解，使用完整内部类路径

#### 4. 序列化器类型错误
- **FloatArrayRedisSerializer.java**: 重写为直接实现 `RedisSerializer<float[]>` 接口

#### 5. JWT API 过时
- **JwtUtil.java**: 将 `parserBuilder()` 改为 `parser()`

#### 6. 缺少导入
- **SecurityConfig.java**: 添加 `UsernamePasswordAuthenticationToken` 导入

### 验收结果
- ✅ 编译成功 (BUILD SUCCESS)
- ✅ 无编译错误
- ⚠️  MySQL 驱动警告（私有仓库无法下载，不影响编译）

---

## Phase 1-2 实施完成情况

### 已完成内容

#### Phase 1: 项目初始化
- ✅ Spring Boot 3.5 + Java 21 + 虚拟线程
- ✅ Vue 3.3 + TypeScript + Vite
- ✅ Docker Compose (MySQL 8 + Redis Stack)
- ✅ 数据库表结构 (7 个表)

#### Phase 2: 基础设施层
- ✅ **ModelGateway** (统一模型调用接口)
  - EmbeddingGateway (stub 实现)
  - ChatGateway (stub 实现)
  - EmbeddingCache (Redis 缓存)
  - ModelRetryStrategy (重试策略)
  - LogSanitizer (日志脱敏)

- ✅ **Citations 结构** (统一引用格式)
  - 支持 PDF 页码范围
  - 支持 Markdown 章节路径

- ✅ **用户认证**
  - AuthService 接口 + 实现
  - JWT 生成与验证
  - BCrypt 密码加密
  - AuthController (/api/auth/*)

- ✅ **工具类**
  - HashUtil (SHA-256)
  - TextNormalizer (文本规范化)
  - PerformanceTimer (性能计时)

### 代码统计
- Java 源文件: 25 个
- 测试文件: 8 个
- 总代码量: ~2500 行 (不含测试)

### 待完成事项
- Phase 5: 完整实现 EmbeddingGateway 和 ChatGateway（当前为 stub）
- 配置系统默认 Java 版本为 21
- 解决 MySQL 驱动下载问题（私有仓库配置）

---

## 架构优化记录

### Redis 序列化优化
- 创建两个 `RedisTemplate` bean:
  - `RedisTemplate<String, Object>` - 通用缓存
  - `RedisTemplate<String, float[]>` - Embedding 向量专用

### 依赖注入规范化
- 统一使用 `@RequiredArgsConstructor` 替代 `@Autowired`
- 采用构造器注入模式
- 修改文件: AuthController, AuthServiceImpl, SecurityConfig

### 包结构调整
- 按标准 Java 项目结构重新组织:
  - `service/` - 接口
  - `service/impl/` - 实现类

---

## 验收测试建议

### 启动后端
```bash
# 1. 设置 Java 21
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 2. 启动后端
cd backend
mvn spring-boot:run
```

### 测试认证 API
```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```
