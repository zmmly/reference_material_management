-- 标准物质管理系统 - 第四阶段数据库表

USE reference_material_management;

-- 预警配置表
CREATE TABLE IF NOT EXISTS `alert_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL COMMENT '预警类型: EXPIRY/STOCK_LOW/UNUSED',
    `name` VARCHAR(100) NOT NULL COMMENT '预警名称',
    `threshold` INT COMMENT '阈值(天数/数量)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB COMMENT='预警配置表';

-- 预警记录表
CREATE TABLE IF NOT EXISTS `alert_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL COMMENT '预警类型',
    `stock_id` BIGINT COMMENT '库存ID',
    `material_id` BIGINT COMMENT '标准物质ID',
    `content` VARCHAR(500) COMMENT '预警内容',
    `level` TINYINT DEFAULT 1 COMMENT '预警级别: 1普通 2重要 3紧急',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0未处理 1已处理 2已忽略',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handle_time` DATETIME COMMENT '处理时间',
    `handle_remark` VARCHAR(500) COMMENT '处理备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_type` (`type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`create_time`)
) ENGINE=InnoDB COMMENT='预警记录表';

-- 初始化预警配置
INSERT INTO `alert_config` (`type`, `name`, `threshold`, `enabled`) VALUES
('EXPIRY_WARNING', '有效期预警（提前天数）', 30, 1),
('EXPIRY_CRITICAL', '有效期紧急预警（提前天数）', 7, 1),
('STOCK_LOW', '库存不足预警（最低数量）', 5, 1),
('UNUSED_MONTHS', '长期未使用预警（月数）', 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
