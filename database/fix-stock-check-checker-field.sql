-- 修复stock_check表缺少checker_id字段的问题
ALTER TABLE stock_check
ADD COLUMN checker_id BIGINT COMMENT '盘点人ID' AFTER creator_id;
