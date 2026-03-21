-- 添加产品证书字段到stock_in表
ALTER TABLE `stock_in` ADD COLUMN `product_certificate` VARCHAR(255) COMMENT '产品证书文件路径' AFTER `remarks`;
