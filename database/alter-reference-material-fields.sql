-- 修改标准物质表字段
-- 执行时间: 2026-03-19

USE reference_material_management;

-- 1. 删除不需要的字段
ALTER TABLE reference_material
  DROP COLUMN standard_value,
  DROP COLUMN uncertainty,
  DROP COLUMN unit,
  DROP COLUMN storage_condition,
  DROP COLUMN manufacturer,
  DROP COLUMN remarks;

-- 2. 添加新字段
ALTER TABLE reference_material
  ADD COLUMN cas_number VARCHAR(50) COMMENT 'CAS号' AFTER english_name,
  ADD COLUMN purity_concentration VARCHAR(100) NOT NULL DEFAULT '' COMMENT '纯度/浓度' AFTER specification,
  ADD COLUMN matrix VARCHAR(200) DEFAULT '' COMMENT '基质' AFTER purity_concentration,
  ADD COLUMN package_form VARCHAR(100) DEFAULT '' COMMENT '包装形式' AFTER matrix;

-- 3. 修改现有字段为必填
ALTER TABLE reference_material
  MODIFY COLUMN specification VARCHAR(200) NOT NULL COMMENT '规格';

-- 查看修改后的表结构
DESCRIBE reference_material;
