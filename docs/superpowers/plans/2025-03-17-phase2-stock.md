# 标准物质管理系统 - 第二阶段实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan.

**Goal:** 实现标准物质主数据管理、库存管理、入库管理、出库申请与审核

**Architecture:** 基于第一阶段的框架，添加 ReferenceMaterial、Stock、StockIn、StockOut 实体和相关服务

**Tech Stack:** Spring Boot 3.x + MyBatis-Plus + Vue 3 + Element Plus

---

## 文件结构

### 后端新增文件
```
backend/src/main/java/com/rmm/
├── entity/
│   ├── ReferenceMaterial.java       # 标准物质主数据
│   ├── Stock.java                   # 库存记录
│   ├── StockIn.java                 # 入库单
│   └── StockOut.java                # 出库单
├── mapper/
│   ├── ReferenceMaterialMapper.java
│   ├── StockMapper.java
│   ├── StockInMapper.java
│   └── StockOutMapper.java
├── service/
│   ├── ReferenceMaterialService.java
│   ├── StockService.java
│   ├── StockInService.java
│   └── StockOutService.java
├── controller/
│   ├── ReferenceMaterialController.java
│   ├── StockController.java
│   ├── StockInController.java
│   └── StockOutController.java
├── dto/
│   ├── ReferenceMaterialDTO.java
│   ├── StockQueryDTO.java
│   ├── StockInDTO.java
│   └── StockOutDTO.java
└── vo/
    ├── ReferenceMaterialVO.java
    ├── StockVO.java
    ├── StockInVO.java
    └── StockOutVO.java
```

### 前端新增文件
```
frontend/src/views/
├── reference-material/              # 标准物质管理
│   └── index.vue
├── stock/                           # 库存管理
│   └── index.vue
├── stock-in/                        # 入库管理
│   └── index.vue
└── stock-out/                       # 出库管理
│   ├── index.vue                    # 出库申请
│   └── audit.vue                    # 出库审核
```

---

## Task 1: 数据库表扩展

**Files:**
- Create: `database/phase2-tables.sql`

- [ ] **Step 1: 创建标准物质主数据表**

```sql
-- database/phase2-tables.sql

USE reference_material_management;

-- 标准物质主数据表
CREATE TABLE `reference_material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '编号',
    `name` VARCHAR(200) NOT NULL COMMENT '名称',
    `english_name` VARCHAR(200) COMMENT '英文名',
    `standard_value` VARCHAR(500) COMMENT '标准值',
    `uncertainty` VARCHAR(100) COMMENT '不确定度',
    `specification` VARCHAR(200) COMMENT '规格',
    `unit` VARCHAR(50) COMMENT '单位',
    `category_id` BIGINT COMMENT '分类ID',
    `storage_condition` VARCHAR(100) COMMENT '储存条件',
    `manufacturer` VARCHAR(200) COMMENT '生产厂商',
    `remarks` VARCHAR(500) COMMENT '备注',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_category` (`category_id`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB COMMENT='标准物质主数据表';

-- 库存表
CREATE TABLE `stock` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `batch_no` VARCHAR(100) COMMENT '批号',
    `internal_code` VARCHAR(50) COMMENT '内部编码',
    `expiry_date` DATE COMMENT '有效期',
    `quantity` DECIMAL(10,2) DEFAULT 0 COMMENT '库存数量',
    `location_id` BIGINT COMMENT '存放位置ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常 2即将过期 3已过期',
    `last_out_time` DATETIME COMMENT '最后出库时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_material` (`material_id`),
    INDEX `idx_location` (`location_id`),
    INDEX `idx_expiry` (`expiry_date`),
    INDEX `idx_internal_code` (`internal_code`)
) ENGINE=InnoDB COMMENT='库存表';

-- 入库记录表
CREATE TABLE `stock_in` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stock_id` BIGINT COMMENT '库存ID(新增时为空)',
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `batch_no` VARCHAR(100) COMMENT '批号',
    `internal_code` VARCHAR(50) COMMENT '内部编码',
    `expiry_date` DATE COMMENT '有效期',
    `quantity` DECIMAL(10,2) NOT NULL COMMENT '入库数量',
    `location_id` BIGINT COMMENT '存放位置ID',
    `reason` VARCHAR(50) COMMENT '入库原因',
    `supplier_id` BIGINT COMMENT '供应商ID',
    `price` DECIMAL(10,2) COMMENT '单价',
    `remarks` VARCHAR(500) COMMENT '备注',
    `operator_id` BIGINT COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_stock` (`stock_id`),
    INDEX `idx_material` (`material_id`),
    INDEX `idx_time` (`create_time`)
) ENGINE=InnoDB COMMENT='入库记录表';

-- 出库申请表
CREATE TABLE `stock_out` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `stock_id` BIGINT NOT NULL COMMENT '库存ID',
    `material_id` BIGINT NOT NULL COMMENT '标准物质ID',
    `quantity` DECIMAL(10,2) NOT NULL COMMENT '申请数量',
    `reason` VARCHAR(50) COMMENT '出库原因',
    `purpose` VARCHAR(500) COMMENT '用途说明',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approve_time` DATETIME COMMENT '审批时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已撤回',
    `reject_reason` VARCHAR(500) COMMENT '拒绝原因',
    `remarks` VARCHAR(500) COMMENT '备注',
    INDEX `idx_stock` (`stock_id`),
    INDEX `idx_applicant` (`applicant_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_time` (`apply_time`)
) ENGINE=InnoDB COMMENT='出库申请表';

-- 供应商表
CREATE TABLE `supplier` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `contact` VARCHAR(50) COMMENT '联系人',
    `phone` VARCHAR(20) COMMENT '电话',
    `address` VARCHAR(500) COMMENT '地址',
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='供应商表';
```

- [ ] **Step 2: Commit**

```bash
git add database/phase2-tables.sql
git commit -m "feat(db): add phase2 tables for stock management"
```

---

## Task 2: 标准物质主数据后端

**Files:**
- Create: `backend/src/main/java/com/rmm/entity/ReferenceMaterial.java`
- Create: `backend/src/main/java/com/rmm/mapper/ReferenceMaterialMapper.java`
- Create: `backend/src/main/java/com/rmm/service/ReferenceMaterialService.java`
- Create: `backend/src/main/java/com/rmm/controller/ReferenceMaterialController.java`

详细代码见实施...
