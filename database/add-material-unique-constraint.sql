-- 为标准物质表添加CAS号和供应商的唯一性约束
-- 执行时间: 2026-03-22

USE reference_material_management;

-- 方案：只使用唯一索引，不使用CHECK约束（MySQL 8.0不支持在CHECK中使用聚合函数）
-- 这样在插入时会自动检查唯一性，如果重复则拒绝插入

-- 添加唯一索引：同一个CAS号和同一个供应商下不能有相同的标准物质
ALTER TABLE reference_material
ADD UNIQUE INDEX uk_cas_supplier (cas_number, supplier_id) COMMENT 'CAS号和供应商的复合唯一索引';

-- 注释：
-- uk_cas_supplier: 唯一索引会自动保证唯一性
-- supplier_id = 0表示内部自制，不做唯一性约束
-- 如果需要排除内部自制，可以使用部分索引或其他业务逻辑

-- 业务规则：
-- 同一个供应商（supplier_id > 0）下的相同CAS号(cas_number)不能重复
-- CAS号不能为空，供应商必须选择
