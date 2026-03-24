# 文件上传配置说明

## 概述

系统支持上传证书、文档等文件（入库登记、采购验收等模块），上传路径可以灵活配置。

## 配置方式

### 1. 上传文件存储位置

**配置方式一：环境变量（推荐用于生产环境）**

```bash
export UPLOAD_PATH=/var/rmm/uploads
```

**配置方式二：配置文件**

编辑 `backend/src/main/resources/application.yml` 或 `application-prod.yml`：

```yaml
upload:
  path: /var/rmm/uploads
```

**默认值：**
- 开发环境：`uploads`（相对于用户主目录，例如：`/home/user/uploads`）
- 生产环境：`/var/rmm/uploads`（绝对路径，需要确保目录存在且有写入权限）

### 2. 文件存储结构

上传的文件按照以下结构组织：

```
{upload.path}/
├── certificate/           # 证书文件
│   └── 2024/
│       └── 03/
│           ├── abc123.pdf
│           └── def456.jpg
├── document/              # 文档文件
│   └── 2024/
│       └── 03/
│           └── xyz789.docx
└── ...
```

- 按 `type`（文件类型）分类存储
- 按年月（`yyyy/MM`）组织目录结构
- 文件名使用 UUID 避免冲突

## 生产环境部署

### 方式一：使用环境变量

在启动脚本中设置环境变量：

```bash
export UPLOAD_PATH=/var/rmm/uploads

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
Environment="UPLOAD_PATH=/var/rmm/uploads"
Environment="BACKUP_DIR=/var/backups/rmm"
Environment="MYSQLDUMP_PATH=/usr/bin/mysqldump"
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC -jar /opt/rmm/reference-material-management-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 方式三：修改配置文件

直接编辑 `application-prod.yml`：

```yaml
upload:
  path: /var/rmm/uploads
```

## 权限要求

### 1. 目录权限

运行应用的用户必须对上传目录有写入权限：

```bash
# 创建上传目录
sudo mkdir -p /var/rmm/uploads

# 设置所有者和权限
sudo chown -R rmm:rmm /var/rmm/uploads
sudo chmod -R 755 /var/rmm/uploads
```

### 2. 磁盘空间

根据实际使用情况规划磁盘空间：
- 证书文件：通常每个文件 1-5 MB
- 文档文件：通常每个文件 1-10 MB
- 建议预留至少 10GB 空间，并设置监控告警

## 文件访问

### API 端点

系统提供以下文件访问接口：

1. **上传文件**
   - **POST** `/api/upload`
   - 参数：`file` (文件), `type` (文件类型，默认 `certificate`)
   - 返回：文件相对路径

2. **预览文件**
   - **GET** `/api/upload/preview?path={文件路径}`
   - 返回：文件内容（支持 PDF、图片、Word 等格式在线预览）

3. **删除文件**
   - **DELETE** `/api/upload?path={文件路径}`
   - 返回：操作结果

### 前端使用示例

```javascript
// 上传证书
const formData = new FormData()
formData.append('file', file)
formData.append('type', 'certificate')

const response = await axios.post('/api/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})

const filePath = response.data.data // 获取文件路径

// 预览证书
const previewUrl = `/api/upload/preview?path=${encodeURIComponent(filePath)}`
window.open(previewUrl, '_blank')
```

## 验证配置

### 1. 检查目录权限

```bash
# 创建测试文件
touch /var/rmm/uploads/test.txt
rm /var/rmm/uploads/test.txt
```

### 2. 测试上传功能

1. 启动应用
2. 访问入库登记页面
3. 上传一个证书文件
4. 检查文件是否保存到指定目录

### 3. 查看日志

```bash
# 查看上传日志
tail -f /var/log/rmm/backend.log | grep "File uploaded"
```

成功上传后会看到类似日志：
```
INFO  c.rmm.controller.UploadController - File uploaded: certificate.pdf -> /var/rmm/uploads/certificate/2024/03/abc123.pdf
INFO  c.rmm.controller.UploadController - Upload path configured: /var/rmm/uploads
```

## 故障排查

### 错误：文件上传失败：权限被拒绝

**原因**：运行应用的用户对上传目录没有写入权限

**解决**：
```bash
sudo chown -R <user>:<group> /var/rmm/uploads
sudo chmod -R 755 /var/rmm/uploads
```

### 错误：上传目录创建失败

**原因**：父目录权限不足或磁盘空间不足

**解决**：
1. 检查磁盘空间：`df -h /var`
2. 检查父目录权限：`ls -ld /var/rmm`
3. 手动创建目录：
   ```bash
   sudo mkdir -p /var/rmm/uploads
   sudo chown -R <user>:<group> /var/rmm/uploads
   ```

### 错误：找不到上传的文件

**原因**：
1. 路径配置不一致（开发环境和生产环境配置不同）
2. 相对路径解析错误

**解决**：
1. 使用绝对路径配置
2. 检查日志确认实际保存路径
3. 验证数据库中存储的文件路径格式

### 文件预览失败

**原因**：
1. 文件路径错误
2. 文件已被删除或移动
3. 浏览器不支持该文件类型

**解决**：
1. 检查文件是否存在：`ls -l /var/rmm/uploads/certificate/2024/03/abc123.pdf`
2. 检查文件权限：`chmod 644 /var/rmm/uploads/certificate/2024/03/abc123.pdf`
3. 使用支持的文件格式（PDF、JPG、PNG、DOC、DOCX 等）

## 备份策略建议

### 1. 定期备份上传文件

```bash
# 备份上传文件目录
rsync -av /var/rmm/uploads/ /backup/rmm/uploads/
```

### 2. 与数据库备份配合

上传文件和数据库应该同时备份，确保一致性。

### 3. 监控磁盘空间

```bash
# 设置磁盘空间告警（示例）
# 当使用率超过 80% 时发送告警
df -h /var/rmm/uploads | awk 'NR==2 {if ($5+0 > 80) print "Warning: Upload directory usage > 80%"}'
```

## 安全建议

### 1. 文件类型限制

建议在代码中添加文件类型白名单验证：

```java
// 允许的文件类型
private static final List<String> ALLOWED_TYPES = Arrays.asList(
    "pdf", "jpg", "jpeg", "png", "doc", "docx"
);

// 验证文件类型
String extension = getFileExtension(filename);
if (!ALLOWED_TYPES.contains(extension.toLowerCase())) {
    throw new BusinessException("不支持的文件类型");
}
```

### 2. 文件大小限制

在 `application.yml` 中配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB      # 单个文件最大 10MB
      max-request-size: 100MB  # 总请求最大 100MB
```

### 3. 病毒扫描

对于生产环境，建议对上传的文件进行病毒扫描。

## 性能优化

### 1. 使用 CDN

对于频繁访问的文件，建议使用 CDN 加速。

### 2. 文件压缩

对于大文件，可以考虑压缩存储：

```bash
# 压缩超过 30 天的文件
find /var/rmm/uploads -type f -mtime +30 -exec gzip {} \;
```

### 3. 定期清理

对于不再使用的文件，定期清理：

```sql
-- 查找没有关联记录的文件
SELECT * FROM stock_in WHERE product_certificate IS NOT NULL
AND product_certificate NOT IN (
    SELECT DISTINCT product_certificate FROM stock_in
);
```
