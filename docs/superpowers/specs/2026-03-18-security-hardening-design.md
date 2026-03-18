# 安全加固设计方案

## 概述

为标准物质管理系统实施完整的安全加固，适用于云服务器公网部署场景。

**部署环境**：传统部署（JAR + Nginx），仅 IP 访问，10 人小团队
**数据库安全**：MySQL 已安装，密码复杂度高，不对外网开放

## 模块一：配置安全化

### 目标
将敏感配置从代码中分离，支持环境变量注入，防止敏感信息泄露。

### 改造内容

#### 1.1 后端配置重构

**application.yml 改造**：
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai}
    username: ${DB_USER:rmm_user}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver

jwt:
  secret: ${JWT_SECRET:}
  expiration: 86400000

app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:3001}
```

#### 1.2 新增文件

| 文件 | 用途 | Git 状态 |
|------|------|---------|
| `.env.example` | 环境变量模板 | 提交 |
| `.env` | 实际环境变量 | 忽略 |
| `application-prod.yml` | 生产环境配置 | 提交 |

#### 1.3 .env.example 内容
```env
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
DB_USER=rmm_user
DB_PASSWORD=your_secure_password_here

# JWT配置 (建议使用64位随机字符串)
JWT_SECRET=please_generate_a_random_64_character_secret_key_here

# CORS 允许的前端地址 (逗号分隔)
CORS_ORIGINS=http://localhost:3000,http://YOUR_SERVER_IP
```

#### 1.4 部署命令
```bash
java -jar app.jar \
  -DDB_URL=jdbc:mysql://localhost:3306/rmm \
  -DDB_USER=rmm_user \
  -DDB_PASSWORD=your_secure_password \
  -DJWT_SECRET=your_random_64_char_secret \
  --spring.profiles.active=prod
```

---

## 模块二：服务端 RBAC 权限控制

### 目标
在服务端实施基于角色的访问控制，确保即使前端被绕过也无法越权操作。

### 2.1 角色体系

| 角色代码 | 角色名称 | 说明 |
|---------|---------|------|
| ADMIN | 系统管理员 | 全部权限 |
| MANAGER | 实验室主管 | 审批、查看、部分操作 |
| USER | 普通用户 | 申请、查看 |

### 2.2 权限矩阵

| 功能模块 | ADMIN | MANAGER | USER |
|---------|-------|---------|------|
| 基础数据管理 | 全部 | 只读 | 只读 |
| 入库登记 | ✓ | ✓ | ✓ |
| 出库申请 | ✓ | ✓ | ✓ |
| 出库审批 | ✓ | ✓ | ✗ |
| 采购申请 | ✓ | ✓ | ✓ |
| 采购审批 | ✓ | ✓ | ✗ |
| 盘点操作 | 全部 | 全部 | 只读 |
| 预警处理 | ✓ | ✓ | 只读 |
| 用户管理 | ✓ | ✗ | ✗ |
| 角色管理 | ✓ | ✗ | ✗ |

### 2.3 技术实现

#### 自定义权限注解
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value() default {};
    boolean requireAll() default false;
}
```

#### AOP 切面
```java
@Aspect
@Component
public class RoleAuthAspect {

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        // 1. 从 SecurityContext 获取当前用户
        // 2. 检查用户角色是否满足要求
        // 3. 满足则放行，不满足则抛出 AccessDeniedException
    }
}
```

#### 使用示例
```java
@PostMapping("/approve")
@RequireRole({"ADMIN", "MANAGER"})
public Result<Void> approve(@RequestBody ApproveDTO dto) {
    // 只有 ADMIN 和 MANAGER 可以访问
}
```

---

## 模块三：登录限流 + CORS 修复

### 3.1 登录限流

#### 限流规则
| 限制类型 | 阈值 | 说明 |
|---------|------|------|
| IP 限流 | 5 次/分钟 | 同一 IP 每分钟最多 5 次登录尝试（10人小团队） |
| 用户名限流 | 5 次/分钟 | 同一用户名每分钟最多 5 次失败尝试 |
| 账户锁定 | 15 分钟 | 连续失败 5 次后锁定 15 分钟 |

#### 技术实现
```java
@Component
public class LoginRateLimiter {
    // IP 限流器
    private final Map<String, RateLimiter> ipLimiters = new ConcurrentHashMap<>();
    // 用户名失败计数
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    // 锁定账户
    private final Map<String, Long> lockedAccounts = new ConcurrentHashMap<>();

    public boolean allowLogin(String ip, String username) { ... }
    public void recordFailure(String username) { ... }
    public void recordSuccess(String username) { ... }
}
```

### 3.2 CORS 修复

#### 当前问题
```java
// 危险配置
config.setAllowedOriginPatterns(Arrays.asList("*"));
config.setAllowCredentials(true);
```

#### 修复方案
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 模块四：Token 黑名单 + 安全响应头

### 4.1 Token 黑名单

#### 目标
支持用户主动登出、管理员强制踢人。

#### 实现方案
使用内存缓存（ConcurrentHashMap）存储失效 Token。

#### JWT 改造
在 JWT 中添加 jti (JWT ID) 字段，用于唯一标识 Token。

```java
public String generateToken(Long userId, String username) {
    return Jwts.builder()
        .setId(UUID.randomUUID().toString())  // 添加 jti
        .setSubject(username)
        .claim("userId", userId)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
}

public String getJti(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getId();
}
```

#### 黑名单管理
```java
@Component
public class TokenBlacklist {
    // 存储 jti -> 过期时间
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void addToBlacklist(String jti, long expireTime) {
        blacklist.put(jti, expireTime);
    }

    public boolean isBlacklisted(String jti) {
        Long expireTime = blacklist.get(jti);
        if (expireTime == null) return false;
        if (System.currentTimeMillis() > expireTime) {
            blacklist.remove(jti);  // 清理过期条目
            return false;
        }
        return true;
    }
}
```

#### 新增接口
| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/auth/logout | POST | 登出当前 Token | 登录用户 |
| /api/auth/logout-all | POST | 踢出指定用户所有设备 | ADMIN |

### 4.2 安全响应头

#### Nginx 配置
```nginx
server {
    listen 80;
    server_name YOUR_SERVER_IP;

    # 安全响应头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:;" always;

    # 前端
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 模块五：操作审计日志

### 目标
记录关键操作，便于事后追溯和安全审计。

### 5.1 日志记录范围

| 操作类型 | 触发条件 | 记录内容 |
|---------|---------|---------|
| LOGIN | 登录成功/失败 | 用户、IP、时间、结果 |
| LOGOUT | 登出 | 用户、IP、时间 |
| CREATE | 新增数据 | 操作人、模块、新增数据 |
| UPDATE | 修改数据 | 操作人、模块、变更前后数据 |
| DELETE | 删除数据 | 操作人、模块、被删数据 |
| APPROVE | 审批操作 | 审批人、结果、意见 |

### 5.2 数据库设计

```sql
CREATE TABLE `operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '操作用户名',
    `operation` VARCHAR(20) NOT NULL COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/APPROVE',
    `module` VARCHAR(50) NOT NULL COMMENT '模块: STOCK/PURCHASE/USER/CATEGORY etc.',
    `target_id` VARCHAR(100) COMMENT '操作对象ID',
    `target_name` VARCHAR(200) COMMENT '操作对象名称',
    `old_value` TEXT COMMENT '变更前数据(JSON)',
    `new_value` TEXT COMMENT '变更后数据(JSON)',
    `ip` VARCHAR(50) COMMENT '操作IP',
    `user_agent` VARCHAR(500) COMMENT '浏览器信息',
    `result` TINYINT DEFAULT 1 COMMENT '结果: 1成功 0失败',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_module` (`module`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

### 5.3 技术实现

#### 自定义注解
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();           // 模块名称
    String operation();        // 操作类型
    String description() default "";  // 操作描述
    boolean recordParam() default true;  // 是否记录参数
    boolean recordResult() default true; // 是否记录返回值
}
```

#### AOP 切面
```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogService logService;

    @AfterReturning(pointcut = "@annotation(opLog)", returning = "result")
    public void recordLog(JoinPoint joinPoint, OperationLog opLog, Object result) {
        // 1. 获取当前用户信息
        // 2. 获取请求参数
        // 3. 构建日志对象
        // 4. 异步保存日志
    }

    @AfterThrowing(pointcut = "@annotation(opLog)", throwing = "e")
    public void recordError(JoinPoint joinPoint, OperationLog opLog, Exception e) {
        // 记录失败日志
    }
}
```

### 5.4 日志查询接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/logs | GET | 分页查询日志 | ADMIN |
| /api/logs/export | GET | 导出日志 | ADMIN |

### 5.5 日志清理

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
public void cleanOldLogs() {
    // 删除 90 天前的日志
    logMapper.deleteOldLogs(LocalDateTime.now().minusDays(90));
}
```

---

## 实施计划

### 阶段一：基础设施（预计 1 小时）
1. 创建配置文件和模板
2. 修改 application.yml
3. 更新 .gitignore

### 阶段二：RBAC 权限（预计 1.5 小时）
1. 创建权限注解和切面
2. 修改 Controller 添加权限注解
3. 更新前端权限检查逻辑

### 阶段三：安全防护（预计 1.5 小时）
1. 实现登录限流
2. 修复 CORS 配置
3. 实现 Token 黑名单
4. 添加登出接口

### 阶段四：审计日志（预计 1 小时）
1. 创建日志表和实体
2. 实现日志注解和切面
3. 添加日志查询接口

### 阶段五：部署配置（预计 0.5 小时）
1. 更新 Nginx 配置
2. 编写部署文档
3. 测试验证

---

## 风险与注意事项

1. **向后兼容**：现有 Token 在添加 jti 后仍可使用，但无法被加入黑名单
2. **性能影响**：操作日志采用异步写入，对接口性能影响极小
3. **日志存储**：建议定期清理或归档，避免日志表过大
4. **限流误伤**：实验室可能共用 IP，需监控限流触发情况
