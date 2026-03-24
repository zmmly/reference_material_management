#!/bin/bash

# ============================================
# 标准物质管理系统 - 生产环境部署脚本
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_PORT=80
BACKEND_PORT=8080
LOG_DIR="/var/log/rmm"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 创建日志目录
mkdir -p $LOG_DIR

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - 生产部署"
echo "======================================"
echo -e "${NC}"

# 检查端口是否被占用
check_port() {
    local port=$1
    if netstat -tlnp 2>/dev/null | grep -q ":$port " || lsof -i :$port > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 杀死占用端口的进程
kill_port() {
    local port=$1
    if check_port $port; then
        echo -e "${YELLOW}正在停止端口 $port 的服务...${NC}"
        if command -v fuser &> /dev/null; then
            fuser -k $port/tcp 2>/dev/null || true
        elif command -v lsof &> /dev/null; then
            lsof -ti :$port | xargs kill -9 2>/dev/null || true
        fi
        sleep 2
    fi
}

# 停止所有服务
stop_services() {
    echo -e "${BLUE}[1/5] 停止旧服务...${NC}"
    kill_port $FRONTEND_PORT
    kill_port $BACKEND_PORT
    echo -e "${GREEN}✓ 旧服务已停止${NC}"
}

# 更新代码
update_code() {
    echo -e "${BLUE}[2/5] 更新代码...${NC}"
    cd "$PROJECT_ROOT"

    # 保存本地修改的配置文件（如果有）
    if [ -f "backend/src/main/resources/application-prod.yml" ]; then
        cp backend/src/main/resources/application-prod.yml /tmp/application-prod.yml.bak
    fi

    # 拉取最新代码
    git fetch origin
    git reset --hard origin/main
    git pull origin main

    # 恢复本地配置（如果需要保留本地修改）
    # cp /tmp/application-prod.yml.bak backend/src/main/resources/application-prod.yml

    echo -e "${GREEN}✓ 代码已更新${NC}"
}

# 启动后端
start_backend() {
    echo -e "${BLUE}[3/5] 启动后端服务...${NC}"

    cd "$PROJECT_ROOT/backend"

    # 检查 Java 环境
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ 未找到 Java 环境${NC}"
        exit 1
    fi

    JAR_FILE="target/reference-material-management-1.0.0.jar"

    # 编译打包（如果 jar 不存在或代码有更新）
    if [ ! -f "$JAR_FILE" ] || \
       [ "$(find src -newer $JAR_FILE 2>/dev/null | wc -l)" -gt 0 ]; then
        echo -e "${YELLOW}正在编译后端项目...${NC}"
        mvn clean package -DskipTests -q
    fi

    # 使用生产环境配置启动后端
    echo -e "${YELLOW}正在启动后端服务 (生产环境配置)...${NC}"
    nohup java -Xms512m -Xmx1024m -XX:+UseG1GC \
        -jar $JAR_FILE \
        --spring.profiles.active=prod \
        > $LOG_DIR/backend.log 2>&1 &

    # 等待后端启动
    local count=0
    while ! check_port $BACKEND_PORT && [ $count -lt 60 ]; do
        sleep 1
        count=$((count + 1))
        echo -ne "${YELLOW}等待后端启动... $count/60${NC}\r"
    done

    if check_port $BACKEND_PORT; then
        echo -e "${GREEN}✓ 后端服务启动成功 (http://localhost:$BACKEND_PORT)${NC}"
    else
        echo -e "${RED}✗ 后端服务启动失败，请查看日志: $LOG_DIR/backend.log${NC}"
        tail -50 $LOG_DIR/backend.log
        exit 1
    fi
}

# 启动前端
start_frontend() {
    echo -e "${BLUE}[4/5] 启动前端服务...${NC}"

    cd "$PROJECT_ROOT/frontend"

    # 检查 node_modules
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}正在安装前端依赖...${NC}"
        npm install
    fi

    # 启动前端
    echo -e "${YELLOW}正在启动前端服务...${NC}"
    nohup npm run dev > $LOG_DIR/frontend.log 2>&1 &

    # 等待前端启动
    local count=0
    while ! check_port $FRONTEND_PORT && [ $count -lt 60 ]; do
        sleep 1
        count=$((count + 1))
        echo -ne "${YELLOW}等待前端启动... $count/60${NC}\r"
    done

    if check_port $FRONTEND_PORT; then
        echo -e "${GREEN}✓ 前端服务启动成功 (http://localhost:$FRONTEND_PORT)${NC}"
    else
        echo -e "${RED}✗ 前端服务启动失败，请查看日志: $LOG_DIR/frontend.log${NC}"
        tail -50 $LOG_DIR/frontend.log
        exit 1
    fi
}

# 显示部署结果
show_result() {
    echo -e "${BLUE}[5/5] 部署完成${NC}"
    echo ""
    echo -e "┌─────────────────────────────────────────────┐"
    echo -e "│  ${GREEN}部署成功！${NC}                                │"
    echo -e "├─────────────────────────────────────────────┤"
    echo -e "│  访问地址:                                   │"
    echo -e "│    前端: http://localhost (端口 80)         │"
    echo -e "│    后端: http://localhost:8080              │"
    echo -e "│    API文档: http://localhost:8080/doc.html  │"
    echo -e "├─────────────────────────────────────────────┤"
    echo -e "│  默认账号: admin / admin123                  │"
    echo -e "├─────────────────────────────────────────────┤"
    echo -e "│  日志文件:                                   │"
    echo -e "│    前端: $LOG_DIR/frontend.log"
    echo -e "│    后端: $LOG_DIR/backend.log"
    echo -e "└─────────────────────────────────────────────┘"
    echo ""
}

# 查看日志
show_logs() {
    echo -e "${BLUE}后端日志 (最后 50 行):${NC}"
    tail -50 $LOG_DIR/backend.log 2>/dev/null || echo "日志文件不存在"
    echo ""
    echo -e "${BLUE}前端日志 (最后 50 行):${NC}"
    tail -50 $LOG_DIR/frontend.log 2>/dev/null || echo "日志文件不存在"
}

# 查看状态
show_status() {
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
}

# 主逻辑
case "${1:-deploy}" in
    deploy)
        stop_services
        update_code
        start_backend
        start_frontend
        show_result
        ;;
    start)
        start_backend
        start_frontend
        show_result
        ;;
    stop)
        stop_services
        ;;
    restart)
        stop_services
        sleep 2
        start_backend
        start_frontend
        show_result
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    *)
        echo "用法: $0 {deploy|start|stop|restart|status|logs}"
        echo ""
        echo "命令说明:"
        echo "  deploy  - 更新代码并部署（默认）"
        echo "  start   - 启动服务（不更新代码）"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务（不更新代码）"
        echo "  status  - 查看服务状态"
        echo "  logs    - 查看服务日志"
        exit 1
        ;;
esac
