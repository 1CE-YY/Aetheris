#!/bin/bash

# Aetheris RAG 系统启动脚本（优化版）
# 用途: 支持命令行参数和交互式菜单的选择性启动

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="/Users/hubin5/app/Aetheris"
cd "$PROJECT_ROOT"

# ========================================
# 环境检查函数
# ========================================
check_environment() {
    echo -e "${YELLOW}[环境检查]${NC}"

    # 检查 Java 21
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java 未安装${NC}"
        return 1
    fi

    # 设置 Java 21
    export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ne 21 ]; then
        echo -e "${RED}❌ Java 版本错误: 当前版本 $JAVA_VERSION, 需要 Java 21${NC}"
        echo -e "${YELLOW}正在设置 Java 21...${NC}"
        export JAVA_HOME=/Users/hubin5/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home
        export PATH=$JAVA_HOME/bin:$PATH
        echo -e "${GREEN}✅ Java 21 已设置${NC}"
    else
        echo -e "${GREEN}✅ Java 版本正确: $(java -version 2>&1 | head -n 1)${NC}"
    fi

    # 检查 Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven 未安装${NC}"
        return 1
    fi
    echo -e "${GREEN}✅ Maven 版本: $(mvn -version | head -n 1)${NC}"

    # 检查 Node.js
    if ! command -v node &> /dev/null; then
        echo -e "${RED}❌ Node.js 未安装${NC}"
        return 1
    fi
    echo -e "${GREEN}✅ Node.js 版本: $(node -v)${NC}"

    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker 未安装${NC}"
        return 1
    fi
    echo -e "${GREEN}✅ Docker 版本: $(docker --version | cut -d' ' -f3)${NC}"

    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}❌ Docker Compose 未安装${NC}"
        return 1
    fi
    echo -e "${GREEN}✅ Docker Compose 版本: $(docker-compose --version | cut -d' ' -f4)${NC}"

    echo ""
    return 0
}

# ========================================
# .env 文件检查函数
# ========================================
check_env_file() {
    if [ ! -f "$PROJECT_ROOT/.env" ]; then
        echo -e "${YELLOW}⚠️  .env 文件不存在，从 .env.example 创建...${NC}"
        cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
        echo -e "${GREEN}✅ .env 文件已创建${NC}"
        echo -e "${YELLOW}⚠️  请编辑 .env 文件，配置 ZHIPU_API_KEY 等关键参数！${NC}"
    else
        echo -e "${GREEN}✅ .env 文件已存在${NC}"
    fi
    echo ""
}

# ========================================
# 启动函数
# ========================================

start_docker() {
    echo -e "${YELLOW}[启动 Docker 服务]${NC}"

    # 检查是否已运行
    if docker-compose ps | grep -q "Up"; then
        echo -e "${YELLOW}⚠️  Docker 服务已在运行${NC}"
        return 0
    fi

    echo -e "${BLUE}正在启动 Docker Compose 服务...${NC}"
    docker-compose up -d

    # 检查服务状态
    if docker-compose ps | grep -q "Up"; then
        echo -e "${BLUE}检查服务健康状态...${NC}"

        # 快速检查（最多等待 10 秒）
        for i in {1..2}; do
            if docker-compose ps | grep -q "healthy"; then
                echo -e "${GREEN}✅ 基础设施启动成功${NC}"
                return 0
            fi
            [ $i -eq 1 ] && echo -e "${YELLOW}等待服务就绪...${NC}"
            sleep 5
        done

        # 如果仍未健康，显示提示但继续
        if ! docker-compose ps | grep -q "healthy"; then
            echo -e "${YELLOW}⚠️  服务启动中，请稍后检查...${NC}"
        fi
    else
        echo -e "${RED}❌ 基础设施启动失败${NC}"
        docker-compose ps
        return 1
    fi
}

start_backend() {
    echo -e "${YELLOW}[启动后端服务]${NC}"

    # 检查后端是否已运行
    if pgrep -f "rag-backend-.*\.jar|spring-boot:run|AetherisRagApplication" > /dev/null; then
        echo -e "${YELLOW}⚠️  后端已在运行${NC}"
        return 0
    fi

    cd "$PROJECT_ROOT/backend"

    # 检查后端是否已编译
    if [ ! -d "target" ] || [ ! -d "target/classes" ]; then
        echo -e "${BLUE}后端未编译，开始编译...${NC}"
        mvn clean compile
    fi

    # 加载 .env 文件中的环境变量
    echo -e "${BLUE}加载环境变量...${NC}"
    if [ -f "$PROJECT_ROOT/.env" ]; then
        while IFS='=' read -r key value; do
            [[ "$key" =~ ^#.*$ ]] && continue
            [[ -z "$key" ]] && continue
            value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed 's/^"//;s/"$//')
            export "$key=$value"
        done < "$PROJECT_ROOT/.env"
        echo -e "${GREEN}✅ 环境变量已加载${NC}"
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
        tmp=$(mktemp)
        jq '.backend.pid = '$BACKEND_PID' | .backend.status = "running" | .backend.started_at = "'$STARTED_AT'"' "$PROJECT_ROOT/.pids.json" > "$tmp"
        mv "$tmp" "$PROJECT_ROOT/.pids.json"
    else
        sed -i '' 's/"pid": null/"pid": '$BACKEND_PID'/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"pid": [0-9]*/"pid": '$BACKEND_PID'/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"status": "stopped"/"status": "running"/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"started_at": null/"started_at": "'$STARTED_AT'"/' "$PROJECT_ROOT/.pids.json"
    fi

    echo -e "${GREEN}✅ 后端启动中...${NC}"
    echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/backend.log${NC}"

    # 等待后端启动
    echo -e "${BLUE}等待后端启动 (10秒)...${NC}"
    sleep 10

    # 检测并显示进程信息
    MVN_PID=$(pgrep -f "java.*spring-boot:run" || true)
    APP_PID=$(pgrep -f "java.*AetherisRagApplication" || true)

    if [ -n "$MVN_PID" ] || [ -n "$APP_PID" ]; then
        echo -e "${BLUE}后端进程信息:${NC}"
        [ -n "$MVN_PID" ] && echo -e "  ${CYAN}- Maven 进程: $MVN_PID${NC}"
        [ -n "$APP_PID" ] && echo -e "  ${CYAN}- 应用进程: $APP_PID${NC}"
    fi

    # 检查后端是否启动成功
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端启动成功${NC}"
    else
        echo -e "${YELLOW}⚠️  后端可能还在启动中，请检查日志${NC}"
    fi

    return 0
}

start_frontend() {
    echo -e "${YELLOW}[启动前端服务]${NC}"

    # 检查前端是否已运行
    if pgrep -f "vite.*frontend|npm.*dev|node.*vite" > /dev/null; then
        echo -e "${YELLOW}⚠️  前端已在运行${NC}"
        return 0
    fi

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
        tmp=$(mktemp)
        jq '.frontend.pid = '$FRONTEND_PID' | .frontend.status = "running" | .frontend.started_at = "'$STARTED_AT'"' "$PROJECT_ROOT/.pids.json" > "$tmp"
        mv "$tmp" "$PROJECT_ROOT/.pids.json"
    else
        sed -i '' 's/"pid": null/"pid": '$FRONTEND_PID'/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"pid": [0-9]*/"pid": '$FRONTEND_PID'/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"status": "stopped"/"status": "running"/' "$PROJECT_ROOT/.pids.json"
        sed -i '' 's/"started_at": null/"started_at": "'$STARTED_AT'"/' "$PROJECT_ROOT/.pids.json"
    fi

    echo -e "${GREEN}✅ 前端启动中...${NC}"
    echo -e "${YELLOW}📄 查看日志: tail -f $PROJECT_ROOT/logs/frontend.log${NC}"

    # 等待前端启动
    echo -e "${BLUE}等待前端启动 (5秒)...${NC}"
    sleep 5

    # 检测并显示进程信息
    NPM_PID=$(pgrep -f "npm.*dev" || true)
    NODE_PID=$(pgrep -f "node.*vite" || true)

    if [ -n "$NPM_PID" ] || [ -n "$NODE_PID" ]; then
        echo -e "${BLUE}前端进程信息:${NC}"
        [ -n "$NPM_PID" ] && echo -e "  ${CYAN}- npm 进程: $NPM_PID${NC}"
        [ -n "$NODE_PID" ] && echo -e "  ${CYAN}- node 进程 (Vite): $NODE_PID${NC}"
    fi

    return 0
}

show_help() {
    cat << EOF
${BLUE}用法:${NC}
  ./start.sh [选项]

${BLUE}选项:${NC}
  --frontend-only      仅启动前端服务
  --backend-only       仅启动后端服务
  --docker-only        仅启动 Docker 服务（MySQL + Redis）
  --all                启动所有服务（前端+后端+Docker）
  --help, -h           显示此帮助信息

${BLUE}交互模式:${NC}
  无参数运行时进入交互模式，可选择要启动的服务

${BLUE}示例:${NC}
  ./start.sh                    # 进入交互菜单
  ./start.sh --frontend-only    # 仅启动前端
  ./start.sh --backend-only     # 仅启动后端
  ./start.sh --all              # 启动所有服务

EOF
}

# ========================================
# 交互式菜单
# ========================================

show_menu() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Aetheris RAG 系统启动脚本${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${GREEN}请选择要启动的服务:${NC}"
    echo ""
    echo -e "  ${YELLOW}1${NC}. 启动前端"
    echo -e "  ${YELLOW}2${NC}. 启动后端"
    echo -e "  ${YELLOW}3${NC}. 启动所有服务（前端 + 后端）"
    echo -e "  ${YELLOW}4${NC}. 启动 Docker 服务（MySQL + Redis）"
    echo -e "  ${YELLOW}5${NC}. 启动所有（包括 Docker）"
    echo ""
    echo -ne "${BLUE}请输入选项 [1-5]: ${NC}"
}

handle_interactive_mode() {
    show_menu
    read choice

    case $choice in
        1)
            check_environment
            start_frontend
            echo ""
            echo -e "${GREEN}✅ 前端启动完成${NC}"
            echo -e "${BLUE}🌐 访问地址: http://localhost:5173${NC}"
            ;;
        2)
            check_environment
            start_backend
            echo ""
            echo -e "${GREEN}✅ 后端启动完成${NC}"
            echo -e "${BLUE}🔧 API 地址: http://localhost:8080${NC}"
            ;;
        3)
            check_environment
            start_backend
            start_frontend
            echo ""
            echo -e "${GREEN}✅ 所有服务启动完成${NC}"
            echo -e "${BLUE}🌐 前端: http://localhost:5173${NC}"
            echo -e "${BLUE}🔧 后端: http://localhost:8080${NC}"
            ;;
        4)
            check_environment
            check_env_file
            start_docker
            echo ""
            echo -e "${GREEN}✅ Docker 服务启动完成${NC}"
            ;;
        5)
            check_environment
            check_env_file
            start_docker
            start_backend
            start_frontend
            echo ""
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
            ;;
        *)
            echo -e "${RED}❌ 无效选项，请输入 1-5${NC}"
            exit 1
            ;;
    esac
}

# ========================================
# 参数解析
# ========================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --frontend-only)
                check_environment
                start_frontend
                exit $?
                ;;
            --backend-only)
                check_environment
                start_backend
                exit $?
                ;;
            --docker-only)
                check_environment
                check_env_file
                start_docker
                exit $?
                ;;
            --all)
                check_environment
                check_env_file
                start_docker
                start_backend
                start_frontend
                echo ""
                echo -e "${GREEN}========================================${NC}"
                echo -e "${GREEN}  ✅ 所有服务启动完成！${NC}"
                echo -e "${GREEN}========================================${NC}"
                echo ""
                echo -e "${BLUE}🌐 前端访问地址: http://localhost:5173${NC}"
                echo -e "${BLUE}🔧 后端 API 地址: http://localhost:8080${NC}"
                exit 0
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
    if [ $# -eq 0 ]; then
        handle_interactive_mode
    else
        parse_arguments "$@"
    fi
}

# 执行主流程
main "$@"
