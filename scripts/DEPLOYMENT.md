# 标准物质管理系统 - 云服务器部署指南

## 📋 目录

1. [环境要求](#环境要求)
2. [快速部署](#快速部署)
3. [更新应用](#更新应用)
4. [配置说明](#配置说明)
5. [故障排查](#故障排查)
6. [安全建议](#安全建议)

## 🌍 环境要求

### 最低配置
- **操作系统**: CentOS 7+ / Ubuntu 18.04+ / Debian 10+
- **内存**: 2GB RAM (推荐 4GB+)
- **磁盘**: 10GB 可用空间
- **网络**: 公网IP或域名

### 软件要求
- **Java**: JDK 17 (脚本自动安装)
- **Web服务器**: Nginx (脚本自动安装)
- **版本控制**: Git (脚本自动安装)
- **数据库**: MySQL 8.0+ (需预先安装)

### 云服务器准备
```bash
# 确保已安装MySQL
mysql --version

# 检查MySQL服务状态
systemctl status mysqld

# 确认MySQL凭据
# 用户名: root
# 密码: xjYY3687!
```

## 🚀 快速部署

### 一键部署步骤

1. **上传部署脚本到服务器**
```bash
# 方式1: 使用scp上传
scp scripts/deploy.sh root@your-server-ip:/root/
scp scripts/deploy-config.env root@your-server-ip:/root/

# 方式2: 使用wget下载（如果代码已推送到GitHub）
wget https://raw.githubusercontent.com/zmmly/reference_material_management/main/scripts/deploy.sh
chmod +x deploy.sh
```

2. **执行部署脚本**
```bash
# 登录服务器
ssh root@your-server-ip

# 给脚本执行权限
chmod +x deploy.sh

# 运行部署脚本
./deploy.sh
```

3. **部署过程自动化**
脚本会自动完成以下步骤：
- ✅ 检查操作系统和root权限
- ✅ 安装JDK 17、Git、Nginx
- ✅ 克隆代码仓库
- ✅ 创建和初始化MySQL数据库
- ✅ 构建前端和后端
- ✅ 配置Nginx反向代理
- ✅ 创建Systemd服务
- ✅ 启动所有服务
- ✅ 配置防火墙规则

### 访问应用
部署成功后，可通过以下地址访问：

```
前端: http://your-server-ip/
后端: http://your-server-ip:8080
API文档: http://your-server-ip:8080/doc.html
默认账号: admin / admin123
```

## 🔄 更新应用

### 更新步骤

1. **运行更新脚本**
```bash
# 登录服务器
ssh root@your-server-ip

# 进入项目目录
cd /opt/reference_material_management/scripts

# 运行更新脚本
./update.sh
```

2. **更新过程自动化**
脚本会自动完成：
- ✅ 备份当前数据库
- ✅ 拉取最新代码
- ✅ 更新数据库结构
- ✅ 重新构建前端和后端
- ✅ 重启所有服务
- ✅ 执行健康检查

3. **更新完成确认**
更新完成后，脚本会显示：
- 数据库备份位置
- Git提交信息
- 服务运行状态
- 管理命令列表

## ⚙️ 配置说明

### 自定义部署配置

编辑 `deploy-config.env` 文件来自定义部署参数：

```bash
# 编辑配置文件
vi deploy-config.env

# 主要配置项
DEPLOY_DIR=/opt/reference-material_management  # 部署目录
NGINX_PORT=80                            # Nginx端口
FRONTEND_PORT=3002                        # 前端开发服务器端口
BACKEND_PORT=8080                          # 后端API端口
JAVA_OPTS="-Xms512m -Xmx1024m"          # JVM参数
```

### 修改部署脚本

如需修改部署逻辑，编辑相应的脚本文件：

```bash
# 修改主部署脚本
vi deploy.sh

# 修改更新脚本
vi update.sh
```

## 🔧 故障排查

### 常见问题解决

#### 1. 服务无法启动
```bash
# 查看服务状态
systemctl status reference-material-management-backend
systemctl status reference-material-management-frontend
systemctl status nginx

# 查看服务日志
journalctl -u reference-material-management-backend -f
journalctl -u reference-material-management-frontend -f
tail -f /var/log/nginx/error.log
```

#### 2. 端口冲突
```bash
# 检查端口占用
netstat -tlnp | grep -E ':(80|3002|8080)'

# 修改脚本中的端口配置
# 在 deploy-config.env 中修改对应端口
```

#### 3. 数据库连接失败
```bash
# 测试MySQL连接
mysql -uroot -pxjYY3687! -e "SELECT VERSION();"

# 检查数据库是否存在
mysql -uroot -pxjYY3687! -e "SHOW DATABASES;"

# 手动创建数据库
mysql -uroot -pxjYY3687! -e "CREATE DATABASE reference_material_management CHARACTER SET utf8mb4;"
```

#### 4. 前端构建失败
```bash
# 清理npm缓存
cd /opt/reference_material_management/frontend
rm -rf node_modules package-lock.json

# 重新安装依赖
npm install

# 单独构建测试
npm run build
```

#### 5. 内存不足
```bash
# 减少JVM内存分配
# 在 deploy-config.env 中修改：
JAVA_OPTS="-Xms256m -Xmx512m"

# 增加Swap空间
dd if=/dev/zero of=/swapfile bs=1M count=1024
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
```

## 🔐 安全建议

### 部署后安全配置

#### 1. 修改默认密码
```bash
# 登录应用后，立即修改管理员密码
# 设置强密码（包含大小写字母、数字、特殊字符）
```

#### 2. 配置防火墙
```bash
# CentOS/RHEL
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=3002/tcp
firewall-cmd --reload

# Ubuntu/Debian
ufw allow 80/tcp
ufw allow 8080/tcp
ufw allow 3002/tcp
ufw reload
```

#### 3. 配置HTTPS
```bash
# 安装SSL证书（Let's Encrypt）
yum install certbot python2-certbot-nginx  # CentOS
apt install certbot python3-certbot-nginx  # Ubuntu

# 获取SSL证书
certbot --nginx -d your-domain.com

# 更新nginx配置添加SSL
```

#### 4. 数据库安全
```bash
# 修改MySQL root密码
mysql -uroot -p -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_strong_password';"

# 删除测试数据库
mysql -uroot -p -e "DROP DATABASE IF EXISTS test;"

# 限制远程访问
# 编辑 /etc/my.cnf 添加：
bind-address = 127.0.0.1
```

## 📊 监控和维护

### 服务监控脚本
```bash
# 创建监控脚本
cat > /usr/local/bin/check-services.sh <<'EOF'
#!/bin/bash
services=("reference-material-management-backend"
           "reference-material-management-frontend"
           "nginx"
           "mysqld")

for service in "${services[@]}"; do
    if ! systemctl is-active --quiet $service; then
        echo "$(date): $service is not running" >> /var/log/service-check.log
        systemctl restart $service
    fi
done
EOF

chmod +x /usr/local/bin/check-services.sh

# 添加定时任务
crontab -e
# 每分钟检查一次
* * * * * * /usr/local/bin/check-services.sh
```

### 数据库备份自动化
```bash
# 创建备份脚本
cat > /usr/local/bin/backup-db.sh <<'EOF'
#!/bin/bash
BACKUP_DIR="/opt/db_backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

mysqldump -uroot -pxjYY3687! reference_material_management | gzip > $BACKUP_DIR/backup_$DATE.sql.gz

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
EOF

chmod +x /usr/local/bin/backup-db.sh

# 添加每日备份任务
crontab -e
# 每天凌晨2点备份
0 2 * * * /usr/local/bin/backup-db.sh
```

## 📞 技术支持

### 部署相关问题
- 查看部署日志：`/var/log/nginx/error.log`
- 查看应用日志：`journalctl -u reference-material-management-backend -f`
- 检查系统资源：`top`, `free -h`, `df -h`

### 获取帮助
- 项目地址：https://github.com/zmmly/reference_material_management
- 问题反馈：在GitHub创建Issue
- 文档更新：提交PR到项目仓库

---

**部署成功后，请及时修改默认密码并配置防火墙规则！**
