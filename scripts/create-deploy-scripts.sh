#!/bin/bash

# ============================================
# 在服务器上直接创建部署脚本
# ============================================

echo "======================================"
echo "  创建标准物质管理系统部署脚本"
echo "======================================"
echo ""

# 创建主部署脚本
cat > /root/deploy.sh <<'DEPLOY_SCRIPT_EOF'
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

    if ! command -v git &> /dev/null; then
        echo -e "${YELLOW}安装git...${NC}"
        ${PKG_MANAGER} install -y git
    fi

    if ! command -v nginx &> /dev/null; then
        echo -e "${YELLOW}安装nginx...${NC}"
        ${PKG_MANAGER} install -y nginx
    fi

    if ! command -v java &> /dev/null || [ "$(java -version 2>&1 | grep -oP 'version "?[0-9]+\.[0-9]+\.[0-9]+' | head -1)" != "1.${JAVA_VERSION}.0" ]; then
        echo -e "${YELLOW}安装JDK ${JAVA_VERSION}...${NC}"
        if [ "$OS" = "centos" ]; then
            ${PKG_MANAGER} install -y java-${JAVA_VERSION}-openjdk-devel
        else
            ${PKG_MANAGER} install -y openjdk-${JAVA_VERSION}-jdk
        fi
        export JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile
        source /etc/profile
    fi

    # 检查Node.js和npm
    if ! command -v node &> /dev/null || ! command -v npm &> /dev/null; then
        echo -e "${YELLOW}安装Node.js和npm...${NC}"
        ${PKG_MANAGER} install -y nodejs npm
        if [ "$OS" = "centos" ]; then
            export PATH="/usr/bin:$PATH"
        else
            export PATH="/usr/bin:$PATH"
        fi
        echo "export PATH=$PATH" >> /etc/profile
        source /etc/profile
    fi

    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${YELLOW}安装Maven...${NC}"
        ${PKG_MANAGER} install -y maven
    fi

    echo -e "${GREEN}✓ 依赖安装完成${NC}"
}

# 配置MySQL数据库
setup_database() {
    echo -e "${BLUE}[3/10] 配置MySQL数据库...${NC}"
    mysql -u${DB_USER} -p${DB_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
        echo -e "${YELLOW}导入数据库结构...${NC}"
        mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql"
    fi
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
    echo -e "${YELLOW}克隆代码仓库...${NC}"
    git clone ${GIT_REPO} ${DEPLOY_DIR}
    echo -e "${GREEN}✓ 代码部署完成${NC}"
}

# 构建前端
build_frontend() {
    echo -e "${BLUE}[5/10] 构建前端...${NC}"
    cd ${FRONTEND_DIR}
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}安装前端依赖...${NC}"
        npm install
    fi
    echo -e "${YELLOW}构建前端...${NC}"
    npm run build
    echo -e "${GREEN}✓ 前端构建完成${NC}"
}

# 构建后端
build_backend() {
    echo -e "${BLUE}[6/10] 构建后端...${NC}"
    cd ${BACKEND_DIR}
    echo -e "${YELLOW}构建后端...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✓ 后端构建完成${NC}"
}

# 配置nginx
configure_nginx() {
    echo -e "${BLUE}[7/10] 配置nginx...${NC}"
    cat > /etc/nginx/conf.d/${PROJECT_NAME}.conf <<'NGINX_CONF'
server {
    listen ${NGINX_PORT};
    server_name _;

    location / {
        root ${FRONTEND_DIR}/dist;
        try_files $uri $uri/ /index.html;
        index index.html;
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    location /api {
        proxy_pass http://localhost:${BACKEND_PORT};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
NGINX_CONF
    nginx -t && systemctl restart nginx
    systemctl enable nginx
    echo -e "${GREEN}✓ nginx配置完成${NC}"
}

# 创建systemd服务文件
create_systemd_services() {
    echo -e "${BLUE}[8/10] 创建systemd服务...${NC}"
    cat > /etc/systemd/system/${PROJECT_NAME}-backend.service <<'BACKEND_SERVICE'
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
BACKEND_SERVICE
    cat > /etc/systemd/system/${PROJECT_NAME}-frontend.service <<'FRONTEND_SERVICE'
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
FRONTEND_SERVICE
    systemctl daemon-reload
    echo -e "${GREEN}✓ systemd服务创建完成${NC}"
}

# 启动服务
start_services() {
    echo -e "${BLUE}[9/10] 启动服务...${NC}"
    systemctl start ${PROJECT_NAME}-backend
    systemctl enable ${PROJECT_NAME}-backend
    systemctl start ${PROJECT_NAME}-frontend
    systemctl enable ${PROJECT_NAME}-frontend
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
    echo -e "${GREEN}╔════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     部署成功！                             ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════╝${NC}"
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
DEPLOY_SCRIPT_EOF

# 创建更新脚本
cat > /root/update.sh <<'UPDATE_SCRIPT_EOF'
#!/bin/bash

set -e
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_NAME="reference-material-management"
DEPLOY_DIR="/opt/${PROJECT_NAME}"
BACKUP_DIR="/opt/${PROJECT_NAME}_backups"
BACKUP_FILE="${BACKUP_DIR}/backup_$(date +%Y%m%d_%H%M%S).sql"
BACKEND_DIR="${DEPLOY_DIR}/backend"
FRONTEND_DIR="${DEPLOY_DIR}/frontend"
DB_NAME="reference_material_management"
DB_USER="root"
DB_PASS="xjYY3687!"

echo -e "${BLUE}"
echo "======================================"
echo "  标准物质管理系统 - 云服务器更新"
echo "======================================"
echo -e "${NC}"

check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用root用户运行此脚本${NC}"
        exit 1
    fi
}

backup_database() {
    echo -e "${BLUE}[1/7] 备份数据库...${NC}"
    mkdir -p ${BACKUP_DIR}
    echo -e "${YELLOW}正在备份数据库...${NC}"
    mysqldump -u${DB_USER} -p${DB_PASS} ${DB_NAME} > ${BACKUP_FILE}
    gzip ${BACKUP_FILE}
    find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete
    echo -e "${GREEN}✓ 数据库备份完成: ${BACKUP_FILE}.gz${NC}"
}

update_code() {
    echo -e "${BLUE}[2/7] 更新代码...${NC}"
    cd ${DEPLOY_DIR}
    echo -e "${YELLOW}备份当前代码...${NC}"
    git stash
    echo -e "${YELLOW}拉取最新代码...${NC}"
    git fetch origin
    git checkout main
    git pull origin main
    echo -e "${GREEN}✓ 代码更新完成${NC}"
}

update_database() {
    echo -e "${BLUE}[3/7] 更新数据库...${NC}"
    if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
        echo -e "${YELLOW}更新数据库结构...${NC}"
        mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql}" 2>/dev/null || true
        echo -e "${GREEN}✓ 数据库结构更新完成${NC}"
    fi
}

rebuild_frontend() {
    echo -e "${BLUE}[4/7] 重新构建前端...${NC}"
    cd ${FRONTEND_DIR}

    # 检查Node.js和npm
    if ! command -v node &> /dev/null || ! command -v npm &> /dev/null; then
        echo -e "${RED}✗ Node.js或npm未安装${NC}"
        echo -e "${YELLOW}正在安装Node.js和npm...${NC}"
        if [ -f /etc/redhat-release ]; then
            yum install -y nodejs npm
        else
            apt update && apt install -y nodejs npm
        fi
    fi

    echo -e "${YELLOW}检查前端依赖...${NC}"
    npm install
    echo -e "${YELLOW}构建前端...${NC}"
    npm run build
    echo -e "${GREEN}✓ 前端构建完成${NC}"
}

rebuild_backend() {
    echo -e "${BLUE}[5/7] 重新构建后端...${NC}"
    cd ${BACKEND_DIR}
    echo -e "${YELLOW}构建后端...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✓ 后端构建完成${NC}"
}

restart_services() {
    echo -e "${BLUE}[6/7] 重启服务...${NC}"
    systemctl restart ${PROJECT_NAME}-backend
    sleep 10
    systemctl restart ${PROJECT_NAME}-frontend
    systemctl reload nginx
    echo -e "${GREEN}✓ 服务重启完成${NC}"
}

health_check() {
    echo -e "${BLUE}[7/7] 健康检查...${NC}"
    if systemctl is-active --quiet ${PROJECT_NAME}-backend; then
        echo -e "${GREEN}✓ 后端服务运行正常${NC}"
    else
        echo -e "${RED}✗ 后端服务未运行${NC}"
        systemctl status ${PROJECT_NAME}-backend
    fi
    if systemctl is-active --quiet ${PROJECT_NAME}-frontend; then
        echo -e "${GREEN}✓ 前端服务运行正常${NC}"
    else
        echo -e "${RED}✗ 前端服务未运行${NC}"
        systemctl status ${PROJECT_NAME}-frontend
    fi
}

show_update_info() {
    echo -e "${BLUE}[8/7] 更新完成${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     更新成功！                          ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════╝${NC}"
    echo ""
    echo -e "💾 数据库备份: ${YELLOW}${BACKUP_FILE}.gz${NC}"
    echo -e "🔄 代码已更新到: ${YELLOW}$(cd ${DEPLOY_DIR} && git log -1 --pretty=format:'%h - %s')${NC}"
    echo ""
}

main() {
    check_root
    backup_database
    update_code
    update_database
    rebuild_frontend
    rebuild_backend
    restart_services
    health_check
    show_update_info
}

main
UPDATE_SCRIPT_EOF

# 添加执行权限
chmod +x /root/deploy.sh
chmod +x /root/update.sh

echo "======================================"
echo "  部署脚本创建完成！"
echo "======================================"
echo ""
echo "已创建的脚本："
echo "  /root/deploy.sh      - 主部署脚本"
echo "  /root/update.sh      - 更新脚本"
echo ""
echo "现在可以运行："
echo "  ./deploy.sh  （完整部署）"
echo "  ./update.sh  （更新应用）"
echo ""
