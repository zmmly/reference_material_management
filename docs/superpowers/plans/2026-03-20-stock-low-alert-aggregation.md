# 库存预警聚合逻辑实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修改库存预警逻辑，按标准物质汇总在库件数进行预警判断，并聚合展示内部编码列表。

**Architecture:** 在现有预警系统基础上，修改 `checkStockLowAlerts()` 方法使用 SQL 聚合查询，新增 `createStockLowAlertIfNotExists()` 方法，更新 `fillRelations()` 方法支持库存预警类型，前端展示层适配聚合编码显示。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Vue 3, Element Plus, MySQL

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `database/phase5-alert-internal-codes.sql` | 创建 | 数据库迁移脚本，新增 internal_codes 字段 |
| `backend/.../entity/AlertRecord.java` | 修改 | 新增 internalCodes 字段 |
| `backend/.../service/AlertService.java` | 修改 | 重构库存预警逻辑，新增方法，更新 fillRelations |
| `frontend/src/views/alert/index.vue` | 修改 | 适配聚合内部编码展示 |

---

## Chunk 1: 数据库与实体层

### Task 1: 数据库迁移脚本

**Files:**
- Create: `database/phase5-alert-internal-codes.sql`

- [ ] **Step 1: 创建迁移文件**

```sql
-- 为预警记录表添加内部编码聚合字段
ALTER TABLE alert_record
ADD COLUMN `internal_codes` TEXT COMMENT '关联的内部编码列表(逗号分隔)' AFTER `stock_id`;
```

- [ ] **Step 2: 执行迁移脚本**

```bash
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/phase5-alert-internal-codes.sql
```

- [ ] **Step 3: 验证字段已添加**

```bash
docker exec -it mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 -e "DESCRIBE reference_material_management.alert_record;"
```

Expected: 输出包含 `internal_codes` 字段

- [ ] **Step 4: 提交**

```bash
git add database/phase5-alert-internal-codes.sql
git commit -m "feat(db): 添加 alert_record 表 internal_codes 字段"
```

---

### Task 2: 更新 AlertRecord 实体类

**Files:**
- Modify: `backend/src/main/java/com/rmm/entity/AlertRecord.java`

- [ ] **Step 1: 添加 internalCodes 字段**

在现有字段后添加：

```java
private String internalCodes;
```

完整修改后的 AlertRecord.java：

```java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private Long stockId;
    private String internalCodes;  // 新增字段
    private Long materialId;
    private String content;
    private Integer level;
    private Integer status;
    private Long handlerId;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String internalCode;
    @TableField(exist = false)
    private String handlerName;
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/rmm/entity/AlertRecord.java
git commit -m "feat(entity): AlertRecord 添加 internalCodes 字段"
```

---

## Chunk 2: 后端服务层

### Task 3: 重构 AlertService - checkStockLowAlerts 方法

**Files:**
- Modify: `backend/src/main/java/com/rmm/service/AlertService.java`

- [ ] **Step 1: 添加必要的导入**

在文件顶部的 import 区域添加：

```java
import java.util.Objects;
import java.util.stream.Collectors;
```

- [ ] **Step 2: 重写 checkStockLowAlerts 方法**

替换原有的 `checkStockLowAlerts()` 方法：

```java
private void checkStockLowAlerts() {
    AlertConfig config = getConfig("STOCK_LOW");
    if (config == null || config.getEnabled() != 1) return;

    // 按标准物质汇总在库数量
    List<Map<String, Object>> lowStockMaterials = stockMapper.selectMaps(
        new LambdaQueryWrapper<Stock>()
            .select("material_id", "COUNT(*) as total_count")
            .gt(Stock::getQuantity, BigDecimal.ZERO)
            .eq(Stock::getStatus, 1)  // 在库状态
            .groupBy(Stock::getMaterialId)
            .having("COUNT(*) <= {0}", config.getThreshold())
    );

    for (Map<String, Object> item : lowStockMaterials) {
        Long materialId = ((Number) item.get("material_id")).longValue();
        Long totalCount = ((Number) item.get("total_count")).longValue();

        // 查询该物质所有在库的内部编码
        List<Stock> stocks = stockMapper.selectList(
            new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMaterialId, materialId)
                .gt(Stock::getQuantity, BigDecimal.ZERO)
                .eq(Stock::getStatus, 1)
        );
        // 过滤掉 null 和空字符串的内部编码
        String internalCodes = stocks.stream()
            .map(Stock::getInternalCode)
            .filter(Objects::nonNull)
            .filter(code -> !code.isEmpty())
            .collect(Collectors.joining(", "));

        createStockLowAlertIfNotExists(materialId, totalCount, config.getThreshold(), internalCodes);
    }
}
```

- [ ] **Step 3: 提交中间状态**

```bash
git add backend/src/main/java/com/rmm/service/AlertService.java
git commit -m "refactor(service): 重构 checkStockLowAlerts 使用聚合查询"
```

---

### Task 4: 新增 createStockLowAlertIfNotExists 方法

**Files:**
- Modify: `backend/src/main/java/com/rmm/service/AlertService.java`

- [ ] **Step 1: 添加新方法**

在 `checkStockLowAlerts()` 方法后添加：

```java
private void createStockLowAlertIfNotExists(Long materialId, Long totalCount,
                                             Integer threshold, String internalCodes) {
    Long existing = alertRecordMapper.selectCount(
        new LambdaQueryWrapper<AlertRecord>()
            .eq(AlertRecord::getType, "STOCK_LOW")
            .eq(AlertRecord::getMaterialId, materialId)
            .eq(AlertRecord::getStatus, 0)
    );
    if (existing > 0) return;

    AlertRecord record = new AlertRecord();
    record.setType("STOCK_LOW");
    record.setStockId(null);
    record.setMaterialId(materialId);
    record.setInternalCodes(internalCodes);
    record.setContent(String.format("【%s】库存不足，当前库存: %d 件（阈值: %d 件）",
        getMaterialName(materialId), totalCount, threshold));
    record.setLevel(2);
    record.setStatus(0);
    alertRecordMapper.insert(record);
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/rmm/service/AlertService.java
git commit -m "feat(service): 添加 createStockLowAlertIfNotExists 方法"
```

---

### Task 5: 更新 fillRelations 方法

**Files:**
- Modify: `backend/src/main/java/com/rmm/service/AlertService.java`

- [ ] **Step 1: 重写 fillRelations 方法**

替换原有的 `fillRelations()` 方法：

```java
private void fillRelations(AlertRecord record) {
    if (record.getMaterialId() != null) {
        ReferenceMaterial material = materialMapper.selectById(record.getMaterialId());
        if (material != null) {
            record.setMaterialName(material.getName());
        }
    }

    // 对于库存预警，使用数据库中存储的 internalCodes
    // 对于其他预警类型，从 stock 表查询单个 internalCode
    if ("STOCK_LOW".equals(record.getType())) {
        // internalCodes 已从数据库加载，无需额外处理
        // 如果需要兼容旧数据（internalCodes 为空），可按 materialId 查询
        if (record.getInternalCodes() == null || record.getInternalCodes().isEmpty()) {
            // 兼容处理：查询该物质所有在库的内部编码
            List<Stock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getMaterialId, record.getMaterialId())
                    .gt(Stock::getQuantity, BigDecimal.ZERO)
                    .eq(Stock::getStatus, 1)
            );
            String codes = stocks.stream()
                .map(Stock::getInternalCode)
                .filter(Objects::nonNull)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.joining(", "));
            record.setInternalCodes(codes);
        }
    } else if (record.getStockId() != null) {
        Stock stock = stockMapper.selectById(record.getStockId());
        if (stock != null) {
            record.setInternalCode(stock.getInternalCode());
        }
    }

    if (record.getHandlerId() != null) {
        User user = userMapper.selectById(record.getHandlerId());
        if (user != null) {
            record.setHandlerName(user.getRealName());
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/rmm/service/AlertService.java
git commit -m "feat(service): 更新 fillRelations 支持库存预警类型"
```

---

### Task 6: 编译验证后端

- [ ] **Step 1: 编译后端项目**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 如果编译失败，检查并修复**

常见问题：
- 缺少 `Objects` 导入
- 缺少 `Collectors` 导入

---

## Chunk 3: 前端展示层

### Task 7: 更新预警列表展示

**Files:**
- Modify: `frontend/src/views/alert/index.vue`

- [ ] **Step 1: 修改内部编码列**

找到内部编码列的定义，修改为：

```vue
<el-table-column label="内部编码" min-width="180">
  <template #default="{ row }">
    {{ row.internalCodes || row.internalCode || '-' }}
  </template>
</el-table-column>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/alert/index.vue
git commit -m "feat(frontend): 适配库存预警聚合内部编码展示"
```

---

## Chunk 4: 集成测试

### Task 8: 执行数据库迁移并启动服务

- [ ] **Step 1: 确保数据库迁移已执行**

```bash
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/phase5-alert-internal-codes.sql
```

- [ ] **Step 2: 启动后端服务**

```bash
cd backend && mvn spring-boot:run &
```

- [ ] **Step 3: 启动前端服务**

```bash
cd frontend && npm run dev -- --port 3002 &
```

- [ ] **Step 4: 验证服务启动**

- 后端: http://localhost:8080/doc.html 可访问
- 前端: http://localhost:3002 可访问

---

### Task 9: 功能验证

- [ ] **Step 1: 登录系统**

使用 admin / admin123 登录

- [ ] **Step 2: 检查预警中心**

1. 导航到预警中心页面
2. 确认页面正常加载，无控制台错误
3. 查看库存预警记录，验证内部编码展示

- [ ] **Step 3: 手动触发预警检查（可选）**

如果有 API 可以手动触发预警检查，执行并验证结果。

或者等待定时任务（每天 8:00 执行）

---

### Task 10: 最终提交

- [ ] **Step 1: 确认所有修改已完成**

```bash
git status
```

Expected: 所有文件已提交

- [ ] **Step 2: 推送代码（如需要）**

```bash
git push origin main
```

---

## 回滚方案

如果出现问题，可以执行以下回滚步骤：

1. **数据库回滚**：
```sql
ALTER TABLE alert_record DROP COLUMN internal_codes;
```

2. **代码回滚**：
```bash
git revert HEAD~6  # 回滚最近 6 个提交
```
