#!/bin/bash

# Aetheris RAG 系统停止脚本（优化版）
# 用途: 支持命令行参数和交互式菜单的选择性停止

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

# ========================================
# 停止函数
# ========================================

stop_backend() {
    echo -e "${YELLOW}[停止后端]${NC}"

    BACKEND_PIDS=$(pgrep -f "spring-boot:run|AetherisRagApplication" || true)

    if [ -n "$BACKEND_PIDS" ]; then
        echo -e "${BLUE}找到后端进程: $BACKEND_PIDS${NC}"

        # 优雅关闭
        pkill -TERM -f "spring-boot:run|AetherisRagApplication" || true
        sleep 3

        # 检查并强制关闭
        REMAINING_PIDS=$(pgrep -f "spring-boot:run|AetherisRagApplication" || true)
        if [ -n "$REMAINING_PIDS" ]; then
            echo -e "${YELLOW}进程仍在运行，强制关闭...${NC}"
            pkill -9 -f "spring-boot:run|AetherisRagApplication" || true
            sleep 1
        fi

        echo -e "${GREEN}✅ 后端已停止${NC}"

        # 更新 PID 文件
        update_pids_json "backend"
        return 0
    else
        echo -e "${YELLOW}⚠️  未找到运行中的后端进程${NC}"
        return 1
    fi
}

stop_frontend() {
    echo -e "${YELLOW}[停止前端]${NC}"

    FRONTEND_PIDS=$(pgrep -f "vite.*frontend|npm.*dev|node.*vite" || true)

    if [ -n "$FRONTEND_PIDS" ]; then
        echo -e "${BLUE}找到前端进程: $FRONTEND_PIDS${NC}"

        # 优雅关闭
        pkill -TERM -f "vite.*frontend|npm.*dev|node.*vite" || true
        sleep 2

        # 检查并强制关闭
        REMAINING_PIDS=$(pgrep -f "vite.*frontend|npm.*dev|node.*vite" || true)
        if [ -n "$REMAINING_PIDS" ]; then
            echo -e "${YELLOW}进程仍在运行，强制关闭...${NC}"
            pkill -9 -f "vite.*frontend|npm.*dev|node.*vite" || true
            sleep 1
        fi

        echo -e "${GREEN}✅ 前端已停止${NC}"

        # 更新 PID 文件
        update_pids_json "frontend"
        return 0
    else
        echo -e "${YELLOW}⚠️  未找到运行中的前端进程${NC}"
        return 1
    fi
}

stop_docker() {
    echo -e "${YELLOW}[停止 Docker 服务]${NC}"

    # 检查 Docker 服务是否在运行
    if docker-compose ps | grep -q "Up"; then
        docker-compose down
        echo -e "${GREEN}✅ Docker 服务已停止${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  Docker 服务未运行${NC}"
        return 1
    fi
}

update_pids_json() {
    local service=$1

    if [ -f ".pids.json" ] && command -v jq &> /dev/null; then
        case $service in
            "backend")
                jq '.backend.pid = null | .backend.status = "stopped" | .backend.started_at = null' .pids.json > .pids.json.tmp
                mv .pids.json.tmp .pids.json
                ;;
            "frontend")
                jq '.frontend.pid = null | .frontend.status = "stopped" | .frontend.started_at = null' .pids.json > .pids.json.tmp
                mv .pids.json.tmp .pids.json
                ;;
        esac
    fi
}

show_help() {
    cat << EOF
${BLUE}用法:${NC}
  ./stop.sh [选项]

${BLUE}选项:${NC}
  --frontend-only      仅停止前端服务
  --backend-only       仅停止后端服务
  --all                停止所有服务（前端+后端）
  --docker-only        仅停止 Docker 服务（MySQL + Redis）
  --help, -h           显示此帮助信息

${BLUE}交互模式:${NC}
  无参数运行时进入交互模式，可选择要停止的服务

${BLUE}示例:${NC}
  ./stop.sh                    # 进入交互菜单
  ./stop.sh --frontend-only    # 仅停止前端
  ./stop.sh --backend-only     # 仅停止后端
  ./stop.sh --all              # 停止前端和后端

EOF
}

# ========================================
# 交互式菜单
# ========================================

show_menu() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Aetheris RAG 系统停止脚本${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${GREEN}请选择要停止的服务:${NC}"
    echo ""
    echo -e "  ${YELLOW}1${NC}. 停止前端"
    echo -e "  ${YELLOW}2${NC}. 停止后端"
    echo -e "  ${YELLOW}3${NC}. 停止所有服务（前端 + 后端）"
    echo -e "  ${YELLOW}4${NC}. 停止 Docker 服务（MySQL + Redis）"
    echo -e "  ${YELLOW}5${NC}. 停止所有（包括 Docker）"
    echo -e "  ${YELLOW}0${NC}. 退出"
    echo ""
    echo -ne "${BLUE}请输入选项 [0-5]: ${NC}"
}

handle_interactive_mode() {
    while true; do
        show_menu
        read choice

        case $choice in
            1)
                stop_frontend || true
                echo ""
                read -p "按 Enter 键继续..."
                ;;
            2)
                stop_backend || true
                echo ""
                read -p "按 Enter 键继续..."
                ;;
            3)
                echo -e "${YELLOW}正在停止所有服务（前端 + 后端）...${NC}"
                stop_backend || true
                stop_frontend || true
                echo ""
                read -p "按 Enter 键继续..."
                ;;
            4)
                stop_docker || true
                echo ""
                read -p "按 Enter 键继续..."
                ;;
            5)
                echo -e "${YELLOW}正在停止所有服务（包括 Docker）...${NC}"
                stop_backend || true
                stop_frontend || true
                stop_docker || true
                echo ""
                read -p "按 Enter 键继续..."
                ;;
            0)
                echo -e "${BLUE}👋 已退出${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ 无效选项，请输入 0-5${NC}"
                echo ""
                sleep 1
                ;;
        esac
    done
}

# ========================================
# 参数解析
# ========================================

parse_arguments() {
    # 默认：交互模式
    INTERACTIVE_MODE=true

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --frontend-only)
                stop_frontend
                exit $?
                ;;
            --backend-only)
                stop_backend
                exit $?
                ;;
            --all)
                stop_backend || true
                stop_frontend || true
                exit 0
                ;;
            --docker-only)
                stop_docker
                exit $?
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                echo -e "${RED}❌ 未知参数: $1${NC}"
                echo ""
                show_help
                exit 1
                ;;
        esac
        shift
    done
}

# ========================================
# 主流程
# ========================================

main() {
    # 解析命令行参数
    if [ $# -eq 0 ]; then
        # 无参数：进入交互模式
        handle_interactive_mode
    else
        # 有参数：解析并执行
        parse_arguments "$@"

        # 如果还有剩余参数（parse_arguments 没有退出），进入交互模式
        if [ "$INTERACTIVE_MODE" = true ]; then
            handle_interactive_mode
        fi
    fi
}

# 执行主流程
main "$@"
