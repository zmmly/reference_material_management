# 标准物质管理系统 - 服务启动脚本

## 📁 可用脚本

| 脚本 | 说明 |
|------|------|
| `stop-all.bat` | 停止所有服务（前后端）|
| `start-all.bat` | 同时启动所有服务（推荐使用）|
| `run-backend.bat` | 仅启动后端（会先停止旧服务）|
| `run-frontend.bat` | 仅启动前端（会先停止旧服务）|
| `run-backend-bg.bat` | 后台启动后端 |
| `run-frontend-bg.bat` | 前台启动前端 |

## 🚀 快速开始

**推荐使用 `start-all.bat`** - 这个脚本会：
1. 检查并停止任何正在运行的旧服务
2. 同时启动前后端服务
3. 显示服务访问地址

## 📝 服务信息

- **后端端口**: 8080
- **前端端口**: 3002
- **后端地址**: http://localhost:8080
- **前端地址**: http://localhost:3002
- **API文档**: http://localhost:8080/doc.html

## 🔐 默认登录信息

- 用户名: `admin`
- 密码: `admin123`

## 📌 使用说明

### 方式1：启动所有服务（推荐）

```bash
cd scripts
start-all.bat
```

### 方式2：分别启动

```bash
cd scripts
# 启动后端
run-backend.bat

# 启动前端
run-frontend.bat
```

### 方式3：后台启动

```bash
cd scripts
# 后端启动
start cmd /c run-backend-bg.bat

# 前端启动
start cmd /c run-frontend-bg.bat
```

### 停止所有服务

```bash
cd scripts
stop-all.bat
```

## ⚠️ 注意事项

1. 启动脚本会自动检查并停止旧服务，避免端口冲突
2. 如果进程无法正常停止，请手动使用任务管理器终止进程
3. 前端依赖安装可能需要几分钟（首次运行）
4. 后端编译可能需要下载Maven依赖（首次运行）

## 🔧 故障排除

### 端口被占用

如果看到端口被占用错误：

```bash
cd scripts
stop-all.bat
```

### 服务启动失败

1. 检查Java版本是否为17：`java -version`
2. 检查数据库连接配置：`backend/src/main/resources/application.yml`
3. 查看日志输出寻找错误信息

### 编译错误

1. 清理并重新编译：`cd backend && mvn clean package -DskipTests`
2. 检查依赖下载：Maven会自动下载缺失的依赖

### 前端错误

1. 清理node_modules：`rm -rf node_modules`
2. 重新安装：`npm install`
3. 查看浏览器控制台错误

## 📊 监控

### 检查服务状态

```bash
# 检查端口占用
netstat -ano | findstr ":3002\|:8080"

# 查看进程
tasklist | findstr "node.exe\|java.exe"

# 查看服务URL
curl http://localhost:8080/actuator/health
```
