-- 为预警记录表添加内部编码聚合字段
ALTER TABLE alert_record
ADD COLUMN `internal_codes` TEXT COMMENT '关联的内部编码列表(逗号分隔)' AFTER `stock_id`;
