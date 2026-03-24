# 系统备份配置说明

## 概述

系统备份功能支持自动备份数据库，备份文件位置和 mysqldump 工具路径都可以灵活配置。

## 配置方式

### 1. 备份文件存储位置

**配置方式一：环境变量（推荐用于生产环境）**

```bash
export BACKUP_DIR=/var/backups/rmm
```

**配置方式二：配置文件**

编辑 `backend/src/main/resources/application.yml` 或 `application-prod.yml`：

```yaml
backup:
  directory: /var/backups/rmm
```

**默认值：**
- 开发环境：`backups`（项目相对路径）
- 生产环境：`/var/backups/rmm`（需要确保目录存在且有写入权限）

### 2. mysqldump 工具路径

系统会自动检测 mysqldump 工具，通常**无需手动配置**。

**自动检测逻辑：**
- **Linux/Mac**：直接使用 `mysqldump`（从系统 PATH 查找）
- **Windows**：依次尝试以下路径
  - `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe`
  - `C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqldump.exe`
  - `C:\Program Files\MySQL\MySQL Server 5.6\bin\mysqldump.exe`
  - `mysqldump`（从系统 PATH 查找）

**手动配置方式（如果自动检测失败）：**

**方式一：环境变量（推荐）**

```bash
# Linux/Mac
export MYSQLDUMP_PATH=/usr/bin/mysqldump

# Windows
set MYSQLDUMP_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe
```

**方式二：配置文件**

编辑 `application.yml` 或 `application-prod.yml`：

```yaml
backup:
  mysqldump-path: /usr/bin/mysqldump  # Linux
  # mysqldump-path: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe  # Windows
```

## 生产环境部署

### 方式一：使用环境变量

在启动脚本中设置环境变量：

```bash
export BACKUP_DIR=/var/backups/rmm
export MYSQLDUMP_PATH=/usr/bin/mysqldump

java -jar target/reference-material-management-1.0.0.jar --spring.profiles.active=prod
```

### 方式二：使用 systemd 服务（推荐）

创建 systemd 服务文件 `/etc/systemd/system/rmm.service`：

```ini
[Unit]
Description=Reference Material Management System
After=network.target mysql.service

[Service]
Type=simple
User=rmm
WorkingDirectory=/opt/rmm
Environment="BACKUP_DIR=/var/backups/rmm"
Environment="MYSQLDUMP_PATH=/usr/bin/mysqldump"
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC -jar /opt/rmm/reference-material-management-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
# 创建备份目录
sudo mkdir -p /var/backups/rmm
sudo chown rmm:rmm /var/backups/rmm

# 启动服务
sudo systemctl daemon-reload
sudo systemctl start rmm
sudo systemctl enable rmm
```

## 前提条件

### Linux 服务器

确保已安装 MySQL 客户端工具：

```bash
# CentOS/RHEL
sudo yum install mysql

# Ubuntu/Debian
sudo apt-get install mysql-client

# 验证 mysqldump 是否可用
which mysqldump
mysqldump --version
```

### 权限要求

1. **备份目录权限**：运行应用的用户必须对备份目录有写入权限
   ```bash
   sudo mkdir -p /var/backups/rmm
   sudo chown <user>:<group> /var/backups/rmm
   sudo chmod 755 /var/backups/rmm
   ```

2. **MySQL 权限**：数据库用户必须有以下权限：
   - `SELECT` - 读取数据
   - `LOCK TABLES` - 锁定表（用于一致性备份）
   - `SHOW VIEW` - 备份视图
   - `TRIGGER` - 备份触发器

## 验证配置

### 检查 mysqldump 可用性

```bash
# Linux
mysqldump --version

# 或者指定路径
/usr/bin/mysqldump --version
```

### 检查备份目录权限

```bash
# 创建测试文件
touch /var/backups/rmm/test.txt
rm /var/backups/rmm/test.txt
```

### 测试备份功能

1. 启动应用
2. 访问系统管理 -> 系统备份
3. 点击"创建备份"按钮
4. 检查备份文件是否生成

## 故障排查

### 错误：备份失败: mysqldump: command not found

**原因**：系统找不到 mysqldump 工具

**解决**：
1. 安装 MySQL 客户端工具（见"前提条件"）
2. 或者手动配置 mysqldump 路径（见"mysqldump 工具路径"）

### 错误：创建备份目录失败

**原因**：没有权限创建备份目录

**解决**：
```bash
sudo mkdir -p /var/backups/rmm
sudo chown <user>:<group> /var/backups/rmm
```

### 错误：备份文件创建失败

**原因**：
1. mysqldump 执行失败（检查路径和权限）
2. 数据库连接信息错误
3. MySQL 用户权限不足

**解决**：
1. 检查日志：`tail -f /var/log/rmm/backend.log`
2. 手动测试 mysqldump：
   ```bash
   mysqldump -h localhost -u root -p reference_material_management > test.sql
   ```

## 备份策略建议

1. **定期备份**：设置定时任务自动备份
   ```bash
   # crontab -e
   0 2 * * * /usr/bin/curl -X POST http://localhost:8080/api/system/backup -H "Authorization: Bearer <token>"
   ```

2. **异地备份**：定期将备份文件同步到其他服务器或云存储

3. **保留策略**：定期清理旧备份，避免磁盘空间不足

4. **监控告警**：监控备份任务执行情况和磁盘空间使用率
