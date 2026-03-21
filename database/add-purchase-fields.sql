-- 添加采购申请新字段
ALTER TABLE purchase
ADD COLUMN specification VARCHAR(200) COMMENT '规格' AFTER material_id,
ADD COLUMN batch_number VARCHAR(100) COMMENT '批号' AFTER specification,
ADD COLUMN unit VARCHAR(50) DEFAULT '支' COMMENT '单位（支、瓶、盒等）' AFTER batch_number,
ADD COLUMN total_amount DECIMAL(12,2) COMMENT '金额（采购数量*预估单价）' AFTER estimated_price;
