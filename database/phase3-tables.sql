-- 标准物质管理系统 - 第三阶段数据库表

USE reference_material_management;

-- 采购申请表
CREATE TABLE IF NOT EXISTS `purchase` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `quantity` DECIMAL(10,2) NOT NULL COMMENT '采购数量',
    `supplier_id` BIGINT COMMENT '供应商ID',
    `estimated_price` DECIMAL(10,2) COMMENT '预估单价',
    `estimated_arrival_date` DATE COMMENT '预计到货日期',
    `reason` VARCHAR(500) COMMENT '采购原因',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approve_time` DATETIME COMMENT '审批时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回 4已到货',
    `reject_reason` VARCHAR(500) COMMENT '拒绝原因',
    `remarks` VARCHAR(500) COMMENT '备注',
    INDEX `idx_material` (`material_id`),
    INDEX `idx_applicant` (`applicant_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`apply_time`)
) ENGINE=InnoDB COMMENT='采购申请表';

-- 盘点任务表
CREATE TABLE IF NOT EXISTS `stock_check` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `check_no` VARCHAR(50) NOT NULL COMMENT '盘点单号',
    `check_date` DATE NOT NULL COMMENT '盘点日期',
    `scope` VARCHAR(50) COMMENT '盘点范围: ALL/CATEGORY/LOCATION',
    `scope_value` VARCHAR(100) COMMENT '范围值',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0进行中 1已完成 2已作废',
    `total_count` INT DEFAULT 0 COMMENT '总项数',
    `checked_count` INT DEFAULT 0 COMMENT '已盘项数',
    `difference_count` INT DEFAULT 0 COMMENT '差异项数',
    `creator_id` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `complete_time` DATETIME COMMENT '完成时间',
    `remarks` VARCHAR(500) COMMENT '备注',
    UNIQUE KEY `uk_check_no` (`check_no`),
    INDEX `idx_status` (`status`),
    INDEX `idx_date` (`check_date`)
) ENGINE=InnoDB COMMENT='盘点任务表';

-- 盘点明细表
CREATE TABLE IF NOT EXISTS `stock_check_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `check_id` BIGINT NOT NULL COMMENT '盘点任务ID',
    `stock_id` BIGINT NOT NULL COMMENT '库存ID',
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `system_quantity` DECIMAL(10,2) COMMENT '系统数量',
    `actual_quantity` DECIMAL(10,2) COMMENT '实盘数量',
    `difference` DECIMAL(10,2) COMMENT '差异',
    `difference_reason` VARCHAR(500) COMMENT '差异原因',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待盘 1已盘 2有差异',
    `checker_id` BIGINT COMMENT '盘点人ID',
    `check_time` DATETIME COMMENT '盘点时间',
    `remarks` VARCHAR(500) COMMENT '备注',
    INDEX `idx_check` (`check_id`),
    INDEX `idx_stock` (`stock_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='盘点明细表';

-- 盘点差异处理表
CREATE TABLE IF NOT EXISTS `stock_check_adjust` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `check_item_id` BIGINT NOT NULL COMMENT '盘点明细ID',
    `adjust_type` VARCHAR(20) COMMENT '调整类型: SURPLUS/LOSS',
    `adjust_quantity` DECIMAL(10,2) COMMENT '调整数量',
    `reason` VARCHAR(500) COMMENT '调整原因',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_check_item` (`check_item_id`)
) ENGINE=InnoDB COMMENT='盘点差异处理表';
