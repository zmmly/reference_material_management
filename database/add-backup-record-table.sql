-- 创建系统备份记录表
-- 执行时间: 2026-03-22

USE reference_material_management;

-- 创建备份记录表
CREATE TABLE IF NOT EXISTS backup_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  filename VARCHAR(255) NOT NULL COMMENT '文件名',
  file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
  backup_time DATETIME NOT NULL COMMENT '备份时间',
  operator_id BIGINT NOT NULL COMMENT '操作人ID',
  operator_name VARCHAR(100) NOT NULL COMMENT '操作人姓名',
  INDEX idx_backup_time (backup_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统备份记录表';

-- 说明：
-- backup_record 表用于存储系统数据库备份记录
-- 系统备份功能需要此表来跟踪备份文件和操作记录
