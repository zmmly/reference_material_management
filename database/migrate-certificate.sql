-- 证书表：独立管理标准物质批号证书
CREATE TABLE IF NOT EXISTS certificate (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  material_id bigint NOT NULL COMMENT '标准物质ID',
  batch_no varchar(100) NOT NULL COMMENT '批号',
  file_path varchar(255) NOT NULL COMMENT '证书文件路径',
  file_name varchar(255) COMMENT '原始文件名',
  uploader_id bigint COMMENT '上传人ID',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_batch (material_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书管理表';

-- 迁移已有证书数据：从 stock_in 提取到 certificate 表
INSERT INTO certificate (material_id, batch_no, file_path, uploader_id, create_time)
SELECT s.material_id, s.batch_no, s.product_certificate, s.operator_id, s.create_time
FROM stock_in s
INNER JOIN (
    SELECT material_id, batch_no, MIN(id) as min_id
    FROM stock_in
    WHERE product_certificate IS NOT NULL AND product_certificate != ''
    GROUP BY material_id, batch_no
) t ON s.material_id = t.material_id AND s.id = t.min_id;
