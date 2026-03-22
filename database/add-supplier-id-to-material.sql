-- 为标准物质表添加供应商ID字段
-- 执行时间: 2026-03-22

USE reference_material_management;

-- 添加供应商ID字段
ALTER TABLE reference_material
ADD COLUMN supplier_id BIGINT DEFAULT NULL COMMENT '供应商ID' AFTER category_id;

-- 添加外键约束（可选）
-- ALTER TABLE reference_material
-- ADD CONSTRAINT fk_material_supplier
-- FOREIGN KEY (supplier_id)
-- REFERENCES supplier(id)
-- ON DELETE SET NULL;
