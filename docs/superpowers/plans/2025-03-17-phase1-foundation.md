# 标准物质管理系统 - 第一阶段实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建基础框架，实现用户权限管理和基础数据管理（分类、位置、元数据配置）

**Architecture:** 采用经典三层架构（Controller-Service-Repository），前后端分离。后端使用 Spring Boot + MyBatis-Plus，前端使用 Vue 3 + Element Plus，数据库使用 MySQL。

**Tech Stack:**
- 后端: Java 17, Spring Boot 3.x, MyBatis-Plus, MySQL 8.0
- 前端: Vue 3, Element Plus, Vue Router, Pinia, Axios
- 构建工具: Maven (后端), Vite (前端)

---

## 文件结构

### 后端项目结构
```
backend/
├── pom.xml
├── src/main/java/com/rmm/
│   ├── RmmApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java        # Spring Security配置
│   │   ├── MyBatisPlusConfig.java     # MyBatis-Plus配置
│   │   └── CorsConfig.java            # 跨域配置
│   ├── controller/
│   │   ├── AuthController.java        # 认证控制器
│   │   ├── UserController.java        # 用户管理
│   │   ├── RoleController.java        # 角色管理
│   │   ├── CategoryController.java    # 分类管理
│   │   ├── LocationController.java    # 位置管理
│   │   └── MetadataController.java    # 元数据配置
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── RoleService.java
│   │   ├── CategoryService.java
│   │   ├── LocationService.java
│   │   └── MetadataService.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── RoleMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── LocationMapper.java
│   │   └── MetadataMapper.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Category.java
│   │   ├── Location.java
│   │   └── Metadata.java
│   ├── dto/
│   │   ├── LoginDTO.java
│   │   ├── UserDTO.java
│   │   └── CommonDTO.java
│   ├── vo/
│   │   ├── LoginVO.java
│   │   ├── UserVO.java
│   │   └── TreeNodeVO.java
│   ├── common/
│   │   ├── Result.java                # 统一响应
│   │   ├── PageResult.java            # 分页响应
│   │   └── BusinessException.java     # 业务异常
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       ├── JwtUtil.java
│       └── SecurityUtil.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── mapper/
        ├── UserMapper.xml
        ├── RoleMapper.xml
        ├── CategoryMapper.xml
        ├── LocationMapper.xml
        └── MetadataMapper.xml
```

### 前端项目结构
```
frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── api/
│   │   ├── auth.js
│   │   ├── user.js
│   │   ├── role.js
│   │   ├── category.js
│   │   ├── location.js
│   │   └── metadata.js
│   ├── views/
│   │   ├── login/
│   │   │   └── index.vue
│   │   ├── layout/
│   │   │   └── index.vue              # 主布局
│   │   ├── dashboard/
│   │   │   └── index.vue              # 首页仪表盘
│   │   ├── system/
│   │   │   ├── user/
│   │   │   │   └── index.vue
│   │   │   └── role/
│   │   │       └── index.vue
│   │   └── basic/
│   │       ├── category/
│   │       │   └── index.vue
│   │       ├── location/
│   │       │   └── index.vue
│   │       └── metadata/
│   │           └── index.vue
│   ├── components/
│   │   └── common/
│   │       ├── Pagination.vue
│   │       └── TreeSelect.vue
│   ├── router/
│   │   └── index.js
│   ├── store/
│   │   ├── index.js
│   │   └── modules/
│   │       └── user.js
│   ├── utils/
│   │   ├── request.js                 # Axios封装
│   │   └── auth.js                    # Token管理
│   └── styles/
│       └── index.scss
└── .env.development
```

### 数据库脚本
```
database/
├── init.sql                           # 建表脚本
├── init-data.sql                      # 初始数据
└── menu.sql                           # 菜单数据
```

---

## Chunk 1: 项目初始化与数据库设计

### Task 1: 创建数据库和基础表结构

**Files:**
- Create: `database/init.sql`

- [ ] **Step 1: 创建数据库初始化脚本**

```sql
-- database/init.sql

-- 创建数据库
CREATE DATABASE IF NOT EXISTS reference_material_management
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE reference_material_management;

-- 用户表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role_id` BIGINT COMMENT '角色ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 1已删除'
) ENGINE=InnoDB COMMENT='用户表';

-- 角色表
CREATE TABLE `role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    `permissions` TEXT COMMENT '权限列表(JSON)',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

-- 分类表(树形结构)
CREATE TABLE `category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='标准物质分类表';

-- 位置表
CREATE TABLE `location` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '位置编码',
    `name` VARCHAR(100) NOT NULL COMMENT '位置名称',
    `temperature` VARCHAR(50) COMMENT '温度要求',
    `capacity` INT COMMENT '容量',
    `description` VARCHAR(255) COMMENT '描述',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='存放位置表';

-- 元数据配置表
CREATE TABLE `metadata` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL COMMENT '类型: STOCK_IN_REASON/STOCK_OUT_REASON/STORAGE_CONDITION',
    `code` VARCHAR(50) NOT NULL COMMENT '编码',
    `name` VARCHAR(100) NOT NULL COMMENT '名称',
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_type_code` (`type`, `code`)
) ENGINE=InnoDB COMMENT='元数据配置表';

-- 操作日志表
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

-- 添加索引
CREATE INDEX `idx_user_role` ON `user`(`role_id`);
CREATE INDEX `idx_category_parent` ON `category`(`parent_id`);
CREATE INDEX `idx_metadata_type` ON `metadata`(`type`);
CREATE INDEX `idx_log_user` ON `operation_log`(`user_id`);
CREATE INDEX `idx_log_time` ON `operation_log`(`create_time`);
```

- [ ] **Step 2: 创建初始数据脚本**

```sql
-- database/init-data.sql

USE reference_material_management;

-- 初始化角色
INSERT INTO `role` (`name`, `code`, `permissions`) VALUES
('系统管理员', 'ADMIN', '["*"]'),
('标准物质管理员', 'MANAGER', '["stock:*","purchase:*","check:*"]'),
('普通用户', 'USER', '["stock:view","stock:out:apply"]');

-- 初始化管理员账号 (密码: admin123，使用BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `role_id`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1, 1);

-- 初始化分类
INSERT INTO `category` (`name`, `parent_id`, `sort_order`) VALUES
('对照品', 0, 1),
('对照药材', 0, 2),
('标准溶液', 0, 3),
('滴定液', 0, 4),
('化妆品标准物质', 0, 5);

-- 初始化元数据
INSERT INTO `metadata` (`type`, `code`, `name`, `sort_order`) VALUES
-- 入库原因
('STOCK_IN_REASON', 'PURCHASE', '新购入', 1),
('STOCK_IN_REASON', 'SURPLUS', '盘盈', 2),
('STOCK_IN_REASON', 'RETURN', '归还', 3),
('STOCK_IN_REASON', 'TRANSFER_IN', '调拨入', 4),
('STOCK_IN_REASON', 'OTHER', '其他', 5),
-- 出库原因
('STOCK_OUT_REASON', 'EXPERIMENT', '实验使用', 1),
('STOCK_OUT_REASON', 'EXPIRED', '过期销毁', 2),
('STOCK_OUT_REASON', 'SCRAP', '报废', 3),
('STOCK_OUT_REASON', 'TRANSFER_OUT', '调拨出', 4),
('STOCK_OUT_REASON', 'DONATE', '赠送', 5),
('STOCK_OUT_REASON', 'OTHER', '其他', 6),
-- 储存条件
('STORAGE_CONDITION', 'MINUS_20', '-20℃', 1),
('STORAGE_CONDITION', 'COLD_2_8', '2-8℃', 2),
('STORAGE_CONDITION', 'ROOM', '常温', 3),
('STORAGE_CONDITION', 'COOL_DRY', '阴凉干燥', 4),
('STORAGE_CONDITION', 'DARK', '避光', 5),
('STORAGE_CONDITION', 'ROOM_10_30', '10-30℃', 6);

-- 初始化位置
INSERT INTO `location` (`code`, `name`, `temperature`, `capacity`) VALUES
('LOC001', '冰箱A (-20℃)', '-20℃', 100),
('LOC002', '冰箱B (2-8℃)', '2-8℃', 150),
('LOC003', '常温柜A', '常温', 200),
('LOC004', '阴凉柜A', '阴凉干燥', 100);
```

- [ ] **Step 3: 验证SQL脚本**

Run: 在MySQL中执行脚本
```bash
mysql -u root -p < database/init.sql
mysql -u root -p < database/init-data.sql
```
Expected: 所有表创建成功，初始数据插入成功

- [ ] **Step 4: Commit**

```bash
git add database/
git commit -m "feat(db): add database schema and initial data"
```

---

### Task 2: 创建后端项目骨架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/rmm/RmmApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建Maven项目配置**

```xml
<!-- backend/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.rmm</groupId>
    <artifactId>reference-material-management</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>Reference Material Management</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jjwt.version>0.12.3</jjwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建应用入口类**

```java
// backend/src/main/java/com/rmm/RmmApplication.java
package com.rmm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RmmApplication {
    public static void main(String[] args) {
        SpringApplication.run(RmmApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建配置文件**

```yaml
# backend/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: reference-material-management
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.rmm.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: YourSecretKeyForJwtTokenGenerationMustBeLongEnough123456
  expiration: 86400000
```

- [ ] **Step 4: 创建开发环境配置**

```yaml
# backend/src/main/resources/application-dev.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/reference_material_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

logging:
  level:
    com.rmm: DEBUG
```

- [ ] **Step 5: 验证项目启动**

Run:
```bash
cd backend && mvn spring-boot:run
```
Expected: 项目启动成功，访问 http://localhost:8080 返回401(安全控制生效)

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat(backend): init Spring Boot project skeleton"
```

---

### Task 3: 创建通用基础类

**Files:**
- Create: `backend/src/main/java/com/rmm/common/Result.java`
- Create: `backend/src/main/java/com/rmm/common/PageResult.java`
- Create: `backend/src/main/java/com/rmm/common/BusinessException.java`
- Create: `backend/src/main/java/com/rmm/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建统一响应类**

```java
// backend/src/main/java/com/rmm/common/Result.java
package com.rmm.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

- [ ] **Step 2: 创建分页响应类**

```java
// backend/src/main/java/com/rmm/common/PageResult.java
package com.rmm.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }
}
```

- [ ] **Step 3: 创建业务异常类**

```java
// backend/src/main/java/com/rmm/common/BusinessException.java
package com.rmm.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 4: 创建全局异常处理器**

```java
// backend/src/main/java/com/rmm/exception/GlobalExceptionHandler.java
package com.rmm.exception;

import com.rmm.common.BusinessException;
import com.rmm.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统异常，请稍后重试");
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/rmm/common/ backend/src/main/java/com/rmm/exception/
git commit -m "feat(common): add Result, PageResult and exception handlers"
```

---

## Chunk 2: 认证授权模块

### Task 4: 实现JWT工具类和安全配置

**Files:**
- Create: `backend/src/main/java/com/rmm/util/JwtUtil.java`
- Create: `backend/src/main/java/com/rmm/util/SecurityUtil.java`
- Create: `backend/src/main/java/com/rmm/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/rmm/config/CorsConfig.java`

- [ ] **Step 1: 创建JWT工具类**

```java
// backend/src/main/java/com/rmm/util/JwtUtil.java
package com.rmm.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
```

- [ ] **Step 2: 创建安全工具类**

```java
// backend/src/main/java/com/rmm/util/SecurityUtil.java
package com.rmm.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }
}
```

- [ ] **Step 3: 创建Security配置**

```java
// backend/src/main/java/com/rmm/config/SecurityConfig.java
package com.rmm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

- [ ] **Step 4: 创建CORS配置**

```java
// backend/src/main/java/com/rmm/config/CorsConfig.java
package com.rmm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/rmm/util/ backend/src/main/java/com/rmm/config/
git commit -m "feat(security): add JWT util and security config"
```

---

### Task 5: 实现实体类和数据访问层

**Files:**
- Create: `backend/src/main/java/com/rmm/entity/User.java`
- Create: `backend/src/main/java/com/rmm/entity/Role.java`
- Create: `backend/src/main/java/com/rmm/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/rmm/mapper/RoleMapper.java`

- [ ] **Step 1: 创建User实体**

```java
// backend/src/main/java/com/rmm/entity/User.java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long roleId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Role role;
}
```

- [ ] **Step 2: 创建Role实体**

```java
// backend/src/main/java/com/rmm/entity/Role.java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String permissions;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建Mapper接口**

```java
// backend/src/main/java/com/rmm/mapper/UserMapper.java
package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

```java
// backend/src/main/java/com/rmm/mapper/RoleMapper.java
package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
```

- [ ] **Step 4: 创建MyBatis-Plus配置**

```java
// backend/src/main/java/com/rmm/config/MyBatisPlusConfig.java
package com.rmm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/rmm/entity/ backend/src/main/java/com/rmm/mapper/ backend/src/main/java/com/rmm/config/MyBatisPlusConfig.java
git commit -m "feat(entity): add User, Role entities and mappers"
```

---

### Task 6: 实现认证服务

**Files:**
- Create: `backend/src/main/java/com/rmm/dto/LoginDTO.java`
- Create: `backend/src/main/java/com/rmm/vo/LoginVO.java`
- Create: `backend/src/main/java/com/rmm/vo/UserVO.java`
- Create: `backend/src/main/java/com/rmm/service/AuthService.java`
- Create: `backend/src/main/java/com/rmm/controller/AuthController.java`

- [ ] **Step 1: 创建登录DTO**

```java
// backend/src/main/java/com/rmm/dto/LoginDTO.java
package com.rmm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建响应VO**

```java
// backend/src/main/java/com/rmm/vo/LoginVO.java
package com.rmm.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO user;
}
```

```java
// backend/src/main/java/com/rmm/vo/UserVO.java
package com.rmm.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long roleId;
    private String roleName;
    private String roleCode;
}
```

- [ ] **Step 3: 创建认证服务**

```java
// backend/src/main/java/com/rmm/service/AuthService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.BusinessException;
import com.rmm.dto.LoginDTO;
import com.rmm.entity.Role;
import com.rmm.entity.User;
import com.rmm.mapper.RoleMapper;
import com.rmm.mapper.UserMapper;
import com.rmm.util.JwtUtil;
import com.rmm.vo.LoginVO;
import com.rmm.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginDTO dto) {
        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 查询角色
        Role role = roleMapper.selectById(user.getRoleId());

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setToken(token);

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setRoleId(user.getRoleId());
        if (role != null) {
            userVO.setRoleName(role.getName());
            userVO.setRoleCode(role.getCode());
        }
        vo.setUser(userVO);

        return vo;
    }
}
```

- [ ] **Step 4: 创建认证控制器**

```java
// backend/src/main/java/com/rmm/controller/AuthController.java
package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.dto.LoginDTO;
import com.rmm.service.AuthService;
import com.rmm.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }
}
```

- [ ] **Step 5: 测试登录接口**

Run:
```bash
# 启动项目后测试
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
Expected: 返回token和用户信息

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/rmm/dto/ backend/src/main/java/com/rmm/vo/ backend/src/main/java/com/rmm/service/AuthService.java backend/src/main/java/com/rmm/controller/AuthController.java
git commit -m "feat(auth): implement login with JWT"
```

---

## Chunk 3: 用户角色管理模块

### Task 7: 实现用户管理

**Files:**
- Create: `backend/src/main/java/com/rmm/service/UserService.java`
- Create: `backend/src/main/java/com/rmm/controller/UserController.java`

- [ ] **Step 1: 创建用户服务**

```java
// backend/src/main/java/com/rmm/service/UserService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.dto.UserDTO;
import com.rmm.entity.Role;
import com.rmm.entity.User;
import com.rmm.mapper.RoleMapper;
import com.rmm.mapper.UserMapper;
import com.rmm.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> list(Integer current, Integer size, String username, Long roleId, Integer status) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), User::getUsername, username)
               .eq(roleId != null, User::getRoleId, roleId)
               .eq(status != null, User::getStatus, status)
               .orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(page, wrapper);

        PageResult<UserVO> pageResult = new PageResult<>();
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        pageResult.setRecords(result.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setRoleId(user.getRoleId());
            if (user.getRoleId() != null) {
                Role role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    vo.setRoleName(role.getName());
                    vo.setRoleCode(role.getCode());
                }
            }
            return vo;
        }).toList());

        return pageResult;
    }

    public void create(UserDTO dto) {
        // 检查用户名是否存在
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setStatus(1);

        userMapper.insert(user);
    }

    public void update(Long id, UserDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());

        userMapper.updateById(user);
    }

    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }
}
```

- [ ] **Step 2: 创建用户控制器**

```java
// backend/src/main/java/com/rmm/controller/UserController.java
package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.dto.UserDTO;
import com.rmm.service.UserService;
import com.rmm.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.list(current, size, username, roleId, status));
    }

    @PostMapping
    public Result<Void> create(@RequestBody UserDTO dto) {
        userService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        userService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/rmm/service/UserService.java backend/src/main/java/com/rmm/controller/UserController.java
git commit -m "feat(user): implement user management CRUD"
```

---

### Task 8: 实现角色管理

**Files:**
- Create: `backend/src/main/java/com/rmm/dto/RoleDTO.java`
- Create: `backend/src/main/java/com/rmm/vo/RoleVO.java`
- Create: `backend/src/main/java/com/rmm/service/RoleService.java`
- Create: `backend/src/main/java/com/rmm/controller/RoleController.java`

- [ ] **Step 1: 创建DTO和VO**

```java
// backend/src/main/java/com/rmm/dto/RoleDTO.java
package com.rmm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleDTO {
    @NotBlank(message = "角色名称不能为空")
    private String name;
    private String code;
    private String permissions;
}
```

```java
// backend/src/main/java/com/rmm/vo/RoleVO.java
package com.rmm.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoleVO {
    private Long id;
    private String name;
    private String code;
    private String permissions;
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建角色服务**

```java
// backend/src/main/java/com/rmm/service/RoleService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.dto.RoleDTO;
import com.rmm.entity.Role;
import com.rmm.mapper.RoleMapper;
import com.rmm.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;

    public List<RoleVO> listAll() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<Role>().eq(Role::getStatus, 1)
        ).stream().map(this::toVO).toList();
    }

    public PageResult<RoleVO> list(Integer current, Integer size, String name) {
        Page<Role> page = new Page<>(current, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Role::getName, name)
               .orderByDesc(Role::getCreateTime);

        Page<Role> result = roleMapper.selectPage(page, wrapper);
        PageResult<RoleVO> pageResult = new PageResult<>();
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        pageResult.setRecords(result.getRecords().stream().map(this::toVO).toList());

        return pageResult;
    }

    public void create(RoleDTO dto) {
        if (roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, dto.getCode())) > 0) {
            throw new BusinessException("角色编码已存在");
        }

        Role role = new Role();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setPermissions(dto.getPermissions());
        role.setStatus(1);
        roleMapper.insert(role);
    }

    public void update(Long id, RoleDTO dto) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setName(dto.getName());
        role.setPermissions(dto.getPermissions());
        roleMapper.updateById(role);
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setPermissions(role.getPermissions());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
```

- [ ] **Step 3: 创建角色控制器**

```java
// backend/src/main/java/com/rmm/controller/RoleController.java
package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.dto.RoleDTO;
import com.rmm.service.RoleService;
import com.rmm.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/all")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }

    @GetMapping
    public Result<PageResult<RoleVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        return Result.success(roleService.list(current, size, name));
    }

    @PostMapping
    public Result<Void> create(@RequestBody RoleDTO dto) {
        roleService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        roleService.update(id, dto);
        return Result.success();
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/rmm/dto/RoleDTO.java backend/src/main/java/com/rmm/vo/RoleVO.java backend/src/main/java/com/rmm/service/RoleService.java backend/src/main/java/com/rmm/controller/RoleController.java
git commit -m "feat(role): implement role management"
```

---

## Chunk 4: 基础数据管理模块

### Task 9: 实现分类管理

**Files:**
- Create: `backend/src/main/java/com/rmm/entity/Category.java`
- Create: `backend/src/main/java/com/rmm/mapper/CategoryMapper.java`
- Create: `backend/src/main/java/com/rmm/service/CategoryService.java`
- Create: `backend/src/main/java/com/rmm/controller/CategoryController.java`

- [ ] **Step 1: 创建实体和Mapper**

```java
// backend/src/main/java/com/rmm/entity/Category.java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Category> children;
}
```

```java
// backend/src/main/java/com/rmm/mapper/CategoryMapper.java
package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
```

- [ ] **Step 2: 创建分类服务**

```java
// backend/src/main/java/com/rmm/service/CategoryService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.BusinessException;
import com.rmm.entity.Category;
import com.rmm.mapper.CategoryMapper;
import com.rmm.vo.TreeNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<TreeNodeVO> tree() {
        List<Category> all = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder)
        );
        return buildTree(all, 0L);
    }

    private List<TreeNodeVO> buildTree(List<Category> all, Long parentId) {
        return all.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .map(c -> {
                    TreeNodeVO node = new TreeNodeVO();
                    node.setId(c.getId());
                    node.setLabel(c.getName());
                    node.setChildren(buildTree(all, c.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    public void create(Category category) {
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    public void update(Category category) {
        if (category.getId().equals(category.getParentId())) {
            throw new BusinessException("父级不能是自己");
        }
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        Long childCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException("存在子分类，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
```

- [ ] **Step 3: 创建TreeNodeVO**

```java
// backend/src/main/java/com/rmm/vo/TreeNodeVO.java
package com.rmm.vo;

import lombok.Data;
import java.util.List;

@Data
public class TreeNodeVO {
    private Long id;
    private String label;
    private List<TreeNodeVO> children;
}
```

- [ ] **Step 4: 创建分类控制器**

```java
// backend/src/main/java/com/rmm/controller/CategoryController.java
package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.entity.Category;
import com.rmm.service.CategoryService;
import com.rmm.vo.TreeNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<TreeNodeVO>> tree() {
        return Result.success(categoryService.tree());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Category category) {
        categoryService.create(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/rmm/entity/Category.java backend/src/main/java/com/rmm/mapper/CategoryMapper.java backend/src/main/java/com/rmm/vo/TreeNodeVO.java backend/src/main/java/com/rmm/service/CategoryService.java backend/src/main/java/com/rmm/controller/CategoryController.java
git commit -m "feat(category): implement category management with tree structure"
```

---

### Task 10: 实现位置管理

**Files:**
- Create: `backend/src/main/java/com/rmm/entity/Location.java`
- Create: `backend/src/main/java/com/rmm/mapper/LocationMapper.java`
- Create: `backend/src/main/java/com/rmm/service/LocationService.java`
- Create: `backend/src/main/java/com/rmm/controller/LocationController.java`

- [ ] **Step 1: 创建实体和服务**

```java
// backend/src/main/java/com/rmm/entity/Location.java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("location")
public class Location {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String temperature;
    private Integer capacity;
    private String description;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

```java
// backend/src/main/java/com/rmm/mapper/LocationMapper.java
package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Location;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {
}
```

```java
// backend/src/main/java/com/rmm/service/LocationService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rmm.common.BusinessException;
import com.rmm.common.PageResult;
import com.rmm.entity.Location;
import com.rmm.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;

    public PageResult<Location> list(Integer current, Integer size, String name, String temperature) {
        Page<Location> page = new Page<>(current, size);
        LambdaQueryWrapper<Location> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Location::getName, name)
               .eq(StringUtils.hasText(temperature), Location::getTemperature, temperature)
               .orderByDesc(Location::getCreateTime);

        Page<Location> result = locationMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    public List<Location> listAll() {
        return locationMapper.selectList(
            new LambdaQueryWrapper<Location>().eq(Location::getStatus, 1)
        );
    }

    public void create(Location location) {
        if (locationMapper.selectCount(new LambdaQueryWrapper<Location>()
                .eq(Location::getCode, location.getCode())) > 0) {
            throw new BusinessException("位置编码已存在");
        }
        location.setStatus(1);
        locationMapper.insert(location);
    }

    public void update(Location location) {
        locationMapper.updateById(location);
    }
}
```

- [ ] **Step 2: 创建位置控制器**

```java
// backend/src/main/java/com/rmm/controller/LocationController.java
package com.rmm.controller;

import com.rmm.common.PageResult;
import com.rmm.common.Result;
import com.rmm.entity.Location;
import com.rmm.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public Result<PageResult<Location>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String temperature) {
        return Result.success(locationService.list(current, size, name, temperature));
    }

    @GetMapping("/all")
    public Result<List<Location>> listAll() {
        return Result.success(locationService.listAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Location location) {
        locationService.create(location);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Location location) {
        location.setId(id);
        locationService.update(location);
        return Result.success();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/rmm/entity/Location.java backend/src/main/java/com/rmm/mapper/LocationMapper.java backend/src/main/java/com/rmm/service/LocationService.java backend/src/main/java/com/rmm/controller/LocationController.java
git commit -m "feat(location): implement location management"
```

---

### Task 11: 实现元数据配置

**Files:**
- Create: `backend/src/main/java/com/rmm/entity/Metadata.java`
- Create: `backend/src/main/java/com/rmm/mapper/MetadataMapper.java`
- Create: `backend/src/main/java/com/rmm/service/MetadataService.java`
- Create: `backend/src/main/java/com/rmm/controller/MetadataController.java`

- [ ] **Step 1: 创建实体和服务**

```java
// backend/src/main/java/com/rmm/entity/Metadata.java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("metadata")
public class Metadata {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String code;
    private String name;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

```java
// backend/src/main/java/com/rmm/mapper/MetadataMapper.java
package com.rmm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rmm.entity.Metadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetadataMapper extends BaseMapper<Metadata> {
}
```

```java
// backend/src/main/java/com/rmm/service/MetadataService.java
package com.rmm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rmm.common.BusinessException;
import com.rmm.entity.Metadata;
import com.rmm.mapper.MetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final MetadataMapper metadataMapper;

    public List<Metadata> listByType(String type) {
        return metadataMapper.selectList(
            new LambdaQueryWrapper<Metadata>()
                .eq(Metadata::getType, type)
                .eq(Metadata::getStatus, 1)
                .orderByAsc(Metadata::getSortOrder)
        );
    }

    public void create(Metadata metadata) {
        Long count = metadataMapper.selectCount(
            new LambdaQueryWrapper<Metadata>()
                .eq(Metadata::getType, metadata.getType())
                .eq(Metadata::getCode, metadata.getCode())
        );
        if (count > 0) {
            throw new BusinessException("该类型下编码已存在");
        }
        metadata.setStatus(1);
        metadataMapper.insert(metadata);
    }

    public void update(Metadata metadata) {
        metadataMapper.updateById(metadata);
    }
}
```

- [ ] **Step 2: 创建元数据控制器**

```java
// backend/src/main/java/com/rmm/controller/MetadataController.java
package com.rmm.controller;

import com.rmm.common.Result;
import com.rmm.entity.Metadata;
import com.rmm.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping("/{type}")
    public Result<List<Metadata>> listByType(@PathVariable String type) {
        return Result.success(metadataService.listByType(type));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Metadata metadata) {
        metadataService.create(metadata);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Metadata metadata) {
        metadata.setId(id);
        metadataService.update(metadata);
        return Result.success();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/rmm/entity/Metadata.java backend/src/main/java/com/rmm/mapper/MetadataMapper.java backend/src/main/java/com/rmm/service/MetadataService.java backend/src/main/java/com/rmm/controller/MetadataController.java
git commit -m "feat(metadata): implement metadata configuration management"
```

---

## Chunk 5: 前端项目初始化

### Task 12: 创建前端项目骨架

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: 初始化Vue项目**

```bash
cd /Users/leyang/Documents/工作/AI项目/标准物质管理
npm create vite@latest frontend -- --template vue
cd frontend
npm install element-plus @element-plus/icons-vue vue-router pinia axios sass
```

- [ ] **Step 2: 配置package.json依赖**

```json
// frontend/package.json
{
  "name": "rmm-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "element-plus": "^2.5.0",
    "@element-plus/icons-vue": "^2.3.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0",
    "sass": "^1.69.0"
  }
}
```

- [ ] **Step 3: 配置vite**

```javascript
// frontend/vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 4: 创建入口文件**

```html
<!-- frontend/index.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>标准物质管理系统</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

```javascript
// frontend/src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './styles/index.scss'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
```

```vue
<!-- frontend/src/App.vue -->
<template>
  <router-view />
</template>

<script setup>
</script>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/
git commit -m "feat(frontend): init Vue3 project with Element Plus"
```

---

### Task 13: 创建API和路由

**Files:**
- Create: `frontend/src/utils/request.js`
- Create: `frontend/src/utils/auth.js`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/store/modules/user.js`

- [ ] **Step 1: 创建Axios封装**

```javascript
// frontend/src/utils/request.js
import axios from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        removeToken()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

```javascript
// frontend/src/utils/auth.js
const TOKEN_KEY = 'rmm_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
```

- [ ] **Step 2: 创建认证API**

```javascript
// frontend/src/api/auth.js
import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}
```

- [ ] **Step 3: 创建路由配置**

```javascript
// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue') },
      { path: 'system/user', name: 'User', component: () => import('@/views/system/user/index.vue') },
      { path: 'system/role', name: 'Role', component: () => import('@/views/system/role/index.vue') },
      { path: 'basic/category', name: 'Category', component: () => import('@/views/basic/category/index.vue') },
      { path: 'basic/location', name: 'Location', component: () => import('@/views/basic/location/index.vue') },
      { path: 'basic/metadata', name: 'Metadata', component: () => import('@/views/basic/metadata/index.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 4: 创建用户Store**

```javascript
// frontend/src/store/modules/user.js
import { defineStore } from 'pinia'
import { login } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null
  }),

  actions: {
    async login(loginForm) {
      const res = await login(loginForm)
      this.token = res.data.token
      this.userInfo = res.data.user
      setToken(res.data.token)
      return res
    },

    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/utils/ frontend/src/api/ frontend/src/router/ frontend/src/store/
git commit -m "feat(frontend): add request utils, router and user store"
```

---

### Task 14: 创建登录页面

**Files:**
- Create: `frontend/src/views/login/index.vue`

- [ ] **Step 1: 创建登录页面**

```vue
<!-- frontend/src/views/login/index.vue -->
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>标准物质管理系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
  h2 { text-align: center; margin-bottom: 30px; color: #333; }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/login/
git commit -m "feat(frontend): add login page"
```

---

### Task 15: 创建主布局和菜单

**Files:**
- Create: `frontend/src/views/layout/index.vue`
- Create: `frontend/src/views/dashboard/index.vue`

- [ ] **Step 1: 创建主布局**

```vue
<!-- frontend/src/views/layout/index.vue -->
<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">{{ isCollapse ? 'RMM' : '标准物质管理系统' }}</div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="basic">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/basic/category">分类管理</el-menu-item>
          <el-menu-item index="/basic/location">位置管理</el-menu-item>
          <el-menu-item index="/basic/metadata">元数据配置</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <component :is="isCollapse ? 'Expand' : 'Fold'" />
        </el-icon>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout-container { height: 100vh; }
.aside {
  background-color: #304156;
  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid #3a4a5e;
  }
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}
.collapse-btn { cursor: pointer; font-size: 20px; }
.user-info { cursor: pointer; display: flex; align-items: center; gap: 5px; }
.main { background: #f0f2f5; }
</style>
```

- [ ] **Step 2: 创建首页仪表盘占位**

```vue
<!-- frontend/src/views/dashboard/index.vue -->
<template>
  <div class="dashboard">
    <h1>欢迎使用标准物质管理系统</h1>
    <p>当前为第一阶段，首页功能将在第四阶段完善</p>
  </div>
</template>

<style scoped>
.dashboard { padding: 20px; }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/layout/ frontend/src/views/dashboard/
git commit -m "feat(frontend): add main layout and dashboard placeholder"
```

---

## Chunk 6: 前端管理页面

### Task 16: 创建用户管理页面

**Files:**
- Create: `frontend/src/api/user.js`
- Create: `frontend/src/api/role.js`
- Create: `frontend/src/views/system/user/index.vue`

- [ ] **Step 1: 创建用户API**

```javascript
// frontend/src/api/user.js
import request from '@/utils/request'

export function getUserList(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function resetPassword(id) {
  return request.put(`/users/${id}/reset-password`)
}
```

```javascript
// frontend/src/api/role.js
import request from '@/utils/request'

export function getRoleList() {
  return request.get('/roles/all')
}
```

- [ ] **Step 2: 创建用户管理页面**

```vue
<!-- frontend/src/views/system/user/index.vue -->
<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="roleName" label="角色" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in roleList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, updateUserStatus, resetPassword } from '@/api/user'
import { getRoleList } from '@/api/role'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const roleList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, username: '', status: null })
const form = reactive({ username: '', realName: '', phone: '', email: '', roleId: null })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  const res = await getRoleList()
  roleList.value = res.data
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { username: '', realName: '', phone: '', email: '', roleId: null })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateUser(editId.value, form)
  } else {
    await createUser(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

const handleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${newStatus === 1 ? '启用' : '禁用'}该用户？`)
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  fetchData()
}

const handleResetPwd = async (row) => {
  await ElMessageBox.confirm('确定重置该用户密码为123456？')
  await resetPassword(row.id)
  ElMessage.success('密码已重置为123456')
}

onMounted(() => {
  fetchData()
  fetchRoles()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/user.js frontend/src/api/role.js frontend/src/views/system/user/
git commit -m "feat(frontend): add user management page"
```

---

### Task 17: 创建角色管理页面

**Files:**
- Create: `frontend/src/views/system/role/index.vue`

- [ ] **Step 1: 创建角色管理页面**

```vue
<!-- frontend/src/views/system/role/index.vue -->
<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑角色' : '新增角色'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, name: '' })
const form = reactive({ name: '', code: '' })
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  // TODO: 调用API
  loading.value = false
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { name: '', code: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/system/role/
git commit -m "feat(frontend): add role management page"
```

---

### Task 18: 创建分类管理页面

**Files:**
- Create: `frontend/src/api/category.js`
- Create: `frontend/src/views/basic/category/index.vue`

- [ ] **Step 1: 创建分类API**

```javascript
// frontend/src/api/category.js
import request from '@/utils/request'

export function getCategoryTree() {
  return request.get('/categories/tree')
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
```

- [ ] **Step 2: 创建分类管理页面**

```vue
<!-- frontend/src/views/basic/category/index.vue -->
<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-button type="primary" @click="handleAdd()">新增顶级分类</el-button>
      </template>

      <el-table :data="tableData" v-loading="loading" row-key="id" border default-expand-all>
        <el-table-column prop="label" label="分类名称" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAdd(row)">添加子级</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="parentId ? '添加子分类' : (editId ? '编辑分类' : '新增分类')" width="400">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const parentId = ref(null)
const formRef = ref()

const form = reactive({ name: '' })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryTree()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = (row) => {
  editId.value = null
  parentId.value = row?.id || 0
  form.name = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  parentId.value = null
  form.name = row.label
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateCategory(editId.value, { name: form.name })
  } else {
    await createCategory({ name: form.name, parentId: parentId.value })
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该分类？')
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/category.js frontend/src/views/basic/category/
git commit -m "feat(frontend): add category management page"
```

---

### Task 19: 创建位置管理页面

**Files:**
- Create: `frontend/src/api/location.js`
- Create: `frontend/src/views/basic/location/index.vue`

- [ ] **Step 1: 创建位置API和页面**

```javascript
// frontend/src/api/location.js
import request from '@/utils/request'

export function getLocationList(params) {
  return request.get('/locations', { params })
}

export function getAllLocations() {
  return request.get('/locations/all')
}

export function createLocation(data) {
  return request.post('/locations', data)
}

export function updateLocation(id, data) {
  return request.put(`/locations/${id}`, data)
}
```

```vue
<!-- frontend/src/views/basic/location/index.vue -->
<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="位置名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="位置编码" width="120" />
        <el-table-column prop="name" label="位置名称" />
        <el-table-column prop="temperature" label="温度要求" width="100" />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑位置' : '新增位置'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="位置编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="位置名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="温度要求">
          <el-select v-model="form.temperature" placeholder="请选择" style="width: 100%">
            <el-option label="-20℃" value="-20℃" />
            <el-option label="2-8℃" value="2-8℃" />
            <el-option label="常温" value="常温" />
            <el-option label="阴凉干燥" value="阴凉干燥" />
            <el-option label="10-30℃" value="10-30℃" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLocationList, createLocation, updateLocation } from '@/api/location'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, name: '' })
const form = reactive({ code: '', name: '', temperature: '', capacity: null, description: '' })
const rules = {
  code: [{ required: true, message: '请输入位置编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入位置名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLocationList(queryParams)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { code: '', name: '', temperature: '', capacity: null, description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateLocation(editId.value, form)
  } else {
    await createLocation(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/location.js frontend/src/views/basic/location/
git commit -m "feat(frontend): add location management page"
```

---

### Task 20: 创建元数据配置页面

**Files:**
- Create: `frontend/src/api/metadata.js`
- Create: `frontend/src/views/basic/metadata/index.vue`

- [ ] **Step 1: 创建元数据API和页面**

```javascript
// frontend/src/api/metadata.js
import request from '@/utils/request'

export function getMetadataByType(type) {
  return request.get(`/metadata/${type}`)
}

export function createMetadata(data) {
  return request.post('/metadata', data)
}

export function updateMetadata(id, data) {
  return request.put(`/metadata/${id}`, data)
}
```

```vue
<!-- frontend/src/views/basic/metadata/index.vue -->
<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeType" @tab-change="fetchData">
        <el-tab-pane label="入库原因" name="STOCK_IN_REASON" />
        <el-tab-pane label="出库原因" name="STOCK_OUT_REASON" />
        <el-tab-pane label="储存条件" name="STORAGE_CONDITION" />
      </el-tabs>

      <el-button type="primary" style="margin-bottom: 16px" @click="handleAdd">新增</el-button>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑' : '新增'" width="400">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="60px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getMetadataByType, createMetadata, updateMetadata } from '@/api/metadata'

const activeType = ref('STOCK_IN_REASON')
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const form = reactive({ code: '', name: '', sortOrder: 1 })
const rules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMetadataByType(activeType.value)
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { code: '', name: '', sortOrder: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  const data = { ...form, type: activeType.value }
  if (editId.value) {
    await updateMetadata(editId.value, data)
  } else {
    await createMetadata(data)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

fetchData()
</script>

<style scoped>.page-container { padding: 20px; }</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/metadata.js frontend/src/views/basic/metadata/
git commit -m "feat(frontend): add metadata configuration page"
```

---

## 阶段总结

第一阶段完成后的项目结构：

```
标准物质管理/
├── backend/                    # Spring Boot后端
│   ├── pom.xml
│   └── src/main/java/com/rmm/
│       ├── config/            # 配置类
│       ├── controller/        # 控制器
│       ├── service/           # 服务层
│       ├── mapper/            # 数据访问层
│       ├── entity/            # 实体类
│       ├── dto/               # 数据传输对象
│       ├── vo/                # 视图对象
│       └── common/            # 通用类
├── frontend/                   # Vue3前端
│   ├── src/
│   │   ├── api/               # API接口
│   │   ├── views/             # 页面组件
│   │   ├── router/            # 路由配置
│   │   ├── store/             # 状态管理
│   │   └── utils/             # 工具函数
│   └── package.json
└── database/                   # 数据库脚本
    ├── init.sql
    └── init-data.sql
```

### 后续阶段规划

| 阶段 | 内容 | 计划文件 |
|------|------|----------|
| 第二阶段 | 标准物质主数据 + 库存管理 + 出入库管理 | phase2-stock.md |
| 第三阶段 | 采购管理 + 盘点管理 | phase3-purchase-check.md |
| 第四阶段 | 预警系统 + 报表统计 + 首页仪表盘 | phase4-alert-report.md |
| 第五阶段 | 数据迁移 + 测试部署 | phase5-migration.md |

---

**计划完成，保存到 `docs/superpowers/plans/2025-03-17-phase1-foundation.md`。准备执行？**

