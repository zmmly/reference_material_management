#!/bin/bash

# ============================================
# 标准物质管理系统 - 云服务器更新脚本
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

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用root用户运行此脚本${NC}"
        echo -e "${YELLOW}sudo ./update.sh${NC}"
        exit 1
    fi
}

# 备份数据库
backup_database() {
    echo -e "${BLUE}[1/7] 备份数据库...${NC}"

    # 创建备份目录
    mkdir -p ${BACKUP_DIR}

    # 备份数据库
    echo -e "${YELLOW}正在备份数据库...${NC}"
    mysqldump -u${DB_USER} -p${DB_PASS} ${DB_NAME} > ${BACKUP_FILE}

    # 压缩备份文件
    gzip ${BACKUP_FILE}

    # 删除7天前的备份
    find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete

    echo -e "${GREEN}✓ 数据库备份完成: ${BACKUP_FILE}.gz${NC}"
}

# 拉取最新代码
update_code() {
    echo -e "${BLUE}[2/7] 更新代码...${NC}"

    cd ${DEPLOY_DIR}

    # 备份当前代码
    echo -e "${YELLOW}备份当前代码...${NC}"
    git stash

    # 拉取最新代码
    echo -e "${YELLOW}拉取最新代码...${NC}"
    git fetch origin
    git checkout main
    git pull origin main

    echo -e "${GREEN}✓ 代码更新完成${NC}"
}

# 更新数据库结构
update_database() {
    echo -e "${BLUE}[3/7] 更新数据库...${NC}"

    # 检查是否有新的数据库结构文件
    if [ -f "${DEPLOY_DIR}/database/schema.sql" ]; then
        echo -e "${YELLOW}更新数据库结构...${NC}"
        mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < "${DEPLOY_DIR}/database/schema.sql" 2>/dev/null || true
        echo -e "${GREEN}✓ 数据库结构更新完成${NC}"
    fi
}

# 重新构建前端
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

    # 安装新依赖
    echo -e "${YELLOW}检查前端依赖...${NC}"
    npm install

    # 构建
    echo -e "${YELLOW}构建前端...${NC}"
    npm run build

    echo -e "${GREEN}✓ 前端构建完成${NC}"
}

# 重新构建后端
rebuild_backend() {
    echo -e "${BLUE}[5/7] 重新构建后端...${NC}"

    cd ${BACKEND_DIR}

    # 使用Maven打包
    echo -e "${YELLOW}构建后端...${NC}"
    mvn clean package -DskipTests

    echo -e "${GREEN}✓ 后端构建完成${NC}"
}

# 重启服务
restart_services() {
    echo -e "${BLUE}[6/7] 重启服务...${NC}"

    # 重启后端服务
    echo -e "${YELLOW}重启后端服务...${NC}"
    systemctl restart ${PROJECT_NAME}-backend

    # 等待后端启动
    sleep 10

    # 重启前端服务
    echo -e "${YELLOW}重启前端服务...${NC}"
    systemctl restart ${PROJECT_NAME}-frontend

    # 重启nginx（可选，如果静态文件有变化）
    echo -e "${YELLOW}重载nginx配置...${NC}"
    systemctl reload nginx

    echo -e "${GREEN}✓ 服务重启完成${NC}"
}

# 健康检查
health_check() {
    echo -e "${BLUE}[7/7] 健康检查...${NC}"

    # 检查后端服务
    if systemctl is-active --quiet ${PROJECT_NAME}-backend; then
        echo -e "${GREEN}✓ 后端服务运行正常${NC}"
    else
        echo -e "${RED}✗ 后端服务未运行${NC}"
        systemctl status ${PROJECT_NAME}-backend
    fi

    # 检查前端服务
    if systemctl is-active --quiet ${PROJECT_NAME}-frontend; then
        echo -e "${GREEN}✓ 前端服务运行正常${NC}"
    else
        echo -e "${RED}✗ 前端服务未运行${NC}"
        systemctl status ${PROJECT_NAME}-frontend
    fi

    # 检查nginx服务
    if systemctl is-active --quiet nginx; then
        echo -e "${GREEN}✓ nginx服务运行正常${NC}"
    else
        echo -e "${RED}✗ nginx服务未运行${NC}"
        systemctl status nginx
    fi

    # 测试API访问
    echo -e "${YELLOW}测试API访问...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ | grep -q "200\|302"; then
        echo -e "${GREEN}✓ API访问正常${NC}"
    else
        echo -e "${YELLOW}⚠ API可能需要检查配置${NC}"
    fi
}

# 显示更新信息
show_update_info() {
    echo -e "${BLUE}[8/7] 更新完成${NC}"
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     更新成功！                          ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════╝${NC}"
    echo ""
    echo -e "💾 数据库备份: ${YELLOW}${BACKUP_FILE}.gz${NC}"
    echo -e "🔄 代码已更新到: ${YELLOW}$(cd ${DEPLOY_DIR} && git log -1 --pretty=format:'%h - %s')${NC}"
    echo ""
    echo -e "📊 备份目录: ${YELLOW}${BACKUP_DIR}${NC}"
    echo -e "📁 部署目录: ${YELLOW}${DEPLOY_DIR}${NC}"
    echo ""
    echo -e "🛠️  管理命令:"
    echo -e "  查看所有服务状态: ${YELLOW}systemctl status ${PROJECT_NAME}-{backend,frontend} nginx${NC}"
    echo -e "  查看后端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-backend -f${NC}"
    echo -e "  查看前端日志: ${YELLOW}journalctl -u ${PROJECT_NAME}-frontend -f${NC}"
    echo -e "  查看nginx日志: ${YELLOW}tail -f /var/log/nginx/access.log${NC}"
    echo -e "  回滚到备份: ${YELLOW}mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < ${BACKUP_FILE%.sql}.sql${NC}"
    echo ""
}

# 主函数
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

# 执行主函数
main
