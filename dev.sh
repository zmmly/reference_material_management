#!/bin/bash

# ============================================
# 开发模式启动脚本（前台运行，Ctrl+C停止）
# ============================================

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "======================================"
echo "  标准物质管理系统 - 开发模式"
echo "======================================"
echo ""
echo "前端: http://localhost:3002"
echo "后端: http://localhost:8080"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo "======================================"
echo ""

# 清理函数
cleanup() {
    echo ""
    echo "正在停止服务..."
    kill $(jobs -p) 2>/dev/null
    exit 0
}

trap cleanup INT TERM

# 启动后端（后台）
cd "$PROJECT_ROOT/backend"
echo "[后端] 启动中..."
mvn spring-boot:run -q &
BACKEND_PID=$!

# 等待后端启动
sleep 5

# 启动前端（后台）
cd "$PROJECT_ROOT/frontend"
echo "[前端] 启动中..."
npm run dev -- --port 3002 &
FRONTEND_PID=$!

# 等待
wait
