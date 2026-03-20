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

在 `alert_record` 表新增字段存储聚合的内部编码：

```sql
ALTER TABLE alert_record
ADD COLUMN `internal_codes` TEXT COMMENT '关联的内部编码列表(逗号分隔)' AFTER `stock_id`;
```

### 2. 后端修改

#### 2.1 AlertRecord 实体类

新增字段：

```java
private String internalCodes;
```

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
        String internalCodes = stocks.stream()
            .map(Stock::getInternalCode)
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

### 3. 前端修改

#### 3.1 预警列表展示

修改内部编码列的展示逻辑，优先显示 `internalCodes`（库存预警的聚合编码），否则显示 `internalCode`（其他预警类型的单个编码）：

```vue
<el-table-column label="内部编码" prop="internalCodes">
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

- 数据库：`alert_record` 表新增字段
- 后端：`AlertRecord.java`、`AlertService.java`
- 前端：`frontend/src/views/alert/index.vue`

## 测试要点

1. 设置库存预警阈值为某个值（如 5）
2. 准备一个标准物质，在库数量低于阈值（如 3 件）
3. 执行预警检查，验证是否生成库存预警
4. 验证预警记录的 `internal_codes` 是否正确聚合
5. 验证前端列表正确展示聚合的内部编码
