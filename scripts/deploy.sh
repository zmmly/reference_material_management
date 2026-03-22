#!/bin/bash

# ============================================
# 标准物质管理系统 - 云服务器一键部署脚本
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
PROJECT_NAME="reference-material-management"
GIT_REPO="https://github.com/zmmly/reference_material_management.git"
DEPLOY_DIR="/opt/${PROJECT_NAME}"
BACKEND_DIR="${DEPLOY_DIR}/backend"
FRONTEND_DIR="${DEPLOY_DIR}/frontend"
DB_NAME="reference_material_management"
DB_USER="root"
DB_PASS="xjYY3687!"
JAVA_VERSION="17"
NGINX_PORT="80"
FRONTEND_PORT="3002"
BACKEND_PORT="8080"

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - 云服务器部署"
echo "======================================"
echo -e "${NC}"

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用root用户运行此脚本${NC}"
        echo -e "${YELLOW}sudo ./deploy.sh${NC}"
        exit 1
    fi
}

# 检查操作系统
check_os() {
    echo -e "${BLUE}[1/10] 检查操作系统...${NC}"
    if [ -f /etc/redhat-release ]; then
        OS="centos"
        PKG_MANAGER="yum"
    elif [ -f /etc/debian_version ]; then
        OS="debian"
        PKG_MANAGER="apt"
    elif [ -f /etc/lsb-release ]; then
        OS="ubuntu"
        PKG_MANAGER="apt"
    else
        echo -e "${RED}不支持的操作系统${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 操作系统: ${OS}${NC}"
}

# 检查和安装依赖
install_dependencies() {
    echo -e "${BLUE}[2/10] 检查和安装依赖...${NC}"

    # 检查git
    if ! command -v git &> /dev/null; then
        echo -e "${YELLOW}安装git...${NC}"
        ${PKG_MANAGER} install -y git
    fi

    # 检查nginx
    if ! command -v nginx &> /dev/null; then
        echo -e "${YELLOW}安装nginx...${NC}"
        ${PKG_MANAGER} install -y nginx
    fi

    # 检查JDK 17
    if ! command -v java &> /dev/null || [ "$(java -version 2>&1 | grep -oP 'version "?[0-9]+\.[0-9]+\.[0-9]+' | head -1)" != "1.${JAVA_VERSION}.0" ]; then
        echo -e "${YELLOW}安装JDK ${JAVA_VERSION}...${NC}"
        if [ "$OS" = "centos" ]; then
            ${PKG_MANAGER} install -y java-${JAVA_VERSION}-openjdk-devel
        else
            ${PKG_MANAGER} install -y openjdk-${JAVA_VERSION}-jdk
        fi

        # 设置JAVA_HOME
        export JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
        export PATH="$JAVA_HOME/bin:$PATH"

        # 永久设置环境变量
        echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile
        source /etc/profile
    fi

    echo -e "${GREEN}✓ 依赖安装完成${NC}"
}

# 配置MySQL数据库
setup_database() {
    echo -e "${BLUE}[3/10] 配置MySQL数据库...${NC}"

    # 创建数据库
    mysql -u${DB_USER} -p${DB_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

    # 导入数据库结构
    if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
        echo -e "${YELLOW}导入数据库结构...${NC}"
        mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql"
    fi

    # 导入数据库数据
    if [ -f "${DEPLOY_DIR}/database/data.sql" ]; then
        echo -e "${YELLOW}导入数据库数据...${NC}"
        mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/data.sql"
    fi

    echo -e "${GREEN}✓ 数据库配置完成${NC}"
}

# 克隆或更新代码
setup_code() {
    echo -e "${BLUE}[4/10] 部署代码...${NC}"

    if [ -d "${DEPLOY_DIR}" ]; then
        echo -e "${YELLOW}项目目录已存在，正在备份...${NC}"
        mv "${DEPLOY_DIR}" "${DEPLOY_DIR}_backup_$(date +%Y%m%d_%H%M%S)" || true
    fi

    # 克隆代码
    echo -e "${YELLOW}克隆代码仓库...${NC}"
    git clone ${GIT_REPO} ${DEPLOY_DIR}

    echo -e "${GREEN}✓ 代码部署完成${NC}"
}

# 构建前端
build_frontend() {
    echo -e "${BLUE}[5/10] 构建前端...${NC}"

    cd ${FRONTEND_DIR}

    # 安装依赖
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}安装前端依赖...${NC}"
        npm install
    fi

    # 构建
    echo -e "${YELLOW}构建前端...${NC}"
    npm run build

    echo -e "${GREEN}✓ 前端构建完成${NC}"
}

# 构建后端
build_backend() {
    echo -e "${BLUE}[6/10] 构建后端...${NC}"

    cd ${BACKEND_DIR}

    # 使用Maven打包
    echo -e "${YELLOW}构建后端...${NC}"
    mvn clean package -DskipTests

    echo -e "${GREEN}✓ 后端构建完成${NC}"
}

# 配置nginx
configure_nginx() {
    echo -e "${BLUE}[7/10] 配置nginx...${NC}"

    # 创建nginx配置文件
    cat > /etc/nginx/conf.d/${PROJECT_NAME}.conf <<EOF
server {
    listen ${NGINX_PORT};
    server_name _;

    # 前端静态文件
    location / {
        root ${FRONTEND_DIR}/dist;
        try_files \$uri \$uri/ /index.html;
        index index.html;

        # 缓存静态资源
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # 后端API代理
    location /api {
        proxy_pass http://localhost:${BACKEND_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF

    # 测试nginx配置
    nginx -t

    # 重启nginx
    systemctl restart nginx

    # 设置nginx开机自启
    systemctl enable nginx

    echo -e "${GREEN}✓ nginx配置完成${NC}"
}

# 创建systemd服务文件
create_systemd_services() {
    echo -e "${BLUE}[8/10] 创建systemd服务...${NC}"

    # 创建后端服务文件
    cat > /etc/systemd/system/${PROJECT_NAME}-backend.service <<EOF
[Unit]
Description=Reference Material Management Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${BACKEND_DIR}
Environment="JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
ExecStart=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk/bin/java -jar ${BACKEND_DIR}/target/reference-material-management-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    # 创建前端服务文件
    cat > /etc/systemd/system/${PROJECT_NAME}-frontend.service <<EOF
[Unit]
Description=Reference Material Management Frontend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${FRONTEND_DIR}
ExecStart=/usr/bin/node ${FRONTEND_DIR}/node_modules/.bin/vite --port ${FRONTEND_PORT} --host
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    # 重载systemd
    systemctl daemon-reload

    echo -e "${GREEN}✓ systemd服务创建完成${NC}"
}

# 启动服务
start_services() {
    echo -e "${BLUE}[9/10] 启动服务...${NC}"

    # 启动后端服务
    systemctl start ${PROJECT_NAME}-backend
    systemctl enable ${PROJECT_NAME}-backend

    # 启动前端服务（开发模式，生产环境建议使用nginx）
    systemctl start ${PROJECT_NAME}-frontend
    systemctl enable ${PROJECT_NAME}-frontend

    # 开放防火墙端口
    if command -v firewall-cmd &> /dev/null; then
        firewall-cmd --permanent --add-port=${BACKEND_PORT}/tcp
        firewall-cmd --permanent --add-port=${FRONTEND_PORT}/tcp
        firewall-cmd --permanent --add-port=${NGINX_PORT}/tcp
        firewall-cmd --reload
    elif command -v ufw &> /dev/null; then
        ufw allow ${BACKEND_PORT}/tcp
        ufw allow ${FRONTEND_PORT}/tcp
        ufw allow ${NGINX_PORT}/tcp
    fi

    echo -e "${GREEN}✓ 服务启动完成${NC}"
}

# 显示部署信息
show_deployment_info() {
    echo -e "${BLUE}[10/10] 部署完成${NC}"
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     部署成功！                             ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
    echo ""
    echo -e "📱 访问地址:"
    echo -e "  前端: ${YELLOW}http://$(curl -s ifconfig.me):${NGINX_PORT}/${NC}"
    echo -e "  后端: ${YELLOW}http://$(curl -s ifconfig.me):${BACKEND_PORT}/${NC}"
    echo -e "  API文档: ${YELLOW}http://$(curl -s ifconfig.me):${BACKEND_PORT}/doc.html${NC}"
    echo ""
    echo -e "🔑 默认账号: ${YELLOW}admin / admin123${NC}"
    echo ""
    echo -e "📂 部署目录: ${YELLOW}${DEPLOY_DIR}${NC}"
    echo ""
    echo -e "🛠️  管理命令:"
    echo -e "  查看后端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-backend -f${NC}"
    echo -e "  查看前端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-frontend -f${NC}"
    echo -e "  重启后端: ${YELLOW}systemctl restart ${PROJECT_NAME}-backend${NC}"
    echo -e "  重启前端: ${YELLOW}systemctl restart ${PROJECT_NAME}-frontend${NC}"
    echo -e "  重启nginx: ${YELLOW}systemctl restart nginx${NC}"
    echo ""
}

# 主函数
main() {
    check_root
    check_os
    install_dependencies
    setup_code
    setup_database
    build_frontend
    build_backend
    configure_nginx
    create_systemd_services
    start_services
    show_deployment_info
}

# 执行主函数
main
