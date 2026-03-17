-- 标准物质管理系统 - 测试数据初始化脚本
-- 用于补充测试所需的各类数据

USE reference_material_management;

-- 清空现有测试数据（可选）
-- DELETE from alert_record where id > 1;
-- delete from stock where id > 1;
-- delete from reference_material where id > 1;
-- delete from location where id > 1;
-- delete from category where id > 1;

-- 插入分类数据
insert into category (id, name, parent_id, sort_order, status, create_time, update_time) values
(1, '有机标准物质', 0, 1, 1, now(), now()),
(2, '无机标准物质', 0, 2, 1, now(), now()),
(3, '纯度标准物质', 1, 1, 1, now(), now()),
(4, '农药残留', 1, 2, 1, now(), now()),
(5, '重金属', 2, 1, 1, now(), now()),
(6, '环境监测', 1, 3, 1, now(), now());

-- 插入位置数据
insert into location (id, code, name, temperature, capacity, description, status, create_time, update_time) values
(1, 'A-01', '冰箱A区', '-20℃', 100, '低温保存区', 1, now(), now()),
(2, 'A-02', '冰箱B区', '2-8℃', 150, '冷藏保存区', 1, now(), now()),
(3, 'B-01', '常温柜A', '常温', 200, '常温保存区', 1, now(), now()),
(4, 'B-02', '常温柜B', '常温', 200, '常温保存区', 1, now(), now()),
(5, 'C-01', '阴凉柜', '阴凉干燥', 100, '阴凉干燥保存区', 1, now(), now());

-- 插入标准物质数据
insert into reference_material (id, code, name, english_name, standard_value, uncertainty, specification, unit, category_id, storage_condition, manufacturer, remarks, status, create_time, update_time) values
(1, 'GBW(E)081234', '邻苯二甲酸酯类农药残留标准品', 'Phthalates', '100μg/mL', '2%', 'GBW(E)081234', 'mg/L', 1, '2-8℃', '中国计量科学研究院', '环境监测用', 1, now(), now()),
(2, 'GBW(E)081235', '有机氯农药残留标准品', 'Organochlorines', '50μg/mL', '3%', 'GBW(E)081235', 'mg/L', 1, '2-8℃', '中国计量科学研究院', '环境监测用', 1, now(), now()),
(3, 'GBW(E)081236', '重金属标准品-铅', 'Lead', '1000mg/L', '1%', 'GBW(E)081236', 'mg/L', 5, '常温', '中国计量科学研究院', '环境监测用', 1, now(), now()),
(4, 'GBW(E)081237', '重金属标准品-镉', 'Cadmium', '1000mg/L', '1%', 'GBW(E)081237', 'mg/L', 5, '常温', '中国计量科学研究院', '环境监测用', 1, now(), now()),
(5, 'GBW(E)081238', '重金属标准品-汞', 'Mercury', '1000mg/L', '1%', 'GBW(E)081238', 'mg/L', 5, '2-8℃', '中国计量科学研究院', '环境监测用', 1, now(), now()),
(6, 'GBW(E)081239', '挥发性有机物标准品', 'VOCs', '1000μg/mL', '2%', 'GBW(E)081239', 'mg/L', 3, '2-8℃', '中国计量科学研究院', '环境监测用', 1, now(), now());

-- 插入库存数据
insert into stock (id, material_id, batch_no, internal_code, expiry_date, quantity, location_id, status, create_time, update_time) values
(1, 1, '2024001', 'RMM2024001', DATE_ADD(now(), INTERVAL 6 MONTH), 50.00, 1, 1, now(), now()),
(2, 1, '2024002', 'RMM2024002', DATE_ADD(now(), INTERVAL 1 MONTH), 30.00, 2, 1, now(), now()),
(3, 2, '2024003', 'RMM2024003', DATE_ADD(now(), INTERVAL 7 DAY), 20.00, 3, 1, now(), now()),
(4, 2, '2024004', 'RMM2024004', DATE_SUB(now(), INTERVAL 1 DAY), 10.00, 4, 3, now(), now()),
(5, 3, '2024005', 'RMM2024005', DATE_ADD(now(), INTERVAL 1 YEAR), 100.00, 5, 1, now(), now()),
(6, 4, '2024006', 'RMM2024006', DATE_ADD(now(), INTERVAL 2 YEAR), 80.00, 3, 1, now(), now()),
(7, 5, '2024007', 'RMM2024007', DATE_ADD(now(), INTERVAL 6 MONTH), 60.00, 1, 1, now(), now()),
(8, 6, '2024008', 'RMM2024008', DATE_ADD(now(), INTERVAL 3 MONTH), 40.00, 2, 1, now(), now());

-- 插入预警记录数据
insert into alert_record (id, type, stock_id, material_id, content, level, status, create_time) values
(1, 'EXPIRY_WARNING', 2, 1, '邻苯二甲酸酯类农药残留标准品(RMM2024002)将于1个月后过期，当前库存30', 2, 0, now()),
(2, 'EXPIRY_CRITICAL', 3, 2, '有机氯农药残留标准品(RMM2024003)将于7天内过期，当前库存20', 3, 0, now()),
(3, 'EXPIRY_CRITICAL', 4, 2, '有机氯农药残留标准品(RMM2024004)已过期，当前库存10', 3, 0, now()),
(4, 'STOCK_LOW', 1, 1, '邻苯二甲酸酯类农药残留标准品(RMM2024001)库存不足，当前库存50，安全库存100', 2, 0, now()),
(5, 'UNUSED', 5, 3, '重金属标准品-铅(RMM2024005)超过90天未使用', 1, 0, now());

-- 插入供应商数据
insert into supplier (id, name, contact, phone, email, address, remarks, status, create_time, update_time) values
(1, '中国计量科学研究院', '张经理', '010-12345678', 'sales@nim.ac.cn', '北京市海淀区', '国家级计量标准供应商', 1, now(), now()),
(2, '国家标准物质研究中心', '李经理', '010-87654321', 'sales@nrm.org.cn', '北京市朝阳区', '标准物质专业供应商', 1, now(), now()),
(3, '上海标准物质研究所', '王经理', '021-12345678', 'sales@shrm.com', '上海市浦东新区', '区域性标准物质供应商', 1, now(), now());
