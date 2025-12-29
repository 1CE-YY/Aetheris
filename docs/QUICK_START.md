# 🚀 Aetheris 快速启动卡片

## 一键启动（推荐）

```bash
cd /Users/hubin5/app/Aetheris
./start.sh
```

启动脚本会自动：
1. ✅ 检查环境（Java 21, Maven, Node.js, Docker）
2. ✅ 创建 .env 配置文件（如不存在）
3. ✅ 启动 MySQL + Redis（Docker）
4. ✅ 启动后端（Spring Boot）
5. ✅ 启动前端（Vite）

---

## 手动启动（分步）

### 1️⃣ 设置 Java 21（必须！）
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### 2️⃣ 启动基础设施
```bash
docker-compose up -d
```

### 3️⃣ 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 4️⃣ 启动前端（新终端）
```bash
cd frontend
npm run dev
```

---

## 访问地址

🌐 **前端**: http://localhost:5173
🔧 **后端 API**: http://localhost:8080
📊 **MySQL**: localhost:3306
📦 **Redis**: localhost:6379

---

## 停止服务

```bash
./stop.sh
```

---

## Phase 1-2 验收测试

### ✅ 测试用户注册
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Password123!"}'
```

### ✅ 测试用户登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123!"}'
```

### ✅ 检查虚拟线程
查看后端日志，确认输出：
```
✅ 虚拟线程已启用 (Virtual threads enabled)
```

---

## 常见问题

❌ **Java 版本错误**
```bash
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

❌ **MySQL 连接失败**
```bash
docker-compose ps
docker-compose logs mysql
```

❌ **端口被占用**
```bash
lsof -i :8080  # 后端
lsof -i :5173  # 前端
lsof -i :3306  # MySQL
```

---

## 查看日志

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

## 📚 详细文档

- **完整启动指南**: `docs/STARTUP_GUIDE.md`
- **项目文档**: `CLAUDE.md`
- **任务清单**: `specs/001-rag-recommendation-system/tasks.md`
