#!/bin/bash

# ============================================
# 标准物质管理系统 - E2E测试运行脚本
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_DIR="$SCRIPT_DIR/e2e-tests"
FRONTEND_PORT=3002
BACKEND_PORT=8080

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - E2E测试"
echo "======================================"
echo -e "${NC}"

# 检查服务是否运行
if ! check_port $BACKEND_PORT; then
    echo -e "${RED}✗ 后端服务未运行，请先启动: ./scripts/start-all.sh start${NC}"
    exit 1
fi

if ! check_port $FRONTEND_PORT; then
    echo -e "${RED}✗ 前端服务未运行，请先启动: ./scripts/start-all.sh start${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 服务检查通过${NC}"
echo -e "  前端: http://localhost:$FRONTEND_PORT"
echo -e "  后端: http://localhost:$BACKEND_PORT"
echo ""

# 检查测试目录
if [ ! -d "$TEST_DIR/node_modules" ]; then
    echo -e "${YELLOW}正在安装测试依赖...${NC}"
    cd "$TEST_DIR"
    npm install
fi

# 运行测试
echo -e "${BLUE}开始运行E2E测试...${NC}"
echo ""

cd "$TEST_DIR"

# 可以指定运行单个测试
if [ -n "$1" ]; then
    echo -e "${YELLOW}运行单个测试: $1${NC}"
    node "$1"
else
    node run-all.js
fi
