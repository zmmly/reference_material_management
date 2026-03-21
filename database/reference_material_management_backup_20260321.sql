/*
 Navicat Premium Data Transfer

 Source Server         : 阿里云
 Source Server Type    : MySQL
 Source Server Version : 80045
 Source Host           : 8.138.246.23:3306
 Source Schema         : reference_material_management

 Target Server Type    : MySQL
 Target Server Version : 80045
 File Encoding         : 65001

 Date: 21/03/2026 11:28:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_config
-- ----------------------------
DROP TABLE IF EXISTS `alert_config`;
CREATE TABLE `alert_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型: EXPIRY/STOCK_LOW/UNUSED',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警名称',
  `threshold` int NULL DEFAULT NULL COMMENT '阈值(天数/数量)',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of alert_config
-- ----------------------------
INSERT INTO `alert_config` VALUES (1, 'EXPIRY_WARNING', '有效期预警（提前天数）', 30, 1, '2026-03-21 00:14:25', '2026-03-21 00:14:25');
INSERT INTO `alert_config` VALUES (2, 'EXPIRY_CRITICAL', '有效期紧急预警（提前天数）', 7, 1, '2026-03-21 00:14:25', '2026-03-21 00:14:25');
INSERT INTO `alert_config` VALUES (3, 'STOCK_LOW', '库存不足预警（最低数量）', 5, 1, '2026-03-21 00:14:25', '2026-03-21 00:14:25');
INSERT INTO `alert_config` VALUES (4, 'UNUSED_MONTHS', '长期未使用预警（月数）', 6, 1, '2026-03-21 00:14:25', '2026-03-21 00:14:25');

-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型',
  `stock_id` bigint NULL DEFAULT NULL COMMENT '库存ID',
  `internal_codes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关联的内部编码列表(逗号分隔)',
  `material_id` bigint NULL DEFAULT NULL COMMENT '标准物质ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预警内容',
  `level` tinyint NULL DEFAULT 1 COMMENT '预警级别: 1普通 2重要 3紧急',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0未处理 1已处理 2已忽略',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime(0) NULL DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预警记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of alert_record
-- ----------------------------
INSERT INTO `alert_record` VALUES (1, 'EXPIRY_CRITICAL', 22, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (2, 'EXPIRY_CRITICAL', 23, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (3, 'EXPIRY_CRITICAL', 24, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (4, 'EXPIRY_CRITICAL', 25, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (5, 'EXPIRY_CRITICAL', 26, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (6, 'EXPIRY_CRITICAL', 27, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (7, 'EXPIRY_CRITICAL', 28, NULL, 2, '【阿魏酸】将在5天后过期', 3, 0, NULL, NULL, NULL, '2026-03-21 10:58:20');
INSERT INTO `alert_record` VALUES (8, 'UNUSED', 22, NULL, 2, '【阿魏酸-001-001】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (9, 'UNUSED', 23, NULL, 2, '【阿魏酸-001-002】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (10, 'UNUSED', 24, NULL, 2, '【阿魏酸-001-003】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (11, 'UNUSED', 25, NULL, 2, '【阿魏酸-001-004】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (12, 'UNUSED', 26, NULL, 2, '【阿魏酸-001-005】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (13, 'UNUSED', 27, NULL, 2, '【阿魏酸-001-006】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');
INSERT INTO `alert_record` VALUES (14, 'UNUSED', 28, NULL, 2, '【阿魏酸-001-007】已超过6个月未使用', 1, 0, NULL, NULL, NULL, '2026-03-21 10:58:21');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级ID',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_parent`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标准物质分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (5, '化妆品标准物质', 0, 5, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `category` VALUES (6, '药品', 0, 0, 1, '2026-03-21 10:23:52', '2026-03-21 10:35:43');
INSERT INTO `category` VALUES (7, '对照品', 6, 0, 1, '2026-03-21 10:24:04', '2026-03-21 10:24:04');
INSERT INTO `category` VALUES (8, '对照药材', 6, 0, 1, '2026-03-21 10:24:14', '2026-03-21 10:24:14');
INSERT INTO `category` VALUES (9, '标准溶液', 6, 0, 1, '2026-03-21 10:24:34', '2026-03-21 10:24:34');
INSERT INTO `category` VALUES (10, '滴定液', 6, 0, 1, '2026-03-21 10:24:42', '2026-03-21 10:24:42');

-- ----------------------------
-- Table structure for location
-- ----------------------------
DROP TABLE IF EXISTS `location`;
CREATE TABLE `location`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '位置编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '位置名称',
  `temperature` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '温度要求',
  `capacity` int NULL DEFAULT NULL COMMENT '容量',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '存放位置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of location
-- ----------------------------
INSERT INTO `location` VALUES (1, 'LOC001', '冰箱A (-20℃)', '-20℃', 100, NULL, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `location` VALUES (2, 'LOC002', '冰箱B (2-8℃)', '2-8℃', 150, NULL, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `location` VALUES (3, 'LOC003', '常温柜A', '常温', 200, NULL, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `location` VALUES (4, 'LOC004', '阴凉柜A', '阴凉干燥', 100, NULL, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');

-- ----------------------------
-- Table structure for metadata
-- ----------------------------
DROP TABLE IF EXISTS `metadata`;
CREATE TABLE `metadata`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: STOCK_IN_REASON/STOCK_OUT_REASON/STORAGE_CONDITION',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `sort_order` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_code`(`type`, `code`) USING BTREE,
  INDEX `idx_metadata_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '元数据配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of metadata
-- ----------------------------
INSERT INTO `metadata` VALUES (1, 'STOCK_IN_REASON', 'PURCHASE', '新购入', 1, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (2, 'STOCK_IN_REASON', 'SURPLUS', '盘盈', 2, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (3, 'STOCK_IN_REASON', 'RETURN', '归还', 3, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (4, 'STOCK_IN_REASON', 'TRANSFER_IN', '调拨入', 4, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (5, 'STOCK_IN_REASON', 'OTHER', '其他', 5, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (6, 'STOCK_OUT_REASON', 'EXPERIMENT', '实验使用', 1, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (7, 'STOCK_OUT_REASON', 'EXPIRED', '过期销毁', 2, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (8, 'STOCK_OUT_REASON', 'SCRAP', '报废', 3, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (9, 'STOCK_OUT_REASON', 'TRANSFER_OUT', '调拨出', 4, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (10, 'STOCK_OUT_REASON', 'DONATE', '赠送', 5, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (11, 'STOCK_OUT_REASON', 'OTHER', '其他', 6, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (12, 'STORAGE_CONDITION', 'MINUS_20', '-20℃', 1, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (13, 'STORAGE_CONDITION', 'COLD_2_8', '2-8℃', 2, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (14, 'STORAGE_CONDITION', 'ROOM', '常温', 3, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (15, 'STORAGE_CONDITION', 'COOL_DRY', '阴凉干燥', 4, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (16, 'STORAGE_CONDITION', 'DARK', '避光', 5, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');
INSERT INTO `metadata` VALUES (17, 'STORAGE_CONDITION', 'ROOM_10_30', '10-30℃', 6, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作用户名',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作类型',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模块',
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作对象',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作详情',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_log_user`(`user_id`) USING BTREE,
  INDEX `idx_log_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for purchase
-- ----------------------------
DROP TABLE IF EXISTS `purchase`;
CREATE TABLE `purchase`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格',
  `batch_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '批号',
  `unit` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '支' COMMENT '单位（支、瓶、盒等）',
  `quantity` decimal(10, 2) NOT NULL COMMENT '采购数量',
  `supplier_id` bigint NULL DEFAULT NULL COMMENT '供应商ID',
  `estimated_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '预估单价',
  `total_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '金额（采购数量*预估单价）',
  `estimated_arrival_date` date NULL DEFAULT NULL COMMENT '预计到货日期',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '采购原因',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '申请时间',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime(0) NULL DEFAULT NULL COMMENT '审批时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回 4已到货',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_material`(`material_id`) USING BTREE,
  INDEX `idx_applicant`(`applicant_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_time`(`apply_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采购申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase
-- ----------------------------
INSERT INTO `purchase` VALUES (1, 2, '20mgg', '待确认', '支', 10.00, 1, 600.00, 6000.00, '2026-03-20', '库存告警', 1, '2026-03-21 11:20:43', NULL, NULL, 0, NULL, NULL);
INSERT INTO `purchase` VALUES (2, 2, '20mgg', '待确认', '支', 10.00, 1, 600.00, 6000.00, '2026-03-20', '库存告警', 1, '2026-03-21 11:20:59', NULL, NULL, 0, NULL, NULL);
INSERT INTO `purchase` VALUES (3, 2, '20mgg', '待确认', '支', 1.00, 1, 500.00, 500.00, '2026-03-27', '测试', 1, '2026-03-21 11:22:17', NULL, NULL, 0, NULL, NULL);

-- ----------------------------
-- Table structure for reference_material
-- ----------------------------
DROP TABLE IF EXISTS `reference_material`;
CREATE TABLE `reference_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编号',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `english_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '英文名',
  `cas_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'CAS号',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格',
  `purity_concentration` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '纯度/浓度',
  `matrix` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '基质',
  `package_form` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '包装形式',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE,
  INDEX `idx_category`(`category_id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标准物质主数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reference_material
-- ----------------------------
INSERT INTO `reference_material` VALUES (2, '110732', '阿魏酸', '', '', '20mgg', '99.3%', '', '', 7, 1, '2026-03-21 10:40:34', '2026-03-21 10:40:34');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `permissions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '权限列表(JSON)',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, '系统管理员', 'ADMIN', '[\"*\"]', 1, '2026-03-21 00:12:33', '2026-03-21 00:12:33');
INSERT INTO `role` VALUES (2, '标准物质管理员', 'MANAGER', '[\"stock:*\",\"purchase:*\",\"check:*\"]', 1, '2026-03-21 00:12:33', '2026-03-21 00:12:33');
INSERT INTO `role` VALUES (3, '普通用户', 'USER', '[\"stock:view\",\"stock:out:apply\"]', 1, '2026-03-21 00:12:33', '2026-03-21 00:12:33');

-- ----------------------------
-- Table structure for stock
-- ----------------------------
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '批号',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内部编码',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期',
  `quantity` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '库存数量',
  `location_id` bigint NULL DEFAULT NULL COMMENT '存放位置ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态: 1正常 2即将过期 3已过期',
  `last_out_time` datetime(0) NULL DEFAULT NULL COMMENT '最后出库时间',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_material`(`material_id`) USING BTREE,
  INDEX `idx_location`(`location_id`) USING BTREE,
  INDEX `idx_expiry`(`expiry_date`) USING BTREE,
  INDEX `idx_internal_code`(`internal_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock
-- ----------------------------
INSERT INTO `stock` VALUES (22, 2, '001', '001-001', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:46:48', '2026-03-21 10:46:48');
INSERT INTO `stock` VALUES (23, 2, '001', '001-002', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:46:50', '2026-03-21 10:46:50');
INSERT INTO `stock` VALUES (24, 2, '001', '001-003', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:47:38', '2026-03-21 10:47:38');
INSERT INTO `stock` VALUES (25, 2, '001', '001-004', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:47:38', '2026-03-21 10:47:38');
INSERT INTO `stock` VALUES (26, 2, '001', '001-005', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:47:38', '2026-03-21 10:47:38');
INSERT INTO `stock` VALUES (27, 2, '001', '001-006', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:47:38', '2026-03-21 10:47:38');
INSERT INTO `stock` VALUES (28, 2, '001', '001-007', '2026-03-26', 1.00, 1, 1, NULL, '2026-03-21 10:47:38', '2026-03-21 10:47:38');

-- ----------------------------
-- Table structure for stock_check
-- ----------------------------
DROP TABLE IF EXISTS `stock_check`;
CREATE TABLE `stock_check`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点单号',
  `check_date` date NOT NULL COMMENT '盘点日期',
  `scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '盘点范围: ALL/CATEGORY/LOCATION',
  `scope_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '范围值',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0进行中 1已完成 2已作废',
  `total_count` int NULL DEFAULT 0 COMMENT '总项数',
  `checked_count` int NULL DEFAULT 0 COMMENT '已盘项数',
  `difference_count` int NULL DEFAULT 0 COMMENT '差异项数',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `complete_time` datetime(0) NULL DEFAULT NULL COMMENT '完成时间',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_check_no`(`check_no`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_date`(`check_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '盘点任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check
-- ----------------------------

-- ----------------------------
-- Table structure for stock_check_adjust
-- ----------------------------
DROP TABLE IF EXISTS `stock_check_adjust`;
CREATE TABLE `stock_check_adjust`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_item_id` bigint NOT NULL COMMENT '盘点明细ID',
  `adjust_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调整类型: SURPLUS/LOSS',
  `adjust_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '调整数量',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调整原因',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operate_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_check_item`(`check_item_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '盘点差异处理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check_adjust
-- ----------------------------

-- ----------------------------
-- Table structure for stock_check_group
-- ----------------------------
DROP TABLE IF EXISTS `stock_check_group`;
CREATE TABLE `stock_check_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `location_id` bigint NULL DEFAULT NULL COMMENT '存放位置ID',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存放位置名称',
  `internal_codes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '内部编码列表(逗号分隔)',
  `item_count` int NULL DEFAULT 0 COMMENT '明细数量(多少件)',
  `system_quantity` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '系统数量(合计)',
  `actual_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '实盘数量',
  `difference` decimal(10, 2) NULL DEFAULT NULL COMMENT '差异',
  `difference_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '差异说明',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态:0-未盘点,1-已盘点(无差异),2-已盘点(有差异)',
  `checker_id` bigint NULL DEFAULT NULL COMMENT '盘点人ID',
  `check_time` datetime(0) NULL DEFAULT NULL COMMENT '盘点时间',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_check_id`(`check_id`) USING BTREE,
  INDEX `idx_material_id`(`material_id`) USING BTREE,
  INDEX `idx_group_key`(`check_id`, `batch_no`, `location_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '盘点分组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check_group
-- ----------------------------

-- ----------------------------
-- Table structure for stock_check_item
-- ----------------------------
DROP TABLE IF EXISTS `stock_check_item`;
CREATE TABLE `stock_check_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `system_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '系统数量',
  `actual_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '实盘数量',
  `difference` decimal(10, 2) NULL DEFAULT NULL COMMENT '差异',
  `difference_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '差异原因',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0待盘 1已盘 2有差异',
  `checker_id` bigint NULL DEFAULT NULL COMMENT '盘点人ID',
  `check_time` datetime(0) NULL DEFAULT NULL COMMENT '盘点时间',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_check`(`check_id`) USING BTREE,
  INDEX `idx_stock`(`stock_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '盘点明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check_item
-- ----------------------------

-- ----------------------------
-- Table structure for stock_check_item_stock
-- ----------------------------
DROP TABLE IF EXISTS `stock_check_item_stock`;
CREATE TABLE `stock_check_item_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `group_id` bigint NOT NULL COMMENT '所属分组ID',
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `system_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '该库存的系统数量',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_check_id`(`check_id`) USING BTREE,
  INDEX `idx_group_id`(`group_id`) USING BTREE,
  INDEX `idx_stock_id`(`stock_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '盘点明细与库存关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check_item_stock
-- ----------------------------

-- ----------------------------
-- Table structure for stock_in
-- ----------------------------
DROP TABLE IF EXISTS `stock_in`;
CREATE TABLE `stock_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint NULL DEFAULT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '批号',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内部编码',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期',
  `quantity` decimal(10, 2) NOT NULL COMMENT '入库数量',
  `location_id` bigint NULL DEFAULT NULL COMMENT '存放位置ID',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入库原因',
  `supplier_id` bigint NULL DEFAULT NULL COMMENT '供应商ID',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `product_certificate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产品证书文件路径',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stock`(`stock_id`) USING BTREE,
  INDEX `idx_material`(`material_id`) USING BTREE,
  INDEX `idx_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '入库记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_in
-- ----------------------------
INSERT INTO `stock_in` VALUES (1, NULL, 2, '001', '001-001', '2026-03-26', 1.00, 1, 'PURCHASE', 1, NULL, '', '', 1, '2026-03-21 10:46:48');
INSERT INTO `stock_in` VALUES (2, NULL, 2, '001', '001-002', '2026-03-26', 1.00, 1, 'PURCHASE', 1, NULL, '', '', 1, '2026-03-21 10:46:50');
INSERT INTO `stock_in` VALUES (3, NULL, 2, '001', '001-003 ~ 001-007', '2026-03-26', 5.00, 1, 'PURCHASE', 1, NULL, '', '', 1, '2026-03-21 10:47:38');

-- ----------------------------
-- Table structure for stock_out
-- ----------------------------
DROP TABLE IF EXISTS `stock_out`;
CREATE TABLE `stock_out`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内部编码',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '批号',
  `quantity` decimal(10, 2) NOT NULL COMMENT '申请数量',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出库原因',
  `purpose` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用途说明',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '申请时间',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime(0) NULL DEFAULT NULL COMMENT '审批时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stock`(`stock_id`) USING BTREE,
  INDEX `idx_applicant`(`applicant_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_time`(`apply_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '出库申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_out
-- ----------------------------
INSERT INTO `stock_out` VALUES (1, 24, 2, '001-003', '001', 1.00, 'EXPERIMENT', '', 1, '2026-03-21 10:54:50', NULL, NULL, 0, NULL, NULL);
INSERT INTO `stock_out` VALUES (2, 25, 2, '001-004', '001', 1.00, 'EXPERIMENT', '', 1, '2026-03-21 10:54:50', 1, '2026-03-21 10:55:47', 2, 'ces', NULL);
INSERT INTO `stock_out` VALUES (3, 26, 2, '001-005', '001', 1.00, 'EXPERIMENT', '', 1, '2026-03-21 10:54:51', NULL, NULL, 0, NULL, NULL);

-- ----------------------------
-- Table structure for supplier
-- ----------------------------
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商名称',
  `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '供应商表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier
-- ----------------------------
INSERT INTO `supplier` VALUES (1, '中国计量科学研究院', '张经理', '010-64524800', NULL, 1, '2026-03-21 00:14:23', '2026-03-21 00:14:23');
INSERT INTO `supplier` VALUES (2, '国家标准物质研究中心', '李主任', '010-64271730', NULL, 1, '2026-03-21 00:14:23', '2026-03-21 00:14:23');
INSERT INTO `supplier` VALUES (3, '上海市计量测试技术研究院', '王工', '021-54031072', NULL, 1, '2026-03-21 00:14:23', '2026-03-21 00:14:23');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role_id` bigint NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `password_changed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已修改密码',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE,
  INDEX `idx_user_role`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$iFWcTSKrauGO8HK6D9VG4.8JZg94nQVjYnC9u9pOk9Bcp.MCyNCzW', '系统管理员', NULL, NULL, 1, 1, 1, '2026-03-21 00:12:34', '2026-03-21 00:12:34', 0);

SET FOREIGN_KEY_CHECKS = 1;
