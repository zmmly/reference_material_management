# 生产环境部署配置指南

## 概述

本文档说明标准物质管理系统在生产环境的配置要点，重点关注可配置的文件路径。

## 核心配置项

### 1. 数据库连接

**环境变量方式（推荐）：**

```bash
export DB_URL="jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export DB_USER="root"
export DB_PASSWORD="your_password"
```

**配置文件方式：**

编辑 `application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/reference_material_management
    username: root
    password: your_password
```

### 2. 备份文件存储路径

系统备份功能会将数据库导出为 SQL 文件，需要配置存储位置。

**环境变量方式：**

```bash
export BACKUP_DIR=/var/backups/rmm
export MYSQLDUMP_PATH=/usr/bin/mysqldump
```

**配置文件方式：**

```yaml
backup:
  directory: /var/backups/rmm
  mysqldump-path: /usr/bin/mysqldump
```

**默认值：**
- 开发环境：`backups`（项目相对路径）
- 生产环境：`/var/backups/rmm`

**详细说明：** 参见 [backup-configuration.md](./backup-configuration.md)

### 3. 上传文件存储路径

入库登记等功能的证书、文档上传位置。

**环境变量方式：**

```bash
export UPLOAD_PATH=/var/rmm/uploads
```

**配置文件方式：**

```yaml
upload:
  path: /var/rmm/uploads
```

**默认值：**
- 开发环境：`uploads`（相对于用户主目录）
- 生产环境：`/var/rmm/uploads`

**详细说明：** 参见 [upload-configuration.md](./upload-configuration.md)

## 完整部署配置

### 使用 systemd 服务（推荐）

创建 `/etc/systemd/system/rmm.service`：

```ini
[Unit]
Description=Reference Material Management System
After=network.target mysql.service

[Service]
Type=simple
User=rmm
WorkingDirectory=/opt/rmm

# 数据库配置
Environment="DB_URL=jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
Environment="DB_USER=root"
Environment="DB_PASSWORD=your_password"

# 备份配置
Environment="BACKUP_DIR=/var/backups/rmm"
Environment="MYSQLDUMP_PATH=/usr/bin/mysqldump"

# 上传配置
Environment="UPLOAD_PATH=/var/rmm/uploads"

# CORS 配置
Environment="CORS_ORIGINS=http://localhost,http://127.0.0.1,http://your-server-ip"

# JVM 参数
ExecStart=/usr/bin/java \
    -Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -jar /opt/rmm/reference-material-management-1.0.0.jar \
    --spring.profiles.active=prod

Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 使用 Docker Compose

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  app:
    image: openjdk:17-jdk-slim
    container_name: rmm-app
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:mysql://mysql:3306/reference_material_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
      - DB_USER=root
      - DB_PASSWORD=your_password
      - BACKUP_DIR=/var/backups/rmm
      - MYSQLDUMP_PATH=/usr/bin/mysqldump
      - UPLOAD_PATH=/var/rmm/uploads
      - CORS_ORIGINS=http://localhost,http://your-server-ip
    volumes:
      - ./backend/target/reference-material-management-1.0.0.jar:/app.jar
      - /var/backups/rmm:/var/backups/rmm
      - /var/rmm/uploads:/var/rmm/uploads
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    command: java -Xms512m -Xmx1024m -XX:+UseG1GC -jar /app.jar

  mysql:
    image: mysql:8.0
    container_name: rmm-mysql
    restart: unless-stopped
    environment:
      - MYSQL_ROOT_PASSWORD=your_password
      - MYSQL_DATABASE=reference_material_management
    volumes:
      - mysql-data:/var/lib/mysql
      - ./database/schema.sql:/docker-entrypoint-initdb.d/1-schema.sql
      - ./database/data.sql:/docker-entrypoint-initdb.d/2-data.sql
    ports:
      - "3306:3306"

volumes:
  mysql-data:
```

## 目录结构准备

### 1. 创建必要的目录

```bash
# 创建备份目录
sudo mkdir -p /var/backups/rmm
sudo chown rmm:rmm /var/backups/rmm
sudo chmod 755 /var/backups/rmm

# 创建上传目录
sudo mkdir -p /var/rmm/uploads
sudo chown rmm:rmm /var/rmm/uploads
sudo chmod 755 /var/rmm/uploads

# 创建日志目录
sudo mkdir -p /var/log/rmm
sudo chown rmm:rmm /var/log/rmm
sudo chmod 755 /var/log/rmm
```

### 2. 安装 MySQL 客户端工具

```bash
# CentOS/RHEL
sudo yum install mysql

# Ubuntu/Debian
sudo apt-get install mysql-client

# 验证
which mysqldump
mysqldump --version
```

### 3. 设置目录权限

```bash
# 确保运行用户有权限访问这些目录
sudo chown -R rmm:rmm /var/backups/rmm
sudo chown -R rmm:rmm /var/rmm/uploads
sudo chown -R rmm:rmm /var/log/rmm

# 设置适当的权限
sudo chmod -R 755 /var/backups/rmm
sudo chmod -R 755 /var/rmm/uploads
sudo chmod -R 755 /var/log/rmm
```

## 部署步骤

### 1. 使用部署脚本（推荐）

```bash
# 更新代码并部署
./scripts/deploy.sh deploy

# 仅重启服务
./scripts/deploy.sh restart

# 查看状态
./scripts/deploy.sh status

# 查看日志
./scripts/deploy.sh logs
```

### 2. 手动部署

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 编译打包
cd backend
mvn clean package -DskipTests

# 3. 停止旧服务
sudo systemctl stop rmm

# 4. 复制新的 jar 文件
sudo cp target/reference-material-management-1.0.0.jar /opt/rmm/

# 5. 启动新服务
sudo systemctl start rmm

# 6. 检查状态
sudo systemctl status rmm
```

## 验证部署

### 1. 检查服务状态

```bash
# systemd 方式
sudo systemctl status rmm

# 或查看日志
tail -f /var/log/rmm/backend.log
```

### 2. 测试备份功能

```bash
# 通过 API 测试（需要认证）
curl -X POST http://localhost:8080/api/system/backup \
  -H "Authorization: Bearer YOUR_TOKEN"

# 检查备份文件
ls -lh /var/backups/rmm/
```

### 3. 测试上传功能

```bash
# 通过前端界面上传证书
# 访问：http://your-server-ip
# 导航到：入库管理 -> 新增入库 -> 上传证书

# 检查上传文件
ls -lh /var/rmm/uploads/certificate/
```

## 监控和维护

### 1. 日志轮转

创建 `/etc/logrotate.d/rmm`：

```
/var/log/rmm/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 rmm rmm
}
```

### 2. 磁盘空间监控

```bash
# 添加到 crontab
# 每天检查磁盘使用情况
0 0 * * * df -h /var/backups/rmm /var/rmm/uploads | mail -s "RMM Disk Usage" admin@example.com
```

### 3. 定期备份

```bash
# 添加到 crontab
# 每天凌晨 2 点自动备份
0 2 * * * curl -X POST http://localhost:8080/api/system/backup -H "Authorization: Bearer YOUR_TOKEN"
```

## 故障排查

### 问题：备份失败

**检查清单：**
1. mysqldump 是否可用：`which mysqldump`
2. 备份目录权限：`ls -ld /var/backups/rmm`
3. 数据库连接是否正常
4. 查看日志：`tail -f /var/log/rmm/backend.log`

### 问题：文件上传失败

**检查清单：**
1. 上传目录权限：`ls -ld /var/rmm/uploads`
2. 磁盘空间是否充足：`df -h /var/rmm/uploads`
3. 文件大小是否超过限制
4. 查看日志：`tail -f /var/log/rmm/backend.log`

### 问题：服务无法启动

**检查清单：**
1. 端口是否被占用：`netstat -tlnp | grep 8080`
2. Java 是否安装：`java -version`
3. 数据库是否运行：`systemctl status mysql`
4. 配置文件是否正确：`cat /opt/rmm/application-prod.yml`
5. 查看日志：`journalctl -u rmm -n 50`

## 安全建议

1. **数据库密码**：使用强密码，并通过环境变量传递
2. **文件权限**：确保上传和备份目录权限适当（755）
3. **防火墙**：只开放必要的端口（80, 443, 8080）
4. **HTTPS**：生产环境建议使用 HTTPS
5. **定期更新**：定期更新依赖和系统补丁

## 相关文档

- [备份配置详细说明](./backup-configuration.md)
- [上传配置详细说明](./upload-configuration.md)
- [项目说明](../CLAUDE.md)
