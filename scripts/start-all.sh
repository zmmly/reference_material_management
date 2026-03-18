#!/bin/bash

# ============================================
# 标准物质管理系统 - 一键启动脚本
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_PORT=3002
BACKEND_PORT=8080

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - 启动脚本"
echo "======================================"
echo -e "${NC}"

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        return 0  # 端口被占用
    else
        return 1  # 端口可用
    fi
}

# 杀死占用端口的进程
kill_port() {
    local port=$1
    if check_port $port; then
        echo -e "${YELLOW}端口 $port 已被占用，正在终止...${NC}"
        lsof -ti :$port | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
}

# 启动后端
start_backend() {
    echo -e "${BLUE}[1/3] 启动后端服务...${NC}"

    if check_port $BACKEND_PORT; then
        echo -e "${GREEN}后端服务已在运行 (端口 $BACKEND_PORT)${NC}"
        return 0
    fi

    cd "$PROJECT_ROOT/backend"

    # 检查是否需要编译
    if [ ! -f "target/classes/com/rmm/Application.class" ]; then
        echo -e "${YELLOW}正在编译后端...${NC}"
        mvn compile -q
    fi

    # 启动后端
    echo -e "${YELLOW}正在启动后端服务...${NC}"
    nohup mvn spring-boot:run -q > /tmp/rmm-backend.log 2>&1 &

    # 等待后端启动
    local count=0
    while ! check_port $BACKEND_PORT && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
        echo -ne "${YELLOW}等待后端启动... $count/30${NC}\r"
    done

    if check_port $BACKEND_PORT; then
        echo -e "${GREEN}✓ 后端服务启动成功 (http://localhost:$BACKEND_PORT)${NC}"
    else
        echo -e "${RED}✗ 后端服务启动失败，请查看日志: /tmp/rmm-backend.log${NC}"
        return 1
    fi
}

# 启动前端
start_frontend() {
    echo -e "${BLUE}[2/3] 启动前端服务...${NC}"

    if check_port $FRONTEND_PORT; then
        echo -e "${GREEN}前端服务已在运行 (端口 $FRONTEND_PORT)${NC}"
        return 0
    fi

    cd "$PROJECT_ROOT/frontend"

    # 检查 node_modules
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}正在安装依赖...${NC}"
        npm install
    fi

    # 启动前端
    echo -e "${YELLOW}正在启动前端服务...${NC}"
    nohup npm run dev -- --port $FRONTEND_PORT > /tmp/rmm-frontend.log 2>&1 &

    # 等待前端启动
    local count=0
    while ! check_port $FRONTEND_PORT && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
        echo -ne "${YELLOW}等待前端启动... $count/30${NC}\r"
    done

    if check_port $FRONTEND_PORT; then
        echo -e "${GREEN}✓ 前端服务启动成功 (http://localhost:$FRONTEND_PORT)${NC}"
    else
        echo -e "${RED}✗ 前端服务启动失败，请查看日志: /tmp/rmm-frontend.log${NC}"
        return 1
    fi
}

# 显示状态
show_status() {
    echo -e "${BLUE}[3/3] 服务状态${NC}"
    echo ""
    echo -e "┌─────────────────────────────────────────────┐"
    echo -e "│  ${GREEN}服务已全部启动${NC}                            │"
    echo -e "├─────────────────────────────────────────────┤"
    echo -e "│  前端地址: http://localhost:$FRONTEND_PORT           │"
    echo -e "│  后端地址: http://localhost:$BACKEND_PORT           │"
    echo -e "│  默认账号: admin / admin123                  │"
    echo -e "├─────────────────────────────────────────────┤"
    echo -e "│  日志文件:                                   │"
    echo -e "│    前端: /tmp/rmm-frontend.log             │"
    echo -e "│    后端: /tmp/rmm-backend.log              │"
    echo -e "└─────────────────────────────────────────────┘"
    echo ""
}

# 停止服务
stop_all() {
    echo -e "${YELLOW}正在停止所有服务...${NC}"
    kill_port $FRONTEND_PORT
    kill_port $BACKEND_PORT
    echo -e "${GREEN}✓ 所有服务已停止${NC}"
}

# 主逻辑
case "${1:-start}" in
    start)
        start_backend
        start_frontend
        show_status
        ;;
    stop)
        stop_all
        ;;
    restart)
        stop_all
        sleep 2
        start_backend
        start_frontend
        show_status
        ;;
    status)
        echo -e "${BLUE}服务状态:${NC}"
        if check_port $BACKEND_PORT; then
            echo -e "  后端: ${GREEN}运行中${NC} (http://localhost:$BACKEND_PORT)"
        else
            echo -e "  后端: ${RED}未运行${NC}"
        fi
        if check_port $FRONTEND_PORT; then
            echo -e "  前端: ${GREEN}运行中${NC} (http://localhost:$FRONTEND_PORT)"
        else
            echo -e "  前端: ${RED}未运行${NC}"
        fi
        ;;
    logs)
        echo -e "${BLUE}后端日志 (最后 50 行):${NC}"
        tail -50 /tmp/rmm-backend.log 2>/dev/null || echo "日志文件不存在"
        echo ""
        echo -e "${BLUE}前端日志 (最后 50 行):${NC}"
        tail -50 /tmp/rmm-frontend.log 2>/dev/null || echo "日志文件不存在"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs}"
        exit 1
        ;;
esac
