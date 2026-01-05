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

    # 检查服务状态（更宽松的检查）
    if docker-compose ps | grep -q "Up"; then
        # 服务已启动，快速检查健康状态
        echo -e "${BLUE}检查服务健康状态...${NC}"

        # 快速检查（最多等待 10 秒）
        for i in {1..2}; do
            if docker-compose ps | grep -q "healthy"; then
                echo -e "${GREEN}✅ 基础设施启动成功${NC}"
                break
            fi
            [ $i -eq 1 ] && echo -e "${YELLOW}等待服务就绪...${NC}"
            sleep 5
        done

        # 如果仍未健康，显示提示但继续启动
        if ! docker-compose ps | grep -q "healthy"; then
            echo -e "${YELLOW}⚠️  服务启动中，继续启动后端...${NC}"
        fi
    else
        echo -e "${RED}❌ 基础设施启动失败${NC}"
        docker-compose ps
        # 不退出，让用户决定是否继续
        read -p "$(echo -e ${YELLOW}是否继续启动后端? [y/N]: ${NC})" choice
        if [[ ! "$choice" =~ ^[Yy]$ ]]; then
            exit 1
        fi
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

# 加载 .env 文件中的环境变量
echo -e "${BLUE}加载环境变量...${NC}"
if [ -f "$PROJECT_ROOT/.env" ]; then
    # 使用更安全的方式加载 .env 文件
    # 忽略注释行和空行，正确处理特殊字符
    while IFS='=' read -r key value; do
        # 跳过注释和空行
        [[ "$key" =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue

        # 去除值前后的空白和引号
        value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed 's/^"//;s/"$//')

        # 导出环境变量
        export "$key=$value"

    done < "$PROJECT_ROOT/.env"

    echo -e "${GREEN}✅ 环境变量已从 .env 加载${NC}"
    echo -e "${BLUE}提示: 如果 .env 文件不存在，将使用 application.yml 默认配置${NC}"
else
    echo -e "${YELLOW}⚠️  .env 文件不存在，使用 application.yml 默认配置${NC}"
fi

# 启动后端（后台运行）
echo -e "${BLUE}启动 Spring Boot 应用...${NC}"
nohup mvn spring-boot:run > "$PROJECT_ROOT/logs/backend.log" 2>&1 &
BACKEND_PID=$!
STARTED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# 更新PID文件
if command -v jq &> /dev/null; then
    # 使用 jq 更新 JSON（推荐）
    tmp=$(mktemp)
    jq '.backend.pid = '$BACKEND_PID' | .backend.status = "running" | .backend.started_at = "'$STARTED_AT'"' "$PROJECT_ROOT/.pids.json" > "$tmp"
    mv "$tmp" "$PROJECT_ROOT/.pids.json"
else
    # 回退到 sed（简化版）
    sed -i '' 's/"pid": null/"pid": '$BACKEND_PID'/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"pid": [0-9]*/"pid": '$BACKEND_PID'/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"status": "stopped"/"status": "running"/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"started_at": null/"started_at": "'$STARTED_AT'"/' "$PROJECT_ROOT/.pids.json"
fi

echo -e "${GREEN}✅ 后端启动中... (PID: $BACKEND_PID)${NC}"
echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/backend.log${NC}"
echo -e "${YELLOW}📄 查看进程状态: cat $PROJECT_ROOT/.pids.json | jq${NC}"

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
STARTED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# 更新PID文件
if command -v jq &> /dev/null; then
    # 使用 jq 更新 JSON（推荐）
    tmp=$(mktemp)
    jq '.frontend.pid = '$FRONTEND_PID' | .frontend.status = "running" | .frontend.started_at = "'$STARTED_AT'"' "$PROJECT_ROOT/.pids.json" > "$tmp"
    mv "$tmp" "$PROJECT_ROOT/.pids.json"
else
    # 回退到 sed（简化版）
    sed -i '' 's/"pid": null/"pid": '$FRONTEND_PID'/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"pid": [0-9]*/"pid": '$FRONTEND_PID'/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"status": "stopped"/"status": "running"/' "$PROJECT_ROOT/.pids.json"
    sed -i '' 's/"started_at": null/"started_at": "'$STARTED_AT'"/' "$PROJECT_ROOT/.pids.json"
fi

echo -e "${GREEN}✅ 前端启动中... (PID: $FRONTEND_PID)${NC}"
echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/frontend.log${NC}"
echo -e "${YELLOW}📄 查看进程状态: cat $PROJECT_ROOT/.pids.json | jq${NC}"

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
echo -e "  - 查看进程状态: cat .pids.json | jq"
echo ""
echo -e "${BLUE}📚 完整启动指南: docs/STARTUP_GUIDE.md${NC}"
echo ""
