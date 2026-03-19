-- 盘点分组表（按批号+位置分组）
CREATE TABLE IF NOT EXISTS stock_check_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_id BIGINT NOT NULL COMMENT '盘点任务ID',
  material_id BIGINT NOT NULL COMMENT '标准物质ID',
  batch_no VARCHAR(100) COMMENT '批号',
  location_id BIGINT COMMENT '存放位置ID',
  location_name VARCHAR(100) COMMENT '存放位置名称',
  internal_codes TEXT COMMENT '内部编码列表(逗号分隔)',
  item_count INT DEFAULT 0 COMMENT '明细数量(多少件)',
  system_quantity DECIMAL(10,2) DEFAULT 0 COMMENT '系统数量(合计)',
  actual_quantity DECIMAL(10,2) COMMENT '实盘数量',
  difference DECIMAL(10,2) COMMENT '差异',
  difference_reason VARCHAR(500) COMMENT '差异说明',
  status TINYINT DEFAULT 0 COMMENT '状态:0-未盘点,1-已盘点(无差异),2-已盘点(有差异)',
  checker_id BIGINT COMMENT '盘点人ID',
  check_time DATETIME COMMENT '盘点时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_check_id (check_id),
  INDEX idx_material_id (material_id),
  INDEX idx_group_key (check_id, batch_no, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点分组表';

-- 盘点明细与库存关联表
CREATE TABLE IF NOT EXISTS stock_check_item_stock (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_id BIGINT NOT NULL COMMENT '盘点任务ID',
  group_id BIGINT NOT NULL COMMENT '所属分组ID',
  stock_id BIGINT NOT NULL COMMENT '库存ID',
  system_quantity DECIMAL(10,2) COMMENT '该库存的系统数量',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_check_id (check_id),
  INDEX idx_group_id (group_id),
  INDEX idx_stock_id (stock_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细与库存关联表';
