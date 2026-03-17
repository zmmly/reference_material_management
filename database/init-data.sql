-- 标准物质管理系统 - 初始数据脚本

USE reference_material_management;

-- 初始化角色
INSERT INTO `role` (`name`, `code`, `permissions`) VALUES
('系统管理员', 'ADMIN', '["*"]'),
('标准物质管理员', 'MANAGER', '["stock:*","purchase:*","check:*"]'),
('普通用户', 'USER', '["stock:view","stock:out:apply"]');

-- 初始化管理员账号 (密码: admin123，使用BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `role_id`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iK7iAt6', '系统管理员', 1, 1);

-- 初始化分类
INSERT INTO `category` (`name`, `parent_id`, `sort_order`) VALUES
('对照品', 0, 1),
('对照药材', 0, 2),
('标准溶液', 0, 3),
('滴定液', 0, 4),
('化妆品标准物质', 0, 5);

-- 初始化元数据 - 入库原因
INSERT INTO `metadata` (`type`, `code`, `name`, `sort_order`) VALUES
('STOCK_IN_REASON', 'PURCHASE', '新购入', 1),
('STOCK_IN_REASON', 'SURPLUS', '盘盈', 2),
('STOCK_IN_REASON', 'RETURN', '归还', 3),
('STOCK_IN_REASON', 'TRANSFER_IN', '调拨入', 4),
('STOCK_IN_REASON', 'OTHER', '其他', 5);

-- 初始化元数据 - 出库原因
INSERT INTO `metadata` (`type`, `code`, `name`, `sort_order`) VALUES
('STOCK_OUT_REASON', 'EXPERIMENT', '实验使用', 1),
('STOCK_OUT_REASON', 'EXPIRED', '过期销毁', 2),
('STOCK_OUT_REASON', 'SCRAP', '报废', 3),
('STOCK_OUT_REASON', 'TRANSFER_OUT', '调拨出', 4),
('STOCK_OUT_REASON', 'DONATE', '赠送', 5),
('STOCK_OUT_REASON', 'OTHER', '其他', 6);

-- 初始化元数据 - 储存条件
INSERT INTO `metadata` (`type`, `code`, `name`, `sort_order`) VALUES
('STORAGE_CONDITION', 'MINUS_20', '-20℃', 1),
('STORAGE_CONDITION', 'COLD_2_8', '2-8℃', 2),
('STORAGE_CONDITION', 'ROOM', '常温', 3),
('STORAGE_CONDITION', 'COOL_DRY', '阴凉干燥', 4),
('STORAGE_CONDITION', 'DARK', '避光', 5),
('STORAGE_CONDITION', 'ROOM_10_30', '10-30℃', 6);

-- 初始化位置
INSERT INTO `location` (`code`, `name`, `temperature`, `capacity`) VALUES
('LOC001', '冰箱A (-20℃)', '-20℃', 100),
('LOC002', '冰箱B (2-8℃)', '2-8℃', 150),
('LOC003', '常温柜A', '常温', 200),
('LOC004', '阴凉柜A', '阴凉干燥', 100);
