-- 将纯度/浓度字段从 reference_material 移动到 stock 和 stock_in 表
-- 执行前请备份数据

-- 1. 修改 reference_material 表，将 purity_concentration 改为可空
ALTER TABLE `reference_material`
MODIFY COLUMN `purity_concentration` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纯度/浓度';

-- 2. 给 stock 表添加 purity_concentration 字段
ALTER TABLE `stock`
ADD COLUMN `purity_concentration` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纯度/浓度' AFTER `internal_code`;

-- 3. 给 stock_in 表添加 purity_concentration 字段
ALTER TABLE `stock_in`
ADD COLUMN `purity_concentration` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '纯度/浓度' AFTER `internal_code`;
