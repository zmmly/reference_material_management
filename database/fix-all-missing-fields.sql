-- 修复所有缺失的数据库字段

-- 1. 为 stock_in 表添加 product_certificate 字段
ALTER TABLE `stock_in` ADD COLUMN `product_certificate` VARCHAR(255) COMMENT '产品证书文件路径' AFTER `remarks`;

-- 2. 为 stock_out 表添加 internal_code 和 batch_no 字段
ALTER TABLE `stock_out` ADD COLUMN `internal_code` VARCHAR(50) COMMENT '内部编码' AFTER `material_id`;
ALTER TABLE `stock_out` ADD COLUMN `batch_no` VARCHAR(100) COMMENT '批号' AFTER `internal_code`;
