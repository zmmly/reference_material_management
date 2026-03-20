# 库存预警逻辑优化设计文档

## 背景

当前库存预警逻辑存在问题：系统按照单条库存记录的 `quantity` 字段判断库存是否不足，但实际上：

- 每条 `stock` 记录代表一个物品实例（有独立的 `internal_code` 内部编号）
- 每条记录的 `quantity` 永远是 1
- 正确的库存预警应该按标准物质汇总在库数量进行判断

## 目标

修改库存预警逻辑，按标准物质（`material_id`）汇总在库件数，当总件数低于阈值时触发预警。

## 设计方案

### 1. 数据库修改

创建新的迁移文件 `database/phase5-alert-internal-codes.sql`：

```sql
-- 为预警记录表添加内部编码聚合字段
ALTER TABLE alert_record
ADD COLUMN `internal_codes` TEXT COMMENT '关联的内部编码列表(逗号分隔)' AFTER `stock_id`;
```

### 2. 后端修改

#### 2.1 AlertRecord 实体类

新增字段（包括数据库字段和前端展示用的非持久化字段）：

```java
// 数据库字段
private String internalCodes;

// 现有的非持久化字段保持不变
@TableField(exist = false)
private String internalCode;
```

**注意**：`internalCodes` 是真实的数据库字段，不需要 `@TableField(exist = false)` 注解。MyBatis-Plus 会自动映射该字段。

#### 2.2 AlertService.checkStockLowAlerts()

修改库存预警检查逻辑：

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
        Long materialId = (Long) item.get("material_id");
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

#### 2.3 新增 createStockLowAlertIfNotExists()

专用于库存预警的记录创建方法：

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

**注意**：原有的 `createAlertIfNotExists()` 保持不变，继续用于过期预警和长期未用预警（这些仍然针对具体库存实例）。

#### 2.4 修改 fillRelations()

更新关联填充方法以正确处理库存预警的内部编码展示：

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

### 3. 前端修改

#### 3.1 预警列表展示

修改内部编码列的展示逻辑，增加列宽以适应聚合编码：

```vue
<el-table-column label="内部编码" min-width="180">
  <template #default="{ row }">
    {{ row.internalCodes || row.internalCode || '-' }}
  </template>
</el-table-column>
```

### 4. 数据示例

| type | stock_id | material_id | internal_codes | content |
|------|----------|-------------|----------------|---------|
| STOCK_LOW | null | 5 | BM-2024-001, BM-2024-002, BM-2024-003 | 【苯酚标准溶液】库存不足，当前库存: 3 件（阈值: 5 件） |
| EXPIRY_WARNING | 102 | 5 | null | 【苯酚标准溶液】将在 15 天后过期 |
| UNUSED | 103 | 8 | null | 【甲醇】已超过 6 个月未使用 |

## 影响范围

- 数据库：新增 `database/phase5-alert-internal-codes.sql`，`alert_record` 表新增字段
- 后端：`AlertRecord.java`、`AlertService.java`
- 前端：`frontend/src/views/alert/index.vue`

## 测试场景

### 基本功能测试

1. 设置库存预警阈值为某个值（如 5）
2. 准备一个标准物质，在库数量低于阈值（如 3 件）
3. 执行预警检查，验证是否生成库存预警
4. 验证预警记录的 `internal_codes` 是否正确聚合
5. 验证前端列表正确展示聚合的内部编码

### 边界条件测试

1. **空结果**：在库数量为 0 的物质不会触发预警
2. **刚好等于阈值**：数量等于阈值时不触发预警（条件是 `<=`）
3. **已删除物质**：验证 `material_id` 引用已删除物质时的处理
4. **多个预警**：两个不同物质都低于阈值时，验证都生成预警
5. **预警处理后**：处理预警后，如果库存仍低，再次检查应生成新预警；如果库存已补充，则不再生成

### 空值处理测试

1. **内部编码为 null**：部分库存记录的 `internal_code` 为 null 时，应被过滤掉
2. **全部为空**：所有库存的 `internal_code` 都为空时，`internal_codes` 应为空字符串
