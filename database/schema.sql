-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: reference_material_management
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alert_config`
--

DROP TABLE IF EXISTS `alert_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型: EXPIRY/STOCK_LOW/UNUSED',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警名称',
  `threshold` int DEFAULT NULL COMMENT '阈值(天数/数量)',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_type` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='预警配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alert_record`
--

DROP TABLE IF EXISTS `alert_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预警类型',
  `stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `internal_codes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '关联的内部编码列表(逗号分隔)',
  `material_id` bigint DEFAULT NULL COMMENT '标准物质ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预警内容',
  `level` tinyint DEFAULT '1' COMMENT '预警级别: 1普通 2重要 3紧急',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0未处理 1已处理 2已忽略',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='预警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `backup_record`
--

DROP TABLE IF EXISTS `backup_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `backup_time` datetime NOT NULL COMMENT '备份时间',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人姓名',
  PRIMARY KEY (`id`),
  KEY `idx_backup_time` (`backup_time` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统备份记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父级ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category_parent` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='标准物质分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `location`
--

DROP TABLE IF EXISTS `location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '位置编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '位置名称',
  `temperature` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '温度要求',
  `capacity` int DEFAULT NULL COMMENT '容量',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='存放位置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `metadata`
--

DROP TABLE IF EXISTS `metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: STOCK_IN_REASON/STOCK_OUT_REASON/STORAGE_CONDITION',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_type_code` (`type`,`code`) USING BTREE,
  KEY `idx_metadata_type` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='元数据配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块',
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '操作详情',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_log_user` (`user_id`) USING BTREE,
  KEY `idx_log_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase`
--

DROP TABLE IF EXISTS `purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购编号',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格',
  `batch_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `unit` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '支' COMMENT '单位（支、瓶、盒等）',
  `quantity` decimal(10,2) NOT NULL COMMENT '采购数量',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `estimated_price` decimal(10,2) DEFAULT NULL COMMENT '预估单价',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '金额（采购数量*预估单价）',
  `estimated_arrival_date` date DEFAULT NULL COMMENT '预计到货日期',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购原因',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回 4已到货',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_material` (`material_id`) USING BTREE,
  KEY `idx_applicant` (`applicant_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`apply_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='采购申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_acceptance`
--

DROP TABLE IF EXISTS `purchase_acceptance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_acceptance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_id` bigint NOT NULL COMMENT '采购申请ID',
  `purchase_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购申请单号',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `material_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标准物质名称',
  `material_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标准物质编码',
  `specification` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格',
  `batch_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `quantity` decimal(10,2) DEFAULT NULL COMMENT '采购数量',
  `actual_quantity` decimal(10,2) DEFAULT NULL COMMENT '实际到货数量',
  `expiry_date` date DEFAULT NULL COMMENT '有效期',
  `location_id` bigint DEFAULT NULL COMMENT '存放位置ID',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `estimated_price` decimal(10,2) DEFAULT NULL COMMENT '预估单价',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `package_intact` tinyint DEFAULT NULL COMMENT '外包装是否完好: 0否 1是',
  `label_complete` tinyint DEFAULT NULL COMMENT '标签是否完整: 0否 1是',
  `has_damage` tinyint DEFAULT NULL COMMENT '有无破损: 0否 1是',
  `acceptance_result` tinyint DEFAULT NULL COMMENT '验收结果: 0待验收 1通过 2拒绝',
  `acceptance_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '验收备注',
  `acceptance_date` datetime DEFAULT NULL COMMENT '验收日期',
  `acceptance_user_id` bigint DEFAULT NULL COMMENT '验收人ID',
  `acceptance_user_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '验收人姓名',
  `stock_in_id` bigint DEFAULT NULL COMMENT '生成的入库单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_purchase_no` (`purchase_no`),
  KEY `idx_status` (`acceptance_result`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购验收表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reference_material`
--

DROP TABLE IF EXISTS `reference_material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reference_material` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编号',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `english_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '英文名',
  `cas_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'CAS号',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格',
  `purity_concentration` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '纯度/浓度',
  `matrix` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '基质',
  `package_form` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '包装形式',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE,
  UNIQUE KEY `uk_cas_supplier` (`cas_number`,`supplier_id`) COMMENT 'CAS号和供应商的复合唯一索引',
  KEY `idx_category` (`category_id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='标准物质主数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `permissions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '权限列表(JSON)',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock`
--

DROP TABLE IF EXISTS `stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内部编码',
  `expiry_date` date DEFAULT NULL COMMENT '有效期',
  `quantity` decimal(10,2) DEFAULT '0.00' COMMENT '库存数量',
  `location_id` bigint DEFAULT NULL COMMENT '存放位置ID',
  `status` tinyint DEFAULT '1' COMMENT '状态: 1正常 2即将过期 3已过期',
  `last_out_time` datetime DEFAULT NULL COMMENT '最后出库时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_material` (`material_id`) USING BTREE,
  KEY `idx_location` (`location_id`) USING BTREE,
  KEY `idx_expiry` (`expiry_date`) USING BTREE,
  KEY `idx_internal_code` (`internal_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='库存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check`
--

DROP TABLE IF EXISTS `stock_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点单号',
  `check_date` date NOT NULL COMMENT '盘点日期',
  `scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点范围: ALL/CATEGORY/LOCATION',
  `scope_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '范围值',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0进行中 1已完成 2已作废',
  `total_count` int DEFAULT '0' COMMENT '总项数',
  `checked_count` int DEFAULT '0' COMMENT '已盘项数',
  `difference_count` int DEFAULT '0' COMMENT '差异项数',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `checker_id` bigint DEFAULT NULL COMMENT '盘点人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_check_no` (`check_no`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_date` (`check_date`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='盘点任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check_adjust`
--

DROP TABLE IF EXISTS `stock_check_adjust`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_adjust` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_item_id` bigint NOT NULL COMMENT '盘点明细ID',
  `adjust_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调整类型: SURPLUS/LOSS',
  `adjust_quantity` decimal(10,2) DEFAULT NULL COMMENT '调整数量',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调整原因',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check_item` (`check_item_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='盘点差异处理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check_group`
--

DROP TABLE IF EXISTS `stock_check_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '批号',
  `location_id` bigint DEFAULT NULL COMMENT '存放位置ID',
  `location_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '存放位置名称',
  `internal_codes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '内部编码列表(逗号分隔)',
  `item_count` int DEFAULT '0' COMMENT '明细数量(多少件)',
  `system_quantity` decimal(10,2) DEFAULT '0.00' COMMENT '系统数量(合计)',
  `actual_quantity` decimal(10,2) DEFAULT NULL COMMENT '实盘数量',
  `difference` decimal(10,2) DEFAULT NULL COMMENT '差异',
  `difference_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '差异说明',
  `status` tinyint DEFAULT '0' COMMENT '状态:0-未盘点,1-已盘点(无差异),2-已盘点(有差异)',
  `checker_id` bigint DEFAULT NULL COMMENT '盘点人ID',
  `check_time` datetime DEFAULT NULL COMMENT '盘点时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check_id` (`check_id`) USING BTREE,
  KEY `idx_material_id` (`material_id`) USING BTREE,
  KEY `idx_group_key` (`check_id`,`batch_no`,`location_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='盘点分组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check_item`
--

DROP TABLE IF EXISTS `stock_check_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `system_quantity` decimal(10,2) DEFAULT NULL COMMENT '系统数量',
  `actual_quantity` decimal(10,2) DEFAULT NULL COMMENT '实盘数量',
  `difference` decimal(10,2) DEFAULT NULL COMMENT '差异',
  `difference_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '差异原因',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0待盘 1已盘 2有差异',
  `checker_id` bigint DEFAULT NULL COMMENT '盘点人ID',
  `check_time` datetime DEFAULT NULL COMMENT '盘点时间',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check` (`check_id`) USING BTREE,
  KEY `idx_stock` (`stock_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='盘点明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_check_item_stock`
--

DROP TABLE IF EXISTS `stock_check_item_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_check_item_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL COMMENT '盘点任务ID',
  `group_id` bigint NOT NULL COMMENT '所属分组ID',
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `system_quantity` decimal(10,2) DEFAULT NULL COMMENT '该库存的系统数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_check_id` (`check_id`) USING BTREE,
  KEY `idx_group_id` (`group_id`) USING BTREE,
  KEY `idx_stock_id` (`stock_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='盘点明细与库存关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_in`
--

DROP TABLE IF EXISTS `stock_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_in` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint DEFAULT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内部编码',
  `expiry_date` date DEFAULT NULL COMMENT '有效期',
  `quantity` decimal(10,2) NOT NULL COMMENT '入库数量',
  `location_id` bigint DEFAULT NULL COMMENT '存放位置ID',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库原因',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `product_certificate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品证书文件路径',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stock` (`stock_id`) USING BTREE,
  KEY `idx_material` (`material_id`) USING BTREE,
  KEY `idx_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='入库记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_out`
--

DROP TABLE IF EXISTS `stock_out`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_out` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_id` bigint NOT NULL COMMENT '库存ID',
  `material_id` bigint NOT NULL COMMENT '标准物质ID',
  `internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内部编码',
  `batch_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批号',
  `quantity` decimal(10,2) NOT NULL COMMENT '申请数量',
  `reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出库原因',
  `purpose` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用途说明',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `remarks` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stock` (`stock_id`) USING BTREE,
  KEY `idx_applicant` (`applicant_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`apply_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='出库申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商名称',
  `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `status` tinyint DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='供应商表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `status` tinyint DEFAULT '1',
  `password_changed` tinyint(1) DEFAULT '0' COMMENT '是否已修改密码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username` (`username`) USING BTREE,
  KEY `idx_user_role` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-22 22:05:01
