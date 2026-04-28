-- 出库归还功能数据库迁移

-- stock_out 表增加 need_return 和 returned 字段
ALTER TABLE `stock_out`
  ADD COLUMN `need_return` tinyint(1) DEFAULT 0 COMMENT '是否需要归还: 0否 1是' AFTER `remarks`,
  ADD COLUMN `returned` tinyint(1) DEFAULT 0 COMMENT '是否已归还: 0否 1是' AFTER `need_return`;

-- stock_in 表增加 stock_out_id 字段
ALTER TABLE `stock_in`
  ADD COLUMN `stock_out_id` bigint DEFAULT NULL COMMENT '关联的出库记录ID' AFTER `stock_id`;

-- 更新 stock 表状态注释
ALTER TABLE `stock` MODIFY COLUMN `status` tinyint DEFAULT '1' COMMENT '状态: 0已出库 1正常 2即将过期 3已过期 4借出/待归还';
