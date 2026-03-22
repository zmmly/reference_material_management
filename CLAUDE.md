# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供项目指导说明。

## 项目概述

**标准物质管理系统** - 一个用于实验室环境管理标准物质、库存、采购和盘点检查的全栈 Web 应用程序。

**技术栈：**
- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router 5
- 后端：Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT
- 数据库：MySQL 8.0
- API 文档：Knife4j (OpenAPI 3)

## 开发环境说明

本项目支持两个开发环境，数据库配置相同但部署方式不同：

### macOS 环境
- **数据库部署**：Docker 容器
- **容器名称**：mysql-dev
- **数据库连接**：localhost:3306
- **用户名/密码**：root / 123456
- **字符集**：utf8mb4

### Windows 环境
- **数据库部署**：本地安装
- **MySQL 版本**：MySQL 8.0.45
- **数据库连接**：localhost:3306
- **用户名/密码**：root / 123456
- **字符集**：utf8mb4

### 通用配置
**数据库配置文件**：`backend/src/main/resources/application.yml`
- Host: localhost
- Database: `reference_material_management`
- Username: root
- Password: 123456
- ⚠️ 始终使用 `--default-character-set=utf8mb4` 防止中文乱码

## 命令

### 开发启动

```bash
# 启动所有服务（后端 + 前端）- 推荐用于开发
./scripts/start-all.sh start    # 后台启动服务
./scripts/start-all.sh stop     # 停止所有服务
./scripts/start-all.sh restart  # 重启所有服务
./scripts/start-all.sh status   # 查看服务状态
./scripts/start-all.sh logs     # 查看服务日志

# 前端单独启动
cd frontend && npm run dev -- --port 3002

# 后端单独启动
cd backend && mvn spring-boot:run
```

**服务地址：**
- 前端：http://localhost:3002
- 后端：http://localhost:8080
- API 文档：http://localhost:8080/doc.html
- 默认账号：admin / admin123

### 构建打包

```bash
# 前端构建
cd frontend && npm run build

# 后端构建
cd backend && mvn clean package -DskipTests
```

### 测试

```bash
# E2E 测试（需要服务运行）
./scripts/run-tests.sh                  # 运行所有 Playwright 测试
./scripts/run-tests.sh test-login.js    # 运行单个测试

# API 健康检查
./scripts/e2e-test.sh
```

### 数据库管理

#### macOS 环境（Docker）
```bash
# 连接到 MySQL 容器（交互模式）
docker exec -it mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4

# 初始化数据库（必须使用 --default-character-set=utf8mb4 避免中文乱码）
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 < database/schema.sql
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/data.sql

# 检查容器状态
docker ps | grep mysql-dev
```

#### Windows 环境（本地安装）
```bash
# MySQL CLI 工具路径
MySQL Shell: C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe
mysqldump: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe

# 执行 SQL 查询
"/c/Program Files/MySQL/MySQL Shell 8.0/bin/mysqlsh.exe" --sql --uri root:123456@localhost:3306/reference_material_management --execute "SELECT VERSION();"

# 交互模式
"/c/Program Files/MySQL/MySQL Shell 8.0/bin/mysqlsh.exe" --sql --uri root:123456@localhost:3306/reference_material_management

# 导出数据库结构
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe" -h localhost -u root -p123456 --no-data reference_material_management > database/schema.sql

# 导出数据库数据
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe" -h localhost -u root -p123456 --no-create-info --skip-triggers reference_material_management > database/data.sql
```

## 项目结构

### 前端结构

```
frontend/src/
├── api/           # API 请求模块（基于 axios，按领域划分）
├── views/         # 页面组件（按路由组织）
├── components/    # 可复用组件
├── router/        # Vue Router 配置（包含路由守卫）
├── store/         # Pinia 状态管理（modules/user.js）
├── utils/         # 工具函数（auth.js 用于 token 管理）
└── styles/        # 全局样式和主题
```

**关键模式：**
- Token 通过 `@/utils/auth.js` 存储在 localStorage
- 路由守卫检查 token，如果缺失则获取用户信息
- API 模块使用 axios 保持一致性

### 后端结构

```
backend/src/main/java/com/rmm/
├── controller/    # REST API 端点
├── service/       # 业务逻辑层
├── mapper/        # MyBatis-Plus 映射器
├── entity/        # 数据库实体
├── dto/           # 数据传输对象（输入）
├── vo/            # 视图对象（输出）
├── config/        # Spring 配置（Security, CORS, MyBatis-Plus）
├── filter/        # JWT 认证过滤器
├── exception/     # 异常处理
├── common/        # 通用工具类
└── util/          # 工具类（JwtUtil）
```

**API 模式：**
- Controller 处理 HTTP 请求，委托给 Service
- Service 使用 MyBatis-Plus Mapper
- DTO 用于请求体，VO 用于响应
- 通过过滤器链进行 JWT 认证

### 数据库结构

**核心表：**
- `user` / `role` - 用户管理及基于角色的访问控制
- `category` - 标准物质分类（树形结构）
- `location` - 存储位置
- `reference_material` - 标准物质主数据
- `stock` - 当前库存
- `stock_in` / `stock_out` - 入库/出库记录
- `purchase` - 采购管理
- `purchase_acceptance` - 采购验收
- `stock_check` / `stock_check_item` - 盘点检查
- `alert_config` / `alert_record` - 有效期预警
- `supplier` - 供应商信息
- `metadata` - 可配置下拉列表（原因、状态等）
- `operation_log` - 操作日志

**关键关系：**
- `stock.material_id` → `reference_material.id`
- `stock.category_id` → `category.id`（通过 material）
- `stock.location_id` → `location.id`
- `stock_out.stock_id` → `stock.id`（出库记录关联库存）

### 脚本说明

**启动脚本** (`scripts/start-all.sh`)
- `start` - 启动所有服务（后台运行）
- `stop` - 停止所有服务
- `restart` - 重启所有服务
- `status` - 查看服务状态
- `logs` - 查看服务日志

**测试脚本** (`scripts/run-tests.sh`)
- 运行 E2E 测试，基于 Playwright
- 支持运行全部测试或单个测试文件

## 模块说明

| 模块 | 前端页面 | 后端控制器 | 功能描述 |
|------|----------|------------|----------|
| 仪表板 | `views/dashboard/` | `DashboardController` | 统计概览 |
| 基础数据 | `views/basic/` | CategoryController, LocationController, MetadataController | 分类、位置、元数据 |
| 标准物质 | `views/reference-material/` | `ReferenceMaterialController` | 标准物质主数据管理 |
| 库存管理 | `views/stock/` | `StockController` | 库存列表 |
| 入库管理 | `views/stock-in/` | `StockInController` | 入库记录 |
| 出库管理 | `views/stock-out/` | `StockOutController` | 出库记录（含审批流程） |
| 采购管理 | `views/purchase/` | `PurchaseController` | 采购申请和验收 |
| 盘点管理 | `views/stock-check/` | `StockCheckController` | 库存盘点 |
| 预警管理 | `views/alert/` | `AlertController` | 有效期预警 |
| 系统管理 | `views/system/` | UserController, RoleController | 用户和角色管理 |

## E2E 测试

基于 Playwright 的测试位于 `scripts/e2e-tests/`。每个测试文件对应一个模块：
- `test-login.js` - 身份认证
- `test-dashboard.js` - 仪表板统计
- `test-category.js`, `test-location.js` - 基础数据
- `test-stock.js`, `test-stock-in.js`, `test-stock-out.js` - 库存管理
- `test-purchase.js`, `test-stock-check.js` - 采购和盘点
- `test-alert.js` - 预警管理
- `test-user.js` - 用户管理

测试使用 `common.js` 中的共享工具函数。
