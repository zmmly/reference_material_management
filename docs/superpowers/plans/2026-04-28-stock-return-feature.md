# 出库归还功能 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在出库申请时让用户选择是否需要归还，审批通过后物品状态变为"借出/待归还"，之后可通过一键归还恢复为"在库"。

**Architecture:** 出库表增加 `need_return` 和 `returned` 字段；stock 状态增加 4="借出/待归还"；审批通过时根据 `need_return` 决定 stock.status=0(已出库) 或 4(借出)；新增归还 API 恢复原 stock 记录并创建入库记录作为审计追踪。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Element Plus

---

## File Structure

| Action | File | Responsibility |
|--------|------|---------------|
| Modify | `database/schema.sql` | 数据库结构更新（字段注释） |
| Create | `database/migration-add-return-fields.sql` | 迁移SQL |
| Modify | `backend/.../entity/StockOut.java` | 增加 needReturn、returned 字段 |
| Modify | `backend/.../entity/StockIn.java` | 增加 stockOutId 字段 |
| Modify | `backend/.../service/StockOutService.java` | apply/batchApply/approve 支持归还标记 |
| Modify | `backend/.../controller/StockOutController.java` | 新增待归还列表和归还API |
| Modify | `frontend/src/api/stock.js` | 新增前端API调用 |
| Modify | `frontend/src/views/stock-out/apply.vue` | 出库申请增加"是否需要归还" |
| Modify | `frontend/src/views/stock/index.vue` | 批量出库增加"是否需要归还" |
| Modify | `frontend/src/views/stock-out/index.vue` | 增加"待归还"tab和归还操作 |

---

## Chunk 1: Database & Backend Entity

### Task 1: 数据库迁移

**Files:**
- Create: `database/migration-add-return-fields.sql`
- Modify: `database/schema.sql`

- [ ] **Step 1: 创建迁移SQL文件**

```sql
-- database/migration-add-return-fields.sql
-- 出库归还功能数据库迁移

-- stock_out 表增加 need_return 和 returned 字段
ALTER TABLE `stock_out`
  ADD COLUMN `need_return` tinyint(1) DEFAULT 0 COMMENT '是否需要归还: 0否 1是' AFTER `remarks`,
  ADD COLUMN `returned` tinyint(1) DEFAULT 0 COMMENT '是否已归还: 0否 1是' AFTER `need_return`;

-- stock_in 表增加 stock_out_id 字段
ALTER TABLE `stock_in`
  ADD COLUMN `stock_out_id` bigint DEFAULT NULL COMMENT '关联的出库记录ID' AFTER `stock_id`;

-- 更新 stock 表状态注释
ALTER TABLE `stock` MODIFY COLUMN `status` tinyint DEFAULT '1' COMMENT '状态: 0已出库 1正常 2即将过期 3已过期 4借出/待归还';
```

- [ ] **Step 2: 同步更新 schema.sql**

在 `stock_out` 表的 `remarks` 字段后添加：
```sql
`need_return` tinyint(1) DEFAULT 0 COMMENT '是否需要归还: 0否 1是',
`returned` tinyint(1) DEFAULT 0 COMMENT '是否已归还: 0否 1是',
```

在 `stock_in` 表的 `stock_id` 字段后添加：
```sql
`stock_out_id` bigint DEFAULT NULL COMMENT '关联的出库记录ID',
```

更新 `stock` 表的 `status` 字段注释。

- [ ] **Step 3: 执行迁移SQL**

```bash
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/migration-add-return-fields.sql
```

- [ ] **Step 4: Commit**

```bash
git add database/migration-add-return-fields.sql database/schema.sql
git commit -m "feat: add need_return and returned fields for stock return feature"
```

### Task 2: 更新后端 Entity

**Files:**
- Modify: `backend/src/main/java/com/rmm/entity/StockOut.java`
- Modify: `backend/src/main/java/com/rmm/entity/StockIn.java`

- [ ] **Step 1: StockOut.java 增加 needReturn 和 returned 字段**

在 `remarks` 字段后添加：
```java
private Boolean needReturn;
private Boolean returned;
```

在 `@TableField(exist = false)` 虚拟字段区域添加：
```java
@TableField(exist = false)
private String returnStatusText;
```

- [ ] **Step 2: StockIn.java 增加 stockOutId 字段**

在 `stockId` 字段后添加：
```java
private Long stockOutId;
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/rmm/entity/StockOut.java backend/src/main/java/com/rmm/entity/StockIn.java
git commit -m "feat: add needReturn, returned, stockOutId fields to entities"
```

---

## Chunk 2: Backend Service & Controller

### Task 3: 修改 StockOutService 支持归还标记

**Files:**
- Modify: `backend/src/main/java/com/rmm/service/StockOutService.java`

- [ ] **Step 1: 修改 apply() 方法**

在 `apply()` 方法中，在 `stockOut.setStatus(0)` 之前，增加 needReturn 的处理：

```java
// 将 stockOut.setStatus(0); 行之前添加：
// needReturn 默认为 false，由前端传入
if (stockOut.getNeedReturn() == null) {
    stockOut.setNeedReturn(false);
}
```

- [ ] **Step 2: 修改 batchApply() 方法签名，增加 needReturn 参数**

方法签名改为：
```java
public void batchApply(List<Long> stockIds, String reason, String purpose, Boolean needReturn, Long applicantId)
```

在循环中创建 StockOut 时设置：
```java
stockOut.setNeedReturn(needReturn != null && needReturn);
```

- [ ] **Step 3: 修改 approve() 方法**

在 `// 标记库存为已出库` 处，根据 needReturn 决定 stock 状态：

```java
// 替换原来的 stock.setStatus(0);
if (Boolean.TRUE.equals(stockOut.getNeedReturn())) {
    stock.setStatus(4);  // 借出/待归还
} else {
    stock.setStatus(0);  // 已出库
}
```

- [ ] **Step 4: 新增 pendingReturns() 方法**

```java
/**
 * 获取待归还列表
 */
public PageResult<StockOut> pendingReturns(Integer current, Integer size) {
    Page<StockOut> page = new Page<>(current, size);
    LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(StockOut::getStatus, 1)           // 已审批通过
           .eq(StockOut::getNeedReturn, true)     // 需要归还
           .eq(StockOut::getReturned, false)      // 未归还
           .orderByDesc(StockOut::getApproveTime);

    Page<StockOut> result = stockOutMapper.selectPage(page, wrapper);
    result.getRecords().forEach(this::fillRelations);

    PageResult<StockOut> pageResult = new PageResult<>();
    pageResult.setRecords(result.getRecords());
    pageResult.setTotal(result.getTotal());
    pageResult.setSize(result.getSize());
    pageResult.setCurrent(result.getCurrent());
    pageResult.setPages(result.getPages());
    return pageResult;
}
```

- [ ] **Step 5: 新增 returnStock() 归还方法**

```java
/**
 * 归还出库物品
 */
@Transactional
public void returnStock(Long stockOutId, Long operatorId) {
    StockOut stockOut = stockOutMapper.selectById(stockOutId);
    if (stockOut == null) {
        throw new BusinessException("出库记录不存在");
    }
    if (stockOut.getStatus() != 1) {
        throw new BusinessException("该出库申请未审批通过");
    }
    if (!Boolean.TRUE.equals(stockOut.getNeedReturn())) {
        throw new BusinessException("该出库申请不需要归还");
    }
    if (Boolean.TRUE.equals(stockOut.getReturned())) {
        throw new BusinessException("该物品已归还");
    }

    // 恢复库存记录
    Stock stock = stockMapper.selectById(stockOut.getStockId());
    if (stock == null) {
        throw new BusinessException("库存记录不存在");
    }
    stock.setStatus(1);  // 恢复为正常
    stockMapper.updateById(stock);

    // 标记出库记录为已归还
    stockOut.setReturned(true);
    stockOutMapper.updateById(stockOut);

    // 创建入库记录（审计追踪）
    StockIn stockIn = new StockIn();
    stockIn.setStockOutId(stockOutId);
    stockIn.setStockId(stock.getId());
    stockIn.setMaterialId(stock.getMaterialId());
    stockIn.setBatchNo(stock.getBatchNo());
    stockIn.setInternalCode(stock.getInternalCode());
    stockIn.setPurityConcentration(stock.getPurityConcentration());
    stockIn.setExpiryDate(stock.getExpiryDate());
    stockIn.setQuantity(BigDecimal.ONE);
    stockIn.setLocationId(stock.getLocationId());
    stockIn.setReason("RETURN");
    stockIn.setOperatorId(operatorId);
    stockInMapper.insert(stockIn);
}
```

需要在类中注入 StockInMapper（已有）。

- [ ] **Step 6: 更新 fillRelations() 添加归还状态文本**

在 fillRelations 方法末尾添加：
```java
// 设置归还状态文本
if (Boolean.TRUE.equals(stockOut.getNeedReturn())) {
    stockOut.setReturnStatusText(Boolean.TRUE.equals(stockOut.getReturned()) ? "已归还" : "待归还");
} else {
    stockOut.setReturnStatusText("无需归还");
}
```

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/rmm/service/StockOutService.java
git commit -m "feat: add return logic to StockOutService"
```

### Task 4: 修改 StockOutController 增加归还API

**Files:**
- Modify: `backend/src/main/java/com/rmm/controller/StockOutController.java`

- [ ] **Step 1: 修改 batchApply 接口接收 needReturn 参数**

在 `batchApply` 方法中提取 needReturn：
```java
Boolean needReturn = params.get("needReturn") != null && Boolean.parseBoolean(params.get("needReturn").toString());
```

修改调用：
```java
stockOutService.batchApply(stockIds, reason, purpose, needReturn, userId);
```

- [ ] **Step 2: 新增待归还列表接口**

```java
@GetMapping("/pending-returns")
public Result<PageResult<StockOut>> pendingReturns(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(stockOutService.pendingReturns(current, size));
}
```

- [ ] **Step 3: 新增归还接口**

```java
@PostMapping("/{id}/return")
public Result<Void> returnStock(@PathVariable Long id, HttpServletRequest request) {
    String token = request.getHeader("Authorization").substring(7);
    Long userId = jwtUtil.getUserId(token);
    String username = jwtUtil.getUsername(token);
    stockOutService.returnStock(id, userId);

    operationLogUtil.log(request, userId, username, "stock", "归还",
        "出库归还", "归还出库记录ID: " + id);

    return Result.success();
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/rmm/controller/StockOutController.java
git commit -m "feat: add pending-returns and return APIs to StockOutController"
```

---

## Chunk 3: Frontend - 出库申请增加归还选项

### Task 5: 出库申请页面增加"是否需要归还"

**Files:**
- Modify: `frontend/src/views/stock-out/apply.vue`
- Modify: `frontend/src/views/stock/index.vue`

- [ ] **Step 1: apply.vue - 在出库原因之前添加"是否需要归还"开关**

在 `<el-form-item label="出库原因" prop="reason">` 之前添加：
```html
<el-form-item label="是否归还">
  <el-switch
    v-model="form.needReturn"
    active-text="需要归还"
    inactive-text="不归还"
  />
  <div class="form-tip" v-if="form.needReturn">
    物品将在审批通过后标记为"借出"，归还后恢复为"在库"
  </div>
</el-form-item>
```

在 form reactive 中添加 `needReturn: false`：
```javascript
const form = reactive({
  stockId: null,
  reason: '',
  purpose: '',
  needReturn: false
})
```

添加样式：
```css
.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
```

- [ ] **Step 2: stock/index.vue - 批量出库弹窗增加"是否需要归还"开关**

在批量出库弹窗的"用途说明"之后添加：
```html
<el-form-item label="是否归还">
  <el-switch
    v-model="batchOutForm.needReturn"
    active-text="需要归还"
    inactive-text="不归还"
  />
</el-form-item>
```

修改 batchOutForm reactive：
```javascript
const batchOutForm = reactive({ reason: '', purpose: '', needReturn: false })
```

修改 confirmBatchOut 中提交数据：
```javascript
await batchApplyStockOut({
  stockIds: selectedRows.value.map(r => r.id),
  reason: batchOutForm.reason,
  purpose: batchOutForm.purpose,
  needReturn: batchOutForm.needReturn
})
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/stock-out/apply.vue frontend/src/views/stock/index.vue
git commit -m "feat: add needReturn switch to stock-out apply forms"
```

### Task 6: 出库管理页面增加"待归还"tab和归还操作

**Files:**
- Modify: `frontend/src/api/stock.js`
- Modify: `frontend/src/views/stock-out/index.vue`

- [ ] **Step 1: api/stock.js 添加新API**

```javascript
// 获取待归还列表
export function getPendingReturns(params) {
  return request.get('/stock-out/pending-returns', { params })
}

// 归还出库物品
export function returnStockOut(id) {
  return request.post(`/stock-out/${id}/return`)
}
```

- [ ] **Step 2: index.vue - import 新API**

修改 import 行：
```javascript
import { getStockOutList, approveStockOut, cancelStockOut, updateStockOut, deleteStockOut, getPendingReturns, returnStockOut } from '@/api/stock'
```

- [ ] **Step 3: index.vue - 在"我的申请"tab表格中增加归还状态列和归还按钮**

在"状态"列之后、"申请时间"列之前添加：
```html
<el-table-column prop="needReturn" label="归还" min-width="90">
  <template #default="{ row }">
    <el-tag v-if="row.needReturn" :type="row.returned ? 'success' : 'warning'" size="small">
      {{ row.returned ? '已归还' : '待归还' }}
    </el-tag>
    <span v-else class="text-muted">-</span>
  </template>
</el-table-column>
```

在操作列中添加归还按钮（在撤回按钮之后）：
```html
<el-button v-if="row.status === 1 && row.needReturn && !row.returned" link type="success" size="small" @click="handleReturn(row)">归还</el-button>
```

同样在"已审批"tab中也添加归还状态列。

- [ ] **Step 4: index.vue - 增加"待归还"tab**

在 `</el-tabs>` 之前添加新的 tab-pane：
```html
<el-tab-pane label="待归还" name="returns">
  <el-table :data="returnsData" v-loading="returnsLoading" border>
    <el-table-column prop="applicantName" label="借出人" min-width="100" show-overflow-tooltip />
    <el-table-column prop="materialCode" label="编号" min-width="130" show-overflow-tooltip />
    <el-table-column prop="materialName" label="名称" min-width="160" show-overflow-tooltip />
    <el-table-column prop="internalCode" label="内部编号" min-width="110" show-overflow-tooltip />
    <el-table-column prop="reason" label="出库原因" min-width="90">
      <template #default="{ row }">{{ reasonText(row.reason) }}</template>
    </el-table-column>
    <el-table-column prop="purpose" label="用途说明" min-width="150" show-overflow-tooltip />
    <el-table-column prop="approveTime" label="借出时间" min-width="150" />
    <el-table-column label="操作" min-width="100" fixed="right">
      <template #default="{ row }">
        <el-button link type="success" size="small" @click="handleReturn(row)">确认归还</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-pagination
    v-model:current-page="returnsPage.current"
    v-model:page-size="returnsPage.size"
    :total="returnsPage.total"
    :page-sizes="[10, 20, 50]"
    layout="total, sizes, prev, pager, next, jumper"
    class="pagination"
    @size-change="fetchReturnsData"
    @current-change="fetchReturnsData"
  />
</el-tab-pane>
```

- [ ] **Step 5: index.vue - script 中添加待归还数据和逻辑**

添加数据：
```javascript
// 待归还
const returnsLoading = ref(false)
const returnsData = ref([])
const returnsPage = reactive({ current: 1, size: 10, total: 0 })

const fetchReturnsData = async () => {
  returnsLoading.value = true
  try {
    const res = await getPendingReturns({
      current: returnsPage.current,
      size: returnsPage.size
    })
    returnsData.value = res.data?.records || []
    returnsPage.total = res.data?.total || 0
  } finally {
    returnsLoading.value = false
  }
}
```

添加归还操作：
```javascript
const handleReturn = async (row) => {
  await ElMessageBox.confirm(`确认归还「${row.internalCode}」？归还后该物品将恢复为"在库"状态。`)
  await returnStockOut(row.id)
  ElMessage.success('归还成功')
  refreshAfterAction()
  if (activeTab.value === 'returns') {
    fetchReturnsData()
  }
}
```

更新 refreshCurrentTab：
```javascript
const refreshCurrentTab = () => {
  if (activeTab.value === 'my') {
    myPage.current = 1
    fetchMyData()
  } else if (activeTab.value === 'pending') {
    pendingPage.current = 1
    fetchPendingData()
  } else if (activeTab.value === 'approved') {
    approvedPage.current = 1
    fetchApprovedData()
  } else if (activeTab.value === 'returns') {
    returnsPage.current = 1
    fetchReturnsData()
  }
}
```

- [ ] **Step 6: index.vue - 在"我的申请"和"已审批"表格中也显示归还状态列**

同样在"待审批"和"已审批"tab中，在状态列后增加归还状态列。

- [ ] **Step 7: Commit**

```bash
git add frontend/src/api/stock.js frontend/src/views/stock-out/index.vue
git commit -m "feat: add pending returns tab and return action to stock-out page"
```

---

## Chunk 4: Stock 状态显示更新

### Task 7: 更新库存状态显示

**Files:**
- Modify: `frontend/src/views/stock/index.vue` (statusText 和 statusType 映射)

- [ ] **Step 1: 更新 statusType 和 statusText 映射**

```javascript
const statusType = (s) => ({ 1: 'success', 2: 'warning', 3: 'danger', 4: '' }[s] || 'info')
const statusText = (s) => ({ 1: '正常', 2: '即将过期', 3: '已过期', 4: '借出' }[s] || '未知')
```

- [ ] **Step 2: 更新库存列表查询筛选，增加"借出"选项**

在状态下拉框中添加：
```html
<el-option label="借出" :value="4" />
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/stock/index.vue
git commit -m "feat: add stock status 4 (borrowed) display"
```
