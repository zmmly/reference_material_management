-- 标准物质管理系统 - 数据库初始化脚本

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
