#!/bin/bash

# Aetheris RAG 系统停止脚本
# 用途: 一键停止所有服务

set -e

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
echo -e "${BLUE}  Aetheris RAG 系统停止脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ========================================
# 停止后端
# ========================================
echo -e "${YELLOW}[1/3] 停止后端服务...${NC}"

if [ -f ".backend.pid" ]; then
    BACKEND_PID=$(cat .backend.pid)
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        kill $BACKEND_PID
        echo -e "${GREEN}✅ 后端已停止 (PID: $BACKEND_PID)${NC}"
    else
        echo -e "${YELLOW}⚠️  后端进程未运行${NC}"
    fi
    rm .backend.pid
else
    echo -e "${YELLOW}⚠️  未找到后端 PID 文件${NC}"

    # 尝试查找并杀死 Spring Boot 进程
    SPRING_PID=$(ps aux | grep 'spring-boot:run' | grep -v grep | awk '{print $2}')
    if [ -n "$SPRING_PID" ]; then
        kill $SPRING_PID
        echo -e "${GREEN}✅ 后端已停止 (PID: $SPRING_PID)${NC}"
    fi
fi

echo ""

# ========================================
# 停止前端
# ========================================
echo -e "${YELLOW}[2/3] 停止前端服务...${NC}"

if [ -f ".frontend.pid" ]; then
    FRONTEND_PID=$(cat .frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        kill $FRONTEND_PID
        echo -e "${GREEN}✅ 前端已停止 (PID: $FRONTEND_PID)${NC}"
    else
        echo -e "${YELLOW}⚠️  前端进程未运行${NC}"
    fi
    rm .frontend.pid
else
    echo -e "${YELLOW}⚠️  未找到前端 PID 文件${NC}"

    # 尝试查找并杀死 Vite 进程
    VITE_PID=$(ps aux | grep 'vite' | grep -v grep | awk '{print $2}')
    if [ -n "$VITE_PID" ]; then
        kill $VITE_PID
        echo -e "${GREEN}✅ 前端已停止 (PID: $VITE_PID)${NC}"
    fi
fi

echo ""

# ========================================
# 停止 Docker 服务（可选）
# ========================================
echo -e "${YELLOW}[3/3] 停止 Docker 服务...${NC}"

read -p "$(echo -e ${YELLOW}是否停止 MySQL 和 Redis? [y/N]: ${NC})" choice

if [[ "$choice" =~ ^[Yy]$ ]]; then
    docker-compose down
    echo -e "${GREEN}✅ Docker 服务已停止${NC}"
else
    echo -e "${BLUE}⏭️  Docker 服务保持运行${NC}"
fi

echo ""

# ========================================
# 完成
# ========================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  ✅ 停止完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}💡 提示:${NC}"
echo -e "  - 重新启动: ./start.sh"
echo -e "  - 仅启动 Docker: docker-compose up -d"
echo ""
