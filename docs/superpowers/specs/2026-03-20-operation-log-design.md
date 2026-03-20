# 操作日志功能设计

## 概述

为标准物质管理系统添加操作日志功能，记录所有关键的用户操作，提供独立的日志查询页面供管理员查看。

## 需求

### 功能需求
- 记录所有重要的写操作（POST/PUT/DELETE）
- 提供独立的日志查询页面
- 仅 ADMIN 角色可查看操作日志

### 非功能需求
- 代码侵入性小
- 不影响现有业务逻辑性能
- 日志记录失败不影响业务操作

## 技术方案

采用 **AOP切面方式** 自动拦截Controller方法记录日志。

### 后端实现

#### 1. 实体类

**OperationLog.java**
```java
@TableName("operation_log")
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String module;
    private String target;
    private String detail;
    private String ip;
    private LocalDateTime createTime;
}
```

#### 2. AOP切面

**OperationLogAspect.java**
- 切点：`com.rmm.controller..*.*(..)`
- 通知类型：
  - `@AfterReturning`（方法执行成功后记录）
  - `@AfterThrowing`（登录失败等异常情况记录）
- 拦截规则：
  - 只拦截 `@PostMapping`, `@PutMapping`, `@DeleteMapping` 方法
  - 排除查询方法（方法名以 list/get/page/query/find 开头）
  - 排除下载方法（方法名以 download 开头）

#### 3. 模块名称映射

从 Controller 类名解析模块名称：

| Controller | 模块名称 |
|-----------|---------|
| UserController | 用户管理 |
| RoleController | 角色管理 |
| CategoryController | 分类管理 |
| LocationController | 位置管理 |
| ReferenceMaterialController | 标准物质 |
| StockController | 库存管理 |
| StockInController | 入库管理 |
| StockOutController | 出库管理 |
| StockCheckController | 盘点管理 |
| PurchaseController | 采购管理 |
| SupplierController | 供应商管理 |
| AlertController | 预警管理 |
| MetadataController | 元数据管理 |
| AuthController | 认证管理 |
| BackupController | 系统备份 |
| UploadController | 文件上传 |
| ReportController | 报表管理 |

#### 4. 操作类型映射

从方法名解析操作类型：

| 方法名前缀/包含 | 操作类型 |
|----------------|---------|
| create/add/save/apply/batch | 新增 |
| update/modify/edit/change/reset | 修改 |
| delete/remove | 删除 |
| approve | 审批通过 |
| reject | 审批拒绝 |
| cancel | 取消 |
| login | 登录 |
| upload | 上传 |

#### 5. Controller API

**OperationLogController.java**
- `GET /api/operation-logs/page` - 分页查询日志
  - 参数：page(默认1), size(默认20), startTime, endTime, module, action, username
  - 返回：分页数据
- `GET /api/operation-logs/{id}` - 查询日志详情

### 前端实现

#### 1. 页面位置

系统管理 → 操作日志

#### 2. 路由配置

```javascript
{
  path: '/system/operation-log',
  name: 'OperationLog',
  component: () => import('@/views/system/OperationLog.vue'),
  meta: { title: '操作日志', requiresAuth: true, roles: ['ADMIN'] }
}
```

#### 3. 页面功能

**OperationLog.vue**
- 搜索表单
  - 时间范围（日期选择器）
  - 模块（下拉选择）
  - 操作类型（下拉选择）
  - 操作人（输入框）
  - 查询/重置按钮

- 数据表格
  - 操作时间
  - 操作人
  - 模块
  - 操作类型
  - 操作对象
  - IP地址
  - 操作（查看详情按钮）

- 详情弹窗
  - 显示完整日志信息
  - detail 字段格式化 JSON 显示

#### 4. 菜单项配置

在 `layout/index.vue` 中添加菜单项：

```vue
<!-- 在"系统管理"菜单组中添加 -->
<el-menu-item index="/system/operation-log" v-if="canAccess('system')">
  <el-icon><Document /></el-icon>
  <span>操作日志</span>
</el-menu-item>
```

#### 5. API 模块

**api/operationLog.js**
```javascript
export function getOperationLogs(params) {
  return request.get('/api/operation-logs/page', { params })
}

export function getOperationLogDetail(id) {
  return request.get(`/api/operation-logs/${id}`)
}
```

### 权限控制

- 后端：在 OperationLogController 添加 `@PreAuthorize("hasRole('ADMIN')")`
- 前端：
  - 操作日志属于 'system' 模块，只有 ADMIN 角色有 'system' 权限
  - 在 `layout/index.vue` 的侧边栏菜单中添加操作日志菜单项
  - 菜单项添加条件渲染：`v-if="canAccess('system')"`

## 日志记录规则

### 记录的操作

| 模块 | 操作 | target字段 | detail字段 |
|------|------|-----------|-----------|
| 用户管理 | 创建/修改/删除用户 | 用户名 | 修改的字段和值 |
| 角色管理 | 创建/修改角色 | 角色名称 | 权限变更详情 |
| 分类管理 | 创建/修改/删除分类 | 分类名称 | 完整参数 |
| 位置管理 | 创建/修改/删除位置 | 位置编码 | 完整参数 |
| 标准物质 | 创建/修改/删除 | 物质编号 | 完整参数 |
| 入库管理 | 入库登记 | 入库单号/物质名称 | 入库数量、批次 |
| 出库管理 | 出库申请/审批 | 出库单号/物质名称 | 出库数量、原因 |
| 盘点管理 | 创建/完成盘点 | 盘点批次 | 盘点结果 |
| 采购管理 | 创建/审批采购单 | 采购单号 | 采购详情 |
| 供应商管理 | 创建/修改/删除 | 供应商名称 | 完整参数 |
| 认证管理 | 登录成功 | 用户名 | 登录IP |
| 系统备份 | 创建/删除备份 | 备份文件名 | 备份类型 |
| 文件上传 | 上传文件 | 文件名 | 文件大小、类型 |

### 敏感字段过滤

detail 字段记录请求参数时，需过滤以下敏感字段：
- password
- oldPassword
- newPassword

## 数据库

### 现有表结构（无需修改）

```sql
CREATE TABLE `operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT COMMENT '操作用户ID',
    `username` VARCHAR(50) COMMENT '操作用户名',
    `action` VARCHAR(100) COMMENT '操作类型',
    `module` VARCHAR(50) COMMENT '模块',
    `target` VARCHAR(255) COMMENT '操作对象',
    `detail` TEXT COMMENT '操作详情',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='操作日志表';
```

已有索引：
- `idx_log_user` - user_id 索引
- `idx_log_time` - create_time 索引

建议新增索引：
```sql
-- 优化查询性能的复合索引
CREATE INDEX idx_log_query ON operation_log(module, action, create_time);
```

## 实现要点

### 1. AOP切面实现

```java
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Pointcut("execution(* com.rmm.controller..*.*(..))")
    public void controllerPointcut() {}

    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void recordLog(JoinPoint joinPoint, Object result) {
        // 1. 检查是否是写操作（POST/PUT/DELETE）
        // 2. 排除查询方法（方法名以 list/get/page/query/find/download 开头）
        // 3. 解析模块、操作类型
        // 4. 提取操作对象和详情
        // 5. 获取当前用户和IP
        // 6. 异步保存日志
    }

    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "ex")
    public void recordFailureLog(JoinPoint joinPoint, Exception ex) {
        // 记录登录失败等异常操作
        // 仅针对 AuthController.login 方法
    }
}
```

### 2. 异步保存

使用 `@Async` 异步保存日志，避免影响业务性能：

```java
@Service
public class OperationLogService {

    @Async
    public void saveLog(OperationLog log) {
        try {
            operationLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败不应影响业务，仅记录错误
            log.error("保存操作日志失败", e);
        }
    }
}
```

需要创建异步配置类：

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("log-");
        executor.initialize();
        return executor;
    }
}
```

### 3. 获取当前用户

从 SecurityContext 获取（当前系统中 principal 是用户ID）：

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth != null && auth.getPrincipal() instanceof Long) {
    Long userId = (Long) auth.getPrincipal();
    // 通过 UserService 获取用户名（可使用缓存优化）
    User user = userService.getById(userId);
    String username = user != null ? user.getUsername() : "unknown";
    // 设置到日志对象
    log.setUserId(userId);
    log.setUsername(username);
}
```

### 4. 获取IP地址

创建工具类处理IP提取（支持代理）：

```java
public class IpUtils {

    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

在切面中使用：
```java
@Autowired
private HttpServletRequest request;

String ip = IpUtils.getIpAddress(request);
```

## 后续优化

1. **数据归档** - 定期归档旧日志到历史表
2. **日志导出** - 支持导出Excel格式
3. **日志统计** - 添加操作统计分析功能
4. **实时监控** - 关键操作实时通知管理员

## 测试计划

### 单元测试
- OperationLogService 测试
- 模块名称解析测试
- 操作类型解析测试
- 敏感字段过滤测试

### 集成测试
- AOP切面拦截测试
- 各模块操作记录测试
- 权限控制测试

### E2E测试
- 日志列表查询
- 日志详情查看
- 筛选条件测试
