-- 采购验收功能表结构

-- 为采购表添加采购申请单号字段
ALTER TABLE `purchase` ADD COLUMN `purchase_no` VARCHAR(50) COMMENT '采购申请单号' AFTER `id`;
ALTER TABLE `purchase` ADD INDEX `idx_purchase_no` (`purchase_no`);

-- 修改采购表状态注释，添加待验收状态
-- 状态: 0待审批 1已通过(待验收) 2已拒绝 3已撤回 4待验收 5验收中 6验收通过 7验收拒绝

-- 采购验收表
CREATE TABLE IF NOT EXISTS `purchase_acceptance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `purchase_id` BIGINT NOT NULL COMMENT '采购申请ID',
    `purchase_no` VARCHAR(50) COMMENT '采购申请单号',
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `material_name` VARCHAR(200) COMMENT '标准物质名称',
    `material_code` VARCHAR(50) COMMENT '标准物质编码',
    `specification` VARCHAR(200) COMMENT '规格',
    `batch_number` VARCHAR(100) COMMENT '批号',
    `quantity` DECIMAL(10,2) COMMENT '采购数量',
    `unit` VARCHAR(20) COMMENT '单位',
    `supplier_id` BIGINT COMMENT '供应商ID',
    `supplier_name` VARCHAR(200) COMMENT '供应商名称',
    `estimated_price` DECIMAL(10,2) COMMENT '预估单价',
    `total_amount` DECIMAL(10,2) COMMENT '总金额',

    -- 验收信息
    `package_intact` TINYINT DEFAULT NULL COMMENT '外包装是否完好: 0否 1是',
    `label_complete` TINYINT DEFAULT NULL COMMENT '标签是否完整: 0否 1是',
    `has_damage` TINYINT DEFAULT NULL COMMENT '有无破损: 0否 1是',
    `acceptance_result` TINYINT DEFAULT NULL COMMENT '验收结果: 0待验收 1通过 2拒绝',
    `acceptance_remark` VARCHAR(500) COMMENT '验收备注',
    `acceptance_date` DATETIME COMMENT '验收日期',
    `acceptance_user_id` BIGINT COMMENT '验收人ID',
    `acceptance_user_name` VARCHAR(100) COMMENT '验收人姓名',

    -- 入库信息
    `stock_in_id` BIGINT COMMENT '生成的入库单ID',

    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_purchase_id` (`purchase_id`),
    INDEX `idx_purchase_no` (`purchase_no`),
    INDEX `idx_status` (`acceptance_result`),
    INDEX `idx_time` (`create_time`)
) ENGINE=InnoDB COMMENT='采购验收表';
