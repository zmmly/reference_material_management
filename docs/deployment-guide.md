# 生产环境部署指南

## 概述

本文档记录标准物质管理系统生产环境部署的安全配置和操作步骤。

**部署环境**：云服务器，公网访问，无 HTTPS（仅 IP 访问）

## 一、云安全组配置

在云控制台配置入站规则：

| 端口 | 协议 | 来源 | 说明 |
|------|------|------|------|
| 80 | TCP | 0.0.0.0/0 | HTTP 访问前端 |
| 22 | TCP | 你的IP | SSH 管理登录 |
| 3306 | TCP | 127.0.0.1 | MySQL 仅本地访问 |
| 8080 | TCP | 127.0.0.1 | 后端仅本地访问 |

**重要**：MySQL 和后端端口禁止外网访问，通过 Nginx 反向代理。

## 二、数据库配置

### 2.1 执行安全补丁

```bash
mysql -u root -p reference_material_management < database/security-patch.sql
```

这会为 `user` 表添加 `password_changed` 字段。

### 2.2 创建应用数据库用户（推荐）

```sql
CREATE USER 'rmm_user'@'localhost' IDENTIFIED BY '复杂密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON reference_material_management.* TO 'rmm_user'@'localhost';
FLUSH PRIVILEGES;
```

## 三、环境变量配置

### 3.1 创建 .env 文件

```bash
cp .env.example .env
vim .env
```

### 3.2 必填配置项

```bash
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
DB_USER=rmm_user
DB_PASSWORD=你的数据库密码

# JWT密钥（使用下面命令生成）
# openssl rand -base64 48
JWT_SECRET=生成的64位随机字符串

# CORS 允许的前端地址（逗号分隔）
CORS_ORIGINS=http://你的服务器公网IP

# Swagger 开关（生产环境关闭）
SWAGGER_ENABLED=false
```

### 3.3 生成 JWT 密钥

```bash
openssl rand -base64 48
```

## 四、启动服务

### 4.1 加载环境变量

```bash
export $(cat .env | xargs)
```

或直接在启动命令中指定：

```bash
java -jar app.jar \
  -DDB_PASSWORD=你的密码 \
  -DJWT_SECRET=你的密钥 \
  -DCORS_ORIGINS=http://你的IP \
  --spring.profiles.active=prod
```

### 4.2 后台运行

```bash
nohup java -jar backend.jar --spring.profiles.active=prod > app.log 2>&1 &
```

## 五、Nginx 配置

### 5.1 配置文件

```nginx
server {
    listen 80;
    server_name YOUR_SERVER_IP;

    # 安全响应头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 5.2 重载配置

```bash
nginx -t && nginx -s reload
```

## 六、安全检查清单

部署完成后，逐项检查：

- [ ] 云安全组已关闭 3306、8080 外网访问
- [ ] `.env` 文件未提交到 Git
- [ ] JWT 密钥已更换为随机值
- [ ] 数据库 root 用户密码已修改
- [ ] admin 用户首次登录已修改密码
- [ ] Swagger API 文档已关闭（访问 /doc.html 应返回 401）
- [ ] Nginx 安全响应头已生效

## 七、已知风险

### 7.1 无 HTTPS（高风险）

**现状**：由于使用 IP 访问，无法配置 SSL 证书

**风险**：密码和 Token 明文传输，可被网络嗅探

**建议**：申请域名后配置 Let's Encrypt 免费证书

### 7.2 其他待实施项

以下功能已在设计文档中规划，暂未实施：

- 登录限流（防暴力破解）
- 服务端 RBAC 权限控制
- Token 黑名单（支持登出）
- 操作审计日志

详见：`docs/superpowers/specs/2026-03-18-security-hardening-design.md`

## 八、常见问题

### Q: 登录后提示需要修改密码

A: 这是首次登录强制改密功能。输入原密码和新密码即可。

### Q: API 文档无法访问

A: 生产环境默认关闭。如需开启，设置 `SWAGGER_ENABLED=true`。

### Q: 前端跨域错误

A: 检查 `CORS_ORIGINS` 是否包含正确的前端地址。

---

**文档版本**：2026-03-18
**对应提交**：0ce4fff
