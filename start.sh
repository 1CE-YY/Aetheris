#!/bin/bash

# Aetheris RAG 系统快速启动脚本
# 用途: 一键启动所有服务（MySQL, Redis, Backend, Frontend）

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="/Users/hubin5/app/Aetheris"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Aetheris RAG 系统启动脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ========================================
# 步骤 1: 检查环境
# ========================================
echo -e "${YELLOW}[1/5] 检查环境...${NC}"

# 检查 Java 21
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java 未安装${NC}"
    exit 1
fi

# 设置 Java 21
export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ne 21 ]; then
    echo -e "${RED}❌ Java 版本错误: 当前版本 $JAVA_VERSION, 需要 Java 21${NC}"
    echo -e "${YELLOW}正在设置 Java 21...${NC}"
    export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.8/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH
    echo -e "${GREEN}✅ Java 21 已设置${NC}"
else
    echo -e "${GREEN}✅ Java 版本正确: $(java -version 2>&1 | head -n 1)${NC}"
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven 版本: $(mvn -version | head -n 1)${NC}"

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Node.js 版本: $(node -v)${NC}"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 版本: $(docker --version | cut -d' ' -f3)${NC}"

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose 版本: $(docker-compose --version | cut -d' ' -f4)${NC}"

echo ""

# ========================================
# 步骤 2: 创建 .env 文件
# ========================================
echo -e "${YELLOW}[2/5] 检查环境配置...${NC}"

if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo -e "${YELLOW}⚠️  .env 文件不存在，从 .env.example 创建...${NC}"
    cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
    echo -e "${GREEN}✅ .env 文件已创建${NC}"
    echo -e "${YELLOW}⚠️  请编辑 .env 文件，配置 ZHIPU_API_KEY 等关键参数！${NC}"
else
    echo -e "${GREEN}✅ .env 文件已存在${NC}"
fi

echo ""

# ========================================
# 步骤 3: 启动基础设施
# ========================================
echo -e "${YELLOW}[3/5] 启动基础设施 (MySQL + Redis)...${NC}"

# 检查是否已运行
if docker-compose ps | grep -q "Up"; then
    echo -e "${YELLOW}⚠️  Docker 服务已在运行，跳过启动${NC}"
else
    echo -e "${BLUE}正在启动 Docker Compose 服务...${NC}"
    docker-compose up -d

    # 等待服务健康
    echo -e "${BLUE}等待服务启动...${NC}"
    sleep 10

    # 检查服务状态
    if docker-compose ps | grep -q "Up (healthy)"; then
        echo -e "${GREEN}✅ 基础设施启动成功${NC}"
    else
        echo -e "${RED}❌ 基础设施启动失败${NC}"
        docker-compose ps
        exit 1
    fi
fi

echo ""

# ========================================
# 步骤 4: 启动后端
# ========================================
echo -e "${YELLOW}[4/5] 启动后端服务...${NC}"

cd "$PROJECT_ROOT/backend"

# 检查后端是否已编译
if [ ! -d "target" ] || [ ! -d "target/classes" ]; then
    echo -e "${BLUE}后端未编译，开始编译...${NC}"
    mvn clean compile
fi

# 启动后端（后台运行）
echo -e "${BLUE}启动 Spring Boot 应用...${NC}"
nohup mvn spring-boot:run > "$PROJECT_ROOT/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$PROJECT_ROOT/.backend.pid"

echo -e "${GREEN}✅ 后端启动中... (PID: $BACKEND_PID)${NC}"
echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/backend.log${NC}"

# 等待后端启动
echo -e "${BLUE}等待后端启动 (30秒)...${NC}"
sleep 30

# 检查后端是否启动成功
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 后端启动成功${NC}"
else
    echo -e "${YELLOW}⚠️  后端可能还在启动中，请检查日志${NC}"
fi

echo ""

# ========================================
# 步骤 5: 启动前端
# ========================================
echo -e "${YELLOW}[5/5] 启动前端服务...${NC}"

cd "$PROJECT_ROOT/frontend"

# 检查 node_modules
if [ ! -d "node_modules" ]; then
    echo -e "${BLUE}node_modules 不存在，开始安装依赖...${NC}"
    npm install
fi

# 启动前端（后台运行）
echo -e "${BLUE}启动 Vite 开发服务器...${NC}"
nohup npm run dev > "$PROJECT_ROOT/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > "$PROJECT_ROOT/.frontend.pid"

echo -e "${GREEN}✅ 前端启动中... (PID: $FRONTEND_PID)${NC}"
echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/frontend.log${NC}"

# 等待前端启动
echo -e "${BLUE}等待前端启动 (10秒)...${NC}"
sleep 10

echo ""

# ========================================
# 完成
# ========================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  ✅ 所有服务启动完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}🌐 前端访问地址: http://localhost:5173${NC}"
echo -e "${BLUE}🔧 后端 API 地址: http://localhost:8080${NC}"
echo ""
echo -e "${YELLOW}📝 查看日志:${NC}"
echo -e "  - 后端: tail -f $PROJECT_ROOT/logs/backend.log"
echo -e "  - 前端: tail -f $PROJECT_ROOT/logs/frontend.log"
echo -e "  - Docker: docker-compose logs -f"
echo ""
echo -e "${YELLOW}🛑 停止服务:${NC}"
echo -e "  - 停止所有: ./stop.sh"
echo -e "  - 停止后端: kill \$(cat .backend.pid)"
echo -e "  - 停止前端: kill \$(cat .frontend.pid)"
echo ""
echo -e "${BLUE}📚 完整启动指南: docs/STARTUP_GUIDE.md${NC}"
echo ""
