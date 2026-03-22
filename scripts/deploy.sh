#!/bin/bash

# ============================================
# 标准物质管理系统 - 云服务器一键部署脚本
# ============================================

set -e

# 错误处理函数
error_exit() {
    echo -e "${RED}错误: $1${NC}"
    exit 1
}

# 命令执行函数
run_command() {
    local description=$1
    shift
    local command=$@

    echo -e "${YELLOW}执行: ${description}${NC}"
    if ! $command; then
        error_exit "${description} 失败"
    fi
}

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/deploy-config.env"

# 默认配置
DEFAULT_GIT_REPO="https://github.com/zmmly/reference_material_management.git"
DEFAULT_GIT_BRANCH="main"
DEFAULT_DB_NAME="reference_material_management"
DEFAULT_DB_USER="root"
DEFAULT_DB_PASS="xjYY3687!"
DEFAULT_DB_HOST="localhost"
DEFAULT_DB_PORT="3306"
DEFAULT_DEPLOY_DIR="/opt/reference_material_management"
DEFAULT_BACKUP_DIR="/opt/reference_material_management_backups"
DEFAULT_PROJECT_NAME="reference-material_management"
DEFAULT_NGINX_PORT="80"
DEFAULT_FRONTEND_PORT="3002"
DEFAULT_BACKEND_PORT="8080"
DEFAULT_JAVA_VERSION="17"
DEFAULT_JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
DEFAULT_SERVICE_USER="root"
DEFAULT_BACKUP_RETENTION_DAYS="7"
DEFAULT_AUTO_BACKUP_ENABLED=true

# 全局变量
export JAVA_HOME=""
export PATH=""

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

# 加载配置文件
load_config() {
    echo -e "${BLUE}[2/10] 加载配置文件...${NC}"

    if [ ! -f "$CONFIG_FILE" ]; then
        echo -e "${YELLOW}配置文件不存在，使用默认配置${NC}"
        use_default_config
        return 0
    fi

    # 读取配置文件
    while IFS='=' read -r key value || [ -z "$key" ]; do
        [[ "$key" =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue
        case "$key" in
            GIT_REPO) GIT_REPO="$value" ;;
            GIT_BRANCH) GIT_BRANCH="$value" ;;
            DEPLOY_DIR) DEPLOY_DIR="$value" ;;
            BACKUP_DIR) BACKUP_DIR="$value" ;;
            BACKEND_DIR) BACKEND_DIR="${DEPLOY_DIR}/backend" ;;
            FRONTEND_DIR) FRONTEND_DIR="${DEPLOY_DIR}/frontend" ;;
            DB_NAME) DB_NAME="$value" ;;
            DB_USER) DB_USER="$value" ;;
            DB_PASS) DB_PASS="$value" ;;
            DB_HOST) DB_HOST="$value" ;;
            DB_PORT) DB_PORT="$value" ;;
            PROJECT_NAME) PROJECT_NAME="$value" ;;
            NGINX_PORT) NGINX_PORT="$value" ;;
            FRONTEND_PORT) FRONTEND_PORT="$value" ;;
            BACKEND_PORT) BACKEND_PORT="$value" ;;
            JAVA_VERSION) JAVA_VERSION="$value" ;;
            JAVA_OPTS) JAVA_OPTS="$value" ;;
            SERVICE_USER) SERVICE_USER="$value" ;;
            BACKUP_RETENTION_DAYS) BACKUP_RETENTION_DAYS="$value" ;;
            AUTO_BACKUP_ENABLED) AUTO_BACKUP_ENABLED="$value" ;;
        esac
    done < "$CONFIG_FILE"

    # 验证必需的配置
    validate_config
}

# 使用默认配置
use_default_config() {
    echo -e "${YELLOW}使用默认配置值...${NC}"
    GIT_REPO="$DEFAULT_GIT_REPO"
    GIT_BRANCH="$DEFAULT_GIT_BRANCH"
    DEPLOY_DIR="$DEFAULT_DEPLOY_DIR"
    BACKUP_DIR="$DEFAULT_BACKUP_DIR"
    BACKEND_DIR="${DEFAULT_DEPLOY_DIR}/backend"
    FRONTEND_DIR="${DEFAULT_DEPLOY_DIR}/frontend"
    DB_NAME="$DEFAULT_DB_NAME"
    DB_USER="$DEFAULT_DB_USER"
    DB_PASS="$DEFAULT_DB_PASS"
    DB_HOST="$DEFAULT_DB_HOST"
    DB_PORT="$DEFAULT_DB_PORT"
    PROJECT_NAME="$DEFAULT_PROJECT_NAME"
    NGINX_PORT="$DEFAULT_NGINX_PORT"
    FRONTEND_PORT="$DEFAULT_FRONTEND_PORT"
    BACKEND_PORT="$DEFAULT_BACKEND_PORT"
    JAVA_VERSION="$DEFAULT_JAVA_VERSION"
    JAVA_OPTS="$DEFAULT_JAVA_OPTS"
    SERVICE_USER="$DEFAULT_SERVICE_USER"
    BACKUP_RETENTION_DAYS="$DEFAULT_BACKUP_RETENTION_DAYS"
    AUTO_BACKUP_ENABLED="$DEFAULT_AUTO_BACKUP_ENABLED"
}

# 验证配置
validate_config() {
    echo -e "${BLUE}[3/10] 验证配置...${NC}"

    local missing_config=false

    if [ -z "$DB_PASS" ]; then
        echo -e "${YELLOW}⚠️  数据库密码未配置，将使用默认值${NC}"
        DB_PASS="$DEFAULT_DB_PASS"
    fi

    if [ -z "$DEPLOY_DIR" ]; then
        echo -e "${RED}❌ 部署目录未配置${NC}"
        missing_config=true
    fi

    if [ -z "$DB_NAME" ]; then
        echo -e "${RED}❌ 数据库名称未配置${NC}"
        missing_config=true
    fi

    if [ -z "$PROJECT_NAME" ]; then
        echo -e "${RED}❌ 项目名称未配置${NC}"
        missing_config=true
    fi

    if [ "$missing_config" = true ]; then
        echo -e "${RED}✗ 配置验证失败${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ 配置验证完成${NC}"
}

# 显示当前配置
show_current_config() {
    echo -e "${BLUE}[4/10] 当前配置${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════╗${NC}"
    echo -e "${GREEN}║              部署配置                  ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Git配置:"
    echo -e "  仓库: ${YELLOW}${GIT_REPO}${NC}"
    echo -e "  分支: ${YELLOW}${GIT_BRANCH}${NC}"
    echo ""
    echo -e "${YELLOW}数据库配置:"
    echo -e "  主机: ${YELLOW}${DB_HOST}:${DB_PORT}${NC}"
    echo -e "  数据库: ${YELLOW}${DB_NAME}${NC}"
    echo -e "  用户: ${YELLOW}${DB_USER}${NC}"
    echo -e "  密码: ${YELLOW}***${NC}"
    echo ""
    echo -e "${YELLOW}部署配置:"
    echo -e "  目录: ${YELLOW}${DEPLOY_DIR}${NC}"
    echo -e "  备份: ${YELLOW}${BACKUP_DIR}${NC}"
    echo -e "  项目名: ${YELLOW}${PROJECT_NAME}${NC}"
    echo ""
    echo -e "${YELLOW}服务配置:"
    echo -e "  Nginx端口: ${YELLOW}${NGINX_PORT}${NC}"
    echo -e "  前端端口: ${YELLOW}${FRONTEND_PORT}${NC}"
    echo -e "  后端端口: ${YELLOW}${BACKEND_PORT}${NC}"
    echo ""
    echo -e "${YELLOW}Java配置:"
    echo -e "  版本: ${YELLOW}${JAVA_VERSION}${NC}"
    echo -e "   选项: ${YELLOW}${JAVA_OPTS}${NC}"
    echo ""
    echo -e "${YELLOW}备份配置:"
    echo -e "  保留天数: ${YELLOW}${BACKUP_RETENTION_DAYS}天${NC}"
    echo -e "  自动备份: ${YELLOW}${AUTO_BACKUP_ENABLED}${NC}"
    echo ""
}

# 检查操作系统
check_os() {
    echo -e "${BLUE}[5/10] 检查操作系统...${NC}"

    # 先检测包管理器（使用多种检测方法增强可靠性）
    PKG_MANAGER=""
    if which apt &> /dev/null || command -v apt &> /dev/null; then
        PKG_MANAGER="apt"
        PKG_UPDATE="apt update"
        PKG_INSTALL="apt install -y"
    elif which yum &> /dev/null || command -v yum &> /dev/null || [ -f /usr/bin/yum ]; then
        PKG_MANAGER="yum"
        PKG_UPDATE="yum update -y"
        PKG_INSTALL="yum install -y"
        # 如果yum不在PATH中，添加到PATH
        if ! command -v yum &> /dev/null && [ -f /usr/bin/yum ]; then
            export PATH="/usr/bin:$PATH"
            echo -e "${YELLOW}⚠️  已添加 /usr/bin 到 PATH${NC}"
        fi
    elif which dnf &> /dev/null || command -v dnf &> /dev/null; then
        PKG_MANAGER="dnf"
        PKG_UPDATE="dnf update -y"
        PKG_INSTALL="dnf install -y"
    elif which zypper &> /dev/null || command -v zypper &> /dev/null; then
        PKG_MANAGER="zypper"
        PKG_UPDATE="zypper refresh"
        PKG_INSTALL="zypper install -y"
    elif which apk &> /dev/null || command -v apk &> /dev/null; then
        PKG_MANAGER="apk"
        PKG_UPDATE="apk update"
        PKG_INSTALL="apk add"
    elif which pacman &> /dev/null || command -v pacman &> /dev/null; then
        PKG_MANAGER="pacman"
        PKG_UPDATE="pacman -Sy"
        PKG_INSTALL="pacman -S --noconfirm"
    fi

    # 如果仍然没有检测到包管理器，显示调试信息
    if [ -z "$PKG_MANAGER" ]; then
        echo -e "${RED}无法检测包管理器，请手动安装所需依赖${NC}"
        echo -e "${YELLOW}支持的包管理器: apt, yum, dnf, zypper, apk, pacman${NC}"
        echo -e "${YELLOW}正在显示调试信息...${NC}"
        debug_package_manager
        exit 1
    fi

    # 检测操作系统类型
    if [ -f /etc/redhat-release ]; then
        OS="centos"
    elif [ -f /etc/debian_version ]; then
        OS="debian"
    elif [ -f /etc/lsb-release ]; then
        OS="ubuntu"
    elif [ -f /etc/alpine-release ]; then
        OS="alpine"
    elif [ -f /etc/arch-release ]; then
        OS="arch"
    else
        OS="unknown"
    fi

    echo -e "${GREEN}✓ 操作系统: ${OS} | 包管理器: ${PKG_MANAGER}${NC}"
}

# 设置全局环境变量
setup_environment_variables() {
    echo -e "${BLUE}[6/10] 设置环境变量...${NC}"

    if [ -d "/usr/lib/jvm/java-${JAVA_VERSION}-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
    elif [ -d "/usr/lib/jvm/java-${JAVA_VERSION}" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}"
    elif command -v java &> /dev/null; then
        JAVA_PATH=$(which java)
        JAVA_DIR=$(dirname $(dirname $(dirname "$JAVA_PATH")))
        export JAVA_HOME="$JAVA_DIR"
    else
        echo -e "${YELLOW}⚠️  未找到JDK ${JAVA_VERSION}，环境变量可能未正确设置${NC}"
    fi

    export PATH="$JAVA_HOME/bin:$PATH"

    {
        echo "export JAVA_HOME=$JAVA_HOME"
        echo "export PATH=$PATH"
        echo "export JAVA_OPTS=\"$JAVA_OPTS\""
    } | tee /etc/profile.d/${PROJECT_NAME}.sh > /dev/null

    source /etc/profile.d/${PROJECT_NAME}.sh

    echo -e "${GREEN}✓ 环境变量设置完成${NC}"
}

# 检查和安装依赖
install_dependencies() {
    echo -e "${BLUE}[7/10] 检查和安装依赖...${NC}"

    # 更新包索引
    echo -e "${YELLOW}更新包索引...${NC}"
    ${PKG_UPDATE} || echo -e "${YELLOW}⚠️  包索引更新失败，继续...${NC}"

    # 安装git
    if ! command -v git &> /dev/null; then
        echo -e "${YELLOW}安装git...${NC}"
        ${PKG_INSTALL} git || error_exit "安装git失败"
    fi

    # 安装nginx
    if ! command -v nginx &> /dev/null; then
        echo -e "${YELLOW}安装nginx...${NC}"
        ${PKG_INSTALL} nginx || error_exit "安装nginx失败"
    fi

    # 安装Node.js和npm
    if ! command -v node &> /dev/null || ! command -v npm &> /dev/null; then
        echo -e "${YELLOW}安装Node.js和npm...${NC}"
        ${PKG_INSTALL} nodejs npm || error_exit "安装Node.js和npm失败"
    fi

    # 安装JDK 17
    if ! command -v java &> /dev/null; then
        echo -e "${YELLOW}安装JDK ${JAVA_VERSION}...${NC}"
        case "$PKG_MANAGER" in
            apt)
                ${PKG_INSTALL} openjdk-${JAVA_VERSION}-jdk || error_exit "安装JDK失败"
                ;;
            yum|dnf)
                ${PKG_INSTALL} java-${JAVA_VERSION}-openjdk-devel || error_exit "安装JDK失败"
                ;;
            zypper)
                ${PKG_INSTALL} java-${JAVA_VERSION}-openjdk-devel || error_exit "安装JDK失败"
                ;;
            apk)
                ${PKG_INSTALL} openjdk${JAVA_VERSION} || error_exit "安装JDK失败"
                ;;
            pacman)
                ${PKG_INSTALL} jdk${JAVA_VERSION}-openjdk || error_exit "安装JDK失败"
                ;;
        esac
    fi

    # 安装Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${YELLOW}安装Maven...${NC}"
        ${PKG_INSTALL} maven || error_exit "安装Maven失败"
    fi

    echo -e "${GREEN}✓ 依赖安装完成${NC}"
}

# 配置MySQL数据库
setup_database() {
    echo -e "${BLUE}[8/10] 配置MySQL数据库...${NC}"

    # 测试MySQL连接
    echo -e "${YELLOW}测试MySQL连接...${NC}"
    if ! mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} -e "SELECT 1;" &> /dev/null; then
        echo -e "${RED}✗ MySQL连接失败${NC}"
        echo -e "${RED}请检查数据库配置: 主机:${DB_HOST}, 用户:${DB_USER}, 端口:${DB_PORT}${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ MySQL连接正常${NC}"

    # 创建数据库
    echo -e "${YELLOW}创建数据库...${NC}"
    if ! mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"; then
        error_exit "创建数据库失败"
    fi

    # 导入数据库结构
    if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
        echo -e "${YELLOW}导入数据库结构...${NC}"
        if ! mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql"; then
            error_exit "导入数据库结构失败"
        fi
    fi

    # 导入数据库数据
    if [ -f "${DEPLOY_DIR}/database/data.sql" ]; then
        echo -e "${YELLOW}导入数据库数据...${NC}"
        if ! mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/data.sql"; then
            echo -e "${YELLOW}⚠️  导入数据库数据失败（可能是数据已存在）${NC}"
        fi
    fi

    echo -e "${GREEN}✓ 数据库配置完成${NC}"
}

# 克隆或更新代码
setup_code() {
    echo -e "${BLUE}[9/10] 部署代码...${NC}"

    if [ -d "${DEPLOY_DIR}" ]; then
        echo -e "${YELLOW}项目目录已存在，正在备份...${NC}"
        mv "${DEPLOY_DIR}" "${DEPLOY_DIR}_backup_$(date +%Y%m%d_%H%M%S)" || true
    fi

    echo -e "${YELLOW}克隆代码仓库...${NC}"
    if [ -n "$GIT_BRANCH" ]; then
        git clone -b ${GIT_BRANCH} ${GIT_REPO} ${DEPLOY_DIR}
    else
        git clone ${GIT_REPO} ${DEPLOY_DIR}
    fi

    echo -e "${GREEN}✓ 代码部署完成${NC}"
}

# 构建前端
build_frontend() {
    echo -e "${BLUE}[10/10] 构建前端...${NC}"

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
    echo -e "${BLUE}[11/10] 构建后端...${NC}"

    cd ${BACKEND_DIR}

    echo -e "${YELLOW}构建后端...${NC}"
    JAVA_OPTS="$JAVA_OPTS" mvn clean package -DskipTests

    echo -e "${GREEN}✓ 后端构建完成${NC}"
}

# 配置nginx
configure_nginx() {
    echo -e "${BLUE}[12/10] 配置nginx...${NC}"

    NGINX_CONF="/etc/nginx/conf.d/${PROJECT_NAME}.conf"

    # 使用echo命令逐行创建配置文件
    {
        echo "server {"
        echo "  listen ${NGINX_PORT};"
        echo "  server_name _;"
        echo ""
        echo "  location / {"
        echo "    root ${FRONTEND_DIR}/dist;"
        echo "    try_files \$uri \$uri/ /index.html;"
        echo "    index index.html;"
        echo ""
        echo "    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot) {"
        echo "      expires 30d;"
        echo "      add_header Cache-Control \"public, immutable\";"
        echo "    }"
        echo ""
        echo "  location /api {"
        echo "    proxy_pass http://localhost:${BACKEND_PORT};"
        echo "    proxy_set_header Host \$host;"
        echo "    proxy_set_header X-Real-IP \$remote_addr;"
        echo "    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;"
        echo "    proxy_set_header X-Forwarded-Proto \$scheme;"
        echo ""
        echo "    proxy_connect_timeout 60s;"
        echo "    proxy_send_timeout 60s;"
        echo "    proxy_read_timeout 60s;"
        echo "  }"
        echo "}"
    } > ${NGINX_CONF}

    # 测试nginx配置
    echo -e "${YELLOW}测试nginx配置...${NC}"
    if ! nginx -t; then
        echo -e "${RED}✗ nginx配置测试失败${NC}"
        exit 1
    fi

    # 重启nginx
    echo -e "${YELLOW}重启nginx服务...${NC}"
    systemctl restart nginx || systemctl start nginx
    systemctl enable nginx

    echo -e "${GREEN}✓ nginx配置完成${NC}"
}

# 创建systemd服务文件
create_systemd_services() {
    echo -e "${BLUE}[13/10] 创建systemd服务...${NC}"

    # 创建后端服务文件
    BACKEND_SERVICE_FILE="/etc/systemd/system/${PROJECT_NAME}-backend.service"
    {
        echo "[Unit]"
        echo "Description=Reference Material Management Backend"
        echo "After=network.target"
        echo ""
        echo "[Service]"
        echo "Type=simple"
        echo "User=${SERVICE_USER}"
        echo "WorkingDirectory=${BACKEND_DIR}"
        echo "Environment=\"JAVA_HOME=${JAVA_HOME}\" \"JAVA_OPTS=${JAVA_OPTS}\" \"PATH=${PATH}\""
        echo "ExecStart=${JAVA_HOME}/bin/java ${JAVA_OPTS} -jar ${BACKEND_DIR}/target/reference-material-management-1.0.0.jar"
        echo "Restart=on-failure"
        echo "RestartSec=10"
        echo ""
        echo "[Install]"
        echo "WantedBy=multi-user.target"
    } > ${BACKEND_SERVICE_FILE}

    # 创建前端服务文件
    FRONTEND_SERVICE_FILE="/etc/systemd/system/${PROJECT_NAME}-frontend.service"
    {
        echo "[Unit]"
        echo "Description=Reference Material Management Frontend"
        echo "After=network.target"
        echo ""
        echo "[Service]"
        echo "Type=simple"
        echo "User=${SERVICE_USER}"
        echo "WorkingDirectory=${FRONTEND_DIR}"
        echo "Environment=\"PATH=${PATH}\""
        echo "ExecStart=/usr/bin/node ${FRONTEND_DIR}/node_modules/.bin/vite --port ${FRONTEND_PORT} --host"
        echo "Restart=on-failure"
        echo "RestartSec=10"
        echo ""
        echo "[Install]"
        echo "WantedBy=multi-user.target"
    } > ${FRONTEND_SERVICE_FILE}

    # 重载systemd
    systemctl daemon-reload

    echo -e "${GREEN}✓ systemd服务创建完成${NC}"
}

# 启动服务
start_services() {
    echo -e "${BLUE}[14/10] 启动服务...${NC}"

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
    echo -e "${BLUE}[15/10] 部署完成${NC}"
    echo ""
    echo -e "${GREEN}╔══════════════════════════════╗${NC}"
    echo -e "${GREEN}║     部署成功！                             ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════╝${NC}"
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
    echo -e "⚙️  环境变量:"
    echo -e "  JAVA_HOME: ${YELLOW}$JAVA_HOME${NC}"
    echo -e "  PATH: ${YELLOW}$PATH${NC}"
    echo ""
    echo -e "🛠️  管理命令:"
    echo -e "  查看后端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-backend -f${NC}"
    echo -e "  查看前端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-frontend -f${NC}"
    echo -e "  重启后端: ${YELLOW}systemctl restart ${PROJECT_NAME}-backend${NC}"
    echo -e "  重启前端: ${YELLOW}systemctl restart ${PROJECT_NAME}-frontend${NC}"
    echo -e "  重启nginx: ${YELLOW}systemctl restart nginx${NC}"
    echo ""
    echo -e "📋 配置文件位置: ${YELLOW}${CONFIG_FILE}${NC}"
    echo -e "  修改配置: ${YELLOW}vi ${CONFIG_FILE}${NC} && source deploy.sh${NC}"
    echo ""
}

# 调试包管理器
debug_package_manager() {
    echo -e "${YELLOW}调试信息: 检测可用包管理器${NC}"
    echo ""
    echo -e "${YELLOW}检查常见包管理器...${NC}"

    for pkg in apt yum dnf zypper apk pacman; do
        if command -v $pkg &> /dev/null; then
            echo -e "  ${GREEN}✓${NC} $pkg 可用"
        elif which $pkg &> /dev/null; then
            echo -e "  ${GREEN}✓${NC} $pkg 可用 (通过which检测)"
        else
            echo -e "  ${RED}✗${NC} $pkg 不可用"
        fi
    done

    echo ""
    echo -e "${YELLOW}操作系统信息:${NC}"
    if [ -f /etc/os-release ]; then
        cat /etc/os-release
    elif [ -f /etc/redhat-release ]; then
        cat /etc/redhat-release
    elif [ -f /etc/debian_version ]; then
        cat /etc/debian_version
    fi

    echo ""
    echo -e "${YELLOW}PATH环境变量:${NC}"
    echo "  $PATH"

    echo ""
    echo -e "${YELLOW}常见命令路径:${NC}"
    for cmd in apt yum dnf zypper apk pacman git; do
        if which $cmd &> /dev/null; then
            echo -e "  $cmd: ${GREEN}$(which $cmd)${NC}"
        fi
    done
}
show_help() {
    echo -e "${GREEN}使用方法${NC}"
    echo ""
    echo -e "${YELLOW}基本用法:${NC}"
    echo -e "${YELLOW}  sudo ./deploy.sh${NC}"
    echo ""
    echo -e "${GREEN}配置文件选项:${NC}"
    echo -e "${YELLOW}  deploy-config.env${NC}  - 部署配置文件"
    echo ""
    echo -e "${GREEN}环境变量:${NC}"
    echo -e "${YELLOW}  JAVA_HOME=${NC}  - 指定JDK安装路径"
    echo -e "${YELLOW}  JAVA_OPTS=${NC} - 指定JVM参数"
    echo ""
    echo ""
    echo -e "${GREEN}调试选项:${NC}"
    echo -e "${YELLOW}  ./deploy.sh --debug    ${NC}  - 调试包管理器检测"
    echo -e "${GREEN}示例配置文件:${NC}"
    echo -e "${YELLOW}# 数据库配置${NC}"
    echo -e "${YELLOW}DB_HOST=your-mysql-host${NC}"
    echo -e "${YELLOW}DB_PASS=your_password${NC}"
    echo -e "${YELLOW}DB_PORT=3306${NC}"
    echo ""
    echo -e "${GREEN}更多信息请查看: DEPLOYMENT.md${NC}"
    echo ""
}

# 主函数
main() {
    # 检查是否显示帮助
    if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
        show_help
        exit 0
    fi

    # 检查是否需要调试包管理器
    if [ "$1" = "--debug" ]; then
        debug_package_manager
        exit 0
    fi

    check_root
    check_os
    load_config
    show_current_config
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
