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

# 方法1: 使用 pkill 查找并杀死所有相关进程
BACKEND_PIDS=$(pgrep -f "spring-boot:run|AetherisRagApplication" || true)

if [ -n "$BACKEND_PIDS" ]; then
    echo -e "${BLUE}找到后端进程: $BACKEND_PIDS${NC}"
    # 先尝试优雅关闭
    pkill -TERM -f "spring-boot:run|AetherisRagApplication" || true
    sleep 3

    # 检查进程是否还在，如果还在则强制杀死
    REMAINING_PIDS=$(pgrep -f "spring-boot:run|AetherisRagApplication" || true)
    if [ -n "$REMAINING_PIDS" ]; then
        echo -e "${YELLOW}进程仍在运行，强制关闭...${NC}"
        pkill -9 -f "spring-boot:run|AetherisRagApplication" || true
        sleep 1
    fi

    echo -e "${GREEN}✅ 后端已停止${NC}"
else
    echo -e "${YELLOW}⚠️  未找到运行中的后端进程${NC}"
fi

# 更新 PID 文件状态
if [ -f ".pids.json" ] && command -v jq &> /dev/null; then
    jq '.backend.pid = null | .backend.status = "stopped" | .backend.started_at = null' .pids.json > .pids.json.tmp
    mv .pids.json.tmp .pids.json
fi

echo ""

# ========================================
# 停止前端
# ========================================
echo -e "${YELLOW}[2/3] 停止前端服务...${NC}"

# 使用 pkill 查找并杀死所有相关进程
FRONTEND_PIDS=$(pgrep -f "vite.*frontend|npm.*dev|node.*vite" || true)

if [ -n "$FRONTEND_PIDS" ]; then
    echo -e "${BLUE}找到前端进程: $FRONTEND_PIDS${NC}"
    # 优雅关闭
    pkill -TERM -f "vite.*frontend|npm.*dev|node.*vite" || true
    sleep 2

    # 检查进程是否还在，如果还在则强制杀死
    REMAINING_PIDS=$(pgrep -f "vite.*frontend|npm.*dev|node.*vite" || true)
    if [ -n "$REMAINING_PIDS" ]; then
        echo -e "${YELLOW}进程仍在运行，强制关闭...${NC}"
        pkill -9 -f "vite.*frontend|npm.*dev|node.*vite" || true
        sleep 1
    fi

    echo -e "${GREEN}✅ 前端已停止${NC}"
else
    echo -e "${YELLOW}⚠️  未找到运行中的前端进程${NC}"
fi

# 更新 PID 文件状态
if [ -f ".pids.json" ] && command -v jq &> /dev/null; then
    jq '.frontend.pid = null | .frontend.status = "stopped" | .frontend.started_at = null' .pids.json > .pids.json.tmp
    mv .pids.json.tmp .pids.json
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
