#!/bin/bash

# ============================================
# 标准物质管理系统 - 快速部署脚本（简化版）
# ============================================

# 快速部署脚本用于快速在已有环境的服务器上部署应用
# 适合用于测试环境或已经配置好基础环境的生产环境

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 基础配置
PROJECT_NAME="reference-material_management"
GIT_REPO="https://github.com/zmmly/reference_material_management.git"
DEPLOY_DIR="/opt/${PROJECT_NAME}"
DB_NAME="reference_material_management"
DB_USER="root"
DB_PASS="xjYY3687!"

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - 快速部署"
echo "======================================"
echo -e "${NC}"

# 环境检查
echo -e "${BLUE}[检查环境]${NC}"

# 检查Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java未安装，请先安装JDK 17${NC}"
    echo -e "${YELLOW}CentOS/RHEL: yum install -y java-17-openjdk-devel${NC}"
    echo -e "${YELLOW}Ubuntu/Debian: apt install -y openjdk-17-jdk${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java版本: $(java -version 2>&1 | head -1)${NC}"

# 检查MySQL
if ! command -v mysql &> /dev/null; then
    echo -e "${RED}✗ MySQL未安装${NC}"
    exit 1
fi

# 测试MySQL连接
if ! mysql -u${DB_USER} -p${DB_PASS} -e "SELECT 1;" &> /dev/null; then
    echo -e "${RED}✗ MySQL连接失败，请检查用户名和密码${NC}"
    exit 1
fi
echo -e "${GREEN}✓ MySQL连接正常${NC}"

# 检查Git
if ! command -v git &> /dev/null; then
    echo -e "${RED}✗ Git未安装${NC}"
    echo -e "${YELLOW}正在安装Git...${NC}"
    if [ -f /etc/redhat-release ]; then
        yum install -y git
    else
        apt update && apt install -y git
    fi
fi
echo -e "${GREEN}✓ Git已安装${NC}"

# 检查Nginx
if ! command -v nginx &> /dev/null; then
    echo -e "${YELLOW}⚠ Nginx未安装，将使用前端开发模式${NC}"
    NGINX_INSTALLED=false
else
    NGINX_INSTALLED=true
    echo -e "${GREEN}✓ Nginx已安装${NC}"
fi

# 开始部署
echo ""
echo -e "${BLUE}[开始部署]${NC}"

# 创建数据库
echo -e "${YELLOW}创建数据库...${NC}"
mysql -u${DB_USER} -p${DB_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo -e "${GREEN}✓ 数据库创建完成${NC}"

# 克隆代码
if [ -d "${DEPLOY_DIR}" ]; then
    echo -e "${YELLOW}项目目录已存在，正在更新...${NC}"
    cd ${DEPLOY_DIR}
    git pull origin main
else
    echo -e "${YELLOW}克隆代码仓库...${NC}"
    git clone ${GIT_REPO} ${DEPLOY_DIR}
fi
echo -e "${GREEN}✓ 代码部署完成${NC}"

# 初始化数据库
echo -e "${YELLOW}初始化数据库...${NC}"
if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
    mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql"
fi
if [ -f "${DEPLOY_DIR}/database/data.sql" ]; then
    mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/data.sql"
fi
echo -e "${GREEN}✓ 数据库初始化完成${NC}"

# 构建前端
echo -e "${YELLOW}构建前端...${NC}"
cd ${FRONTEND_DIR}
npm install
npm run build
echo -e "${GREEN}✓ 前端构建完成${NC}"

# 构建后端
echo -e "${YELLOW}构建后端...${NC}"
cd ${BACKEND_DIR}
mvn clean package -DskipTests
echo -e "${GREEN}✓ 后端构建完成${NC}"

# 配置服务
if [ "$NGINX_INSTALLED" = true ]; then
    echo -e "${YELLOW}配置Nginx...${NC}"

    # 创建nginx配置
    cat > /etc/nginx/conf.d/${PROJECT_NAME}.conf <<EOF
server {
    listen 80;
    server_name _;

    location / {
        root ${FRONTEND_DIR}/dist;
        try_files \$uri \$uri/ /index.html;
        index index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

    nginx -t && systemctl restart nginx
    echo -e "${GREEN}✓ Nginx配置完成${NC}"
fi

# 启动后端（后台运行）
echo -e "${YELLOW}启动后端服务...${NO}"
nohup java -jar ${BACKEND_DIR}/target/reference-material-management-1.0.0.jar > /opt/${PROJECT_NAME}/backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > /opt/${PROJECT_NAME}/backend.pid

# 等待后端启动
sleep 10

# 检查后端是否启动
if ps -p $BACKEND_PID > /dev/null; then
    echo -e "${GREEN}✓ 后端服务启动成功 (PID: $BACKEND_PID)${NC}"
else
    echo -e "${RED}✗ 后端服务启动失败${NC}"
    tail -50 /opt/${PROJECT_NAME}/backend.log
    exit 1
fi

# 显示部署结果
echo ""
echo -e "${GREEN}╔════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     部署成功！                         ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════╝${NC}"
echo ""
echo -e "📱 访问地址:"
if [ "$NGINX_INSTALLED" = true ]; then
    echo -e "  前端: ${YELLOW}http://$(curl -s ifconfig.me)${NC}"
else
    echo -e "  前端: ${YELLOW}http://$(curl -s ifconfig.me):3002${NC} (开发模式)"
fi
echo -e "  后端: ${YELLOW}http://$(curl -s ifconfig.me):8080${NC}"
echo -e "  API文档: ${YELLOW}http://$(curl -s ifconfig.me):8080/doc.html${NC}"
echo ""
echo -e "🔑 默认账号: ${YELLOW}admin / admin123${NC}"
echo ""
echo -e "📁 部署目录: ${YELLOW}${DEPLOY_DIR}${NC}"
echo -e "📋 后端日志: ${YELLOW}/opt/${PROJECT_NAME}/backend.log${NC}"
echo ""
echo -e "🛠️  管理命令:"
echo -e "  查看后端日志: ${YELLOW}tail -f /opt/${PROJECT_NAME}/backend.log${NC}"
echo -e "  停止后端: ${YELLOW}kill \$(cat /opt/${PROJECT_NAME}/backend.pid)${NC}"
echo -e "  重启Nginx: ${YELLOW}systemctl restart nginx${NC}"
echo ""
