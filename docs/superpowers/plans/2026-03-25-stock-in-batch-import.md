# 入库登记批量导入功能实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为入库登记页面添加批量导入功能，支持下载 Excel 模板和批量导入入库信息，采用预览-确认模式确保数据准确性。

**Architecture:** 后端使用 EasyExcel 解析上传的 Excel 文件，校验后返回预览结果；前端使用 Element Plus Upload 组件上传文件，预览表格展示解析结果，用户确认后调用确认接口批量创建入库记录。复用现有 StockInService.create() 的核心逻辑。

**Tech Stack:** Spring Boot 3.2, EasyExcel, MyBatis-Plus, Vue 3, Element Plus

---

## 文件结构

### 新增文件
- `frontend/src/api/import.js` - 前端导入相关 API
- `backend/src/main/java/com/rmm/dto/StockInImportDTO.java` - Excel 行数据 DTO
- `backend/src/main/java/com/rmm/dto/StockInImportPreviewVO.java` - 预览响应 VO
- `backend/src/main/java/com/rmm/dto/StockInImportConfirmDTO.java` - 确认导入请求 DTO

### 修改文件
- `backend/src/main/java/com/rmm/controller/StockInController.java` - 添加模板下载、预览、确认接口
- `backend/src/main/java/com/rmm/service/StockInService.java` - 添加导入相关业务逻辑
- `frontend/src/views/stock-in/index.vue` - 添加下载模板和批量导入按钮、导入对话框
- `frontend/src/api/stock.js` - 添加导入相关 API 函数

---

## Chunk 1: 后端 DTO 和 VO 类

### Task 1.1: 创建 Excel 导入 DTO

**Files:**
- Create: `backend/src/main/java/com/rmm/dto/StockInImportDTO.java`

- [ ] **Step 1: 创建 StockInImportDTO.java**

```java
package com.rmm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 入库导入 Excel 行数据 DTO
 */
@Data
@ColumnWidth(20)
public class StockInImportDTO {

    @ExcelProperty("标准物质编码")
    private String materialCode;

    @ExcelProperty("批号")
    private String batchNo;

    @ExcelProperty("入库数量")
    private Integer quantity;

    @ExcelProperty("有效期")
    private String expiryDate;

    @ExcelProperty("存放位置")
    private String locationName;

    @ExcelProperty("入库原因")
    private String reason;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remarks;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/rmm/dto/StockInImportDTO.java
git commit -m "feat(dto): add StockInImportDTO for batch import"
```

### Task 1.2: 创建预览响应 VO

**Files:**
- Create: `backend/src/main/java/com/rmm/dto/StockInImportPreviewVO.java`

- [ ] **Step 1: 创建 StockInImportPreviewVO.java**

```java
package com.rmm.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库导入预览响应 VO
 */
@Data
public class StockInImportPreviewVO {

    /** 预览项列表 */
    private List<PreviewItem> items;

    /** 总数量 */
    private Integer totalCount;

    /** 有效数量 */
    private Integer validCount;

    /** 无效数量 */
    private Integer invalidCount;

    /**
     * 单行预览数据
     */
    @Data
    public static class PreviewItem {
        /** Excel 行号 */
        private Integer rowNum;

        /** 标准物质编码 */
        private String materialCode;

        /** 标准物质ID（校验通过后填充） */
        private Long materialId;

        /** 标准物质名称（校验通过后填充） */
        private String materialName;

        /** 批号 */
        private String batchNo;

        /** 入库数量 */
        private Integer quantity;

        /** 有效期 */
        private LocalDate expiryDate;

        /** 存放位置ID（校验通过后填充） */
        private Long locationId;

        /** 存放位置名称 */
        private String locationName;

        /** 入库原因（文字） */
        private String reasonText;

        /** 入库原因编码（校验通过后填充） */
        private String reasonCode;

        /** 备注 */
        private String remarks;

        /** 是否有效 */
        private Boolean valid;

        /** 错误信息列表 */
        private List<String> errors;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/rmm/dto/StockInImportPreviewVO.java
git commit -m "feat(dto): add StockInImportPreviewVO for import preview"
```

### Task 1.3: 创建确认导入请求 DTO

**Files:**
- Create: `backend/src/main/java/com/rmm/dto/StockInImportConfirmDTO.java`

- [ ] **Step 1: 创建 StockInImportConfirmDTO.java**

```java
package com.rmm.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 确认入库导入请求 DTO
 */
@Data
public class StockInImportConfirmDTO {

    /** 导入项列表 */
    private List<ImportItem> items;

    @Data
    public static class ImportItem {
        /** 标准物质ID */
        private Long materialId;

        /** 批号 */
        private String batchNo;

        /** 入库数量 */
        private Integer quantity;

        /** 有效期 */
        private LocalDate expiryDate;

        /** 存放位置ID */
        private Long locationId;

        /** 入库原因编码 */
        private String reason;

        /** 备注 */
        private String remarks;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/rmm/dto/StockInImportConfirmDTO.java
git commit -m "feat(dto): add StockInImportConfirmDTO for import confirmation"
```

---

## Chunk 2: 后端 Service 层

### Task 2.1: 在 StockInService 添加导入业务逻辑

**Files:**
- Modify: `backend/src/main/java/com/rmm/service/StockInService.java`

- [ ] **Step 1: 添加必要的导入**

在文件顶部添加：
```java
import com.alibaba.excel.EasyExcel;
import com.rmm.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
```

- [ ] **Step 2: 添加入库原因映射常量**

在类中添加：
```java
    /** 入库原因文字到编码的映射 */
    private static final Map<String, String> REASON_TEXT_TO_CODE = Map.of(
        "新购入", "PURCHASE",
        "盘盈", "SURPLUS",
        "归还", "RETURN",
        "调拨入", "TRANSFER_IN",
        "其他", "OTHER"
    );
```

- [ ] **Step 3: 添加预览导入数据方法**

```java
    /**
     * 预览导入数据
     */
    public StockInImportPreviewVO previewImport(MultipartFile file) throws IOException {
        // 解析 Excel
        List<StockInImportDTO> rows = EasyExcel.read(file.getInputStream())
                .head(StockInImportDTO.class)
                .sheet()
                .doReadSync();

        // 预加载标准物质编码映射
        Map<String, ReferenceMaterial> materialCodeMap = loadMaterialCodeMap();
        // 预加载位置名称映射
        Map<String, Location> locationNameMap = loadLocationNameMap();

        List<StockInImportPreviewVO.PreviewItem> items = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            StockInImportDTO row = rows.get(i);
            // 跳过示例数据行（第二行，索引为0，但 Excel 行号从 2 开始）
            if (i == 0 && isSampleDataRow(row)) {
                continue;
            }

            StockInImportPreviewVO.PreviewItem item = validateRow(row, i + 2, materialCodeMap, locationNameMap);
            items.add(item);

            if (item.getValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        StockInImportPreviewVO result = new StockInImportPreviewVO();
        result.setItems(items);
        result.setTotalCount(items.size());
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        return result;
    }

    /**
     * 判断是否为示例数据行
     */
    private boolean isSampleDataRow(StockInImportDTO row) {
        // 如果所有字段都为空，跳过
        return row.getMaterialCode() == null && row.getBatchNo() == null;
    }

    /**
     * 加载标准物质编码映射
     */
    private Map<String, ReferenceMaterial> loadMaterialCodeMap() {
        Map<String, ReferenceMaterial> map = new HashMap<>();
        List<ReferenceMaterial> materials = materialMapper.selectList(null);
        for (ReferenceMaterial m : materials) {
            if (m.getCode() != null) {
                map.put(m.getCode(), m);
            }
        }
        return map;
    }

    /**
     * 加载位置名称映射
     */
    private Map<String, Location> loadLocationNameMap() {
        Map<String, Location> map = new HashMap<>();
        List<Location> locations = locationMapper.selectList(null);
        for (Location l : locations) {
            if (l.getName() != null) {
                map.put(l.getName(), l);
            }
        }
        return map;
    }

    /**
     * 校验单行数据
     */
    private StockInImportPreviewVO.PreviewItem validateRow(
            StockInImportDTO row, int rowNum,
            Map<String, ReferenceMaterial> materialCodeMap,
            Map<String, Location> locationNameMap) {

        StockInImportPreviewVO.PreviewItem item = new StockInImportPreviewVO.PreviewItem();
        item.setRowNum(rowNum);
        item.setMaterialCode(row.getMaterialCode());
        item.setBatchNo(row.getBatchNo());
        item.setQuantity(row.getQuantity());
        item.setLocationName(row.getLocationName());
        item.setReasonText(row.getReason());
        item.setRemarks(row.getRemarks());

        List<String> errors = new ArrayList<>();

        // 校验标准物质编码
        if (row.getMaterialCode() == null || row.getMaterialCode().isBlank()) {
            errors.add("标准物质编码不能为空");
        } else {
            ReferenceMaterial material = materialCodeMap.get(row.getMaterialCode());
            if (material == null) {
                errors.add("标准物质编码不存在");
            } else {
                item.setMaterialId(material.getId());
                item.setMaterialName(material.getName());
            }
        }

        // 校验批号
        if (row.getBatchNo() == null || row.getBatchNo().isBlank()) {
            errors.add("批号不能为空");
        }

        // 校验入库数量
        if (row.getQuantity() == null) {
            errors.add("入库数量不能为空");
        } else if (row.getQuantity() < 1) {
            errors.add("入库数量必须大于0");
        }

        // 校验有效期格式
        if (row.getExpiryDate() != null && !row.getExpiryDate().isBlank()) {
            try {
                item.setExpiryDate(LocalDate.parse(row.getExpiryDate()));
            } catch (Exception e) {
                errors.add("有效期格式错误，应为 YYYY-MM-DD");
            }
        }

        // 校验存放位置
        if (row.getLocationName() == null || row.getLocationName().isBlank()) {
            errors.add("存放位置不能为空");
        } else {
            Location location = locationNameMap.get(row.getLocationName());
            if (location == null) {
                errors.add("存放位置不存在");
            } else {
                item.setLocationId(location.getId());
            }
        }

        // 校验入库原因
        if (row.getReason() == null || row.getReason().isBlank()) {
            errors.add("入库原因不能为空");
        } else {
            String code = REASON_TEXT_TO_CODE.get(row.getReason());
            if (code == null) {
                errors.add("入库原因必须是：新购入/盘盈/归还/调拨入/其他");
            } else {
                item.setReasonCode(code);
            }
        }

        item.setValid(errors.isEmpty());
        item.setErrors(errors);
        return item;
    }
```

- [ ] **Step 4: 添加确认导入方法**

```java
    /**
     * 确认批量导入
     */
    @Transactional
    public int confirmImport(StockInImportConfirmDTO dto, Long operatorId) {
        int successCount = 0;
        for (StockInImportConfirmDTO.ImportItem item : dto.getItems()) {
            // 构建 StockIn 对象，复用现有 create 方法逻辑
            StockIn stockIn = new StockIn();
            stockIn.setMaterialId(item.getMaterialId());
            stockIn.setBatchNo(item.getBatchNo());
            stockIn.setQuantity(java.math.BigDecimal.valueOf(item.getQuantity()));
            stockIn.setExpiryDate(item.getExpiryDate());
            stockIn.setLocationId(item.getLocationId());
            stockIn.setReason(item.getReason());
            stockIn.setRemarks(item.getRemarks());

            // 复用 create 方法的核心逻辑（但不调用 create 方法本身，避免重复查询）
            createStockInRecord(stockIn, operatorId);
            successCount++;
        }
        return successCount;
    }

    /**
     * 创建入库记录（复用 create 方法核心逻辑）
     */
    private void createStockInRecord(StockIn stockIn, Long operatorId) {
        ReferenceMaterial material = materialMapper.selectById(stockIn.getMaterialId());

        int quantity = stockIn.getQuantity() != null ? stockIn.getQuantity().intValue() : 1;

        // 获取该批号的最大序列号
        int maxSequence = getMaxSequence(stockIn.getBatchNo());

        // 生成内部编号范围
        String firstCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + 1);
        String lastCode = generateInternalCode(stockIn.getBatchNo(), maxSequence + quantity);
        String internalCodeRange = quantity == 1 ? firstCode : firstCode + " ~ " + lastCode;

        stockIn.setOperatorId(operatorId);
        stockIn.setInternalCode(internalCodeRange);

        // 为每个物品创建独立的库存记录
        for (int i = 1; i <= quantity; i++) {
            int sequence = maxSequence + i;
            String internalCode = generateInternalCode(stockIn.getBatchNo(), sequence);

            Stock stock = new Stock();
            stock.setMaterialId(stockIn.getMaterialId());
            stock.setBatchNo(stockIn.getBatchNo());
            stock.setInternalCode(internalCode);
            stock.setExpiryDate(stockIn.getExpiryDate());
            stock.setQuantity(java.math.BigDecimal.ONE);
            stock.setLocationId(stockIn.getLocationId());
            stock.setStatus(1);
            stockMapper.insert(stock);
        }

        stockInMapper.insert(stockIn);
    }
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/rmm/service/StockInService.java
git commit -m "feat(service): add batch import preview and confirm methods"
```

---

## Chunk 3: 后端 Controller 层

### Task 3.1: 在 StockInController 添加导入接口

**Files:**
- Modify: `backend/src/main/java/com/rmm/controller/StockInController.java`

- [ ] **Step 1: 添加必要的导入**

在文件顶部添加：
```java
import com.rmm.dto.StockInImportDTO;
import com.rmm.dto.StockInImportPreviewVO;
import com.rmm.dto.StockInImportConfirmDTO;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
```

- [ ] **Step 2: 添加下载模板接口**

在 `export` 方法后添加：
```java
    @GetMapping("/template")
    @Operation(summary = "下载入库导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("入库导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 创建模板数据（标题行 + 示例行）
        List<StockInImportDTO> templateData = new ArrayList<>();

        // 示例数据行
        StockInImportDTO sample = new StockInImportDTO();
        sample.setMaterialCode("RM001");
        sample.setBatchNo("BATCH20260325");
        sample.setQuantity(5);
        sample.setExpiryDate("2026-12-31");
        sample.setLocationName("仓库A");
        sample.setReason("新购入");
        sample.setRemarks("示例备注");
        templateData.add(sample);

        // 写入 Excel
        EasyExcel.write(response.getOutputStream(), StockInImportDTO.class)
                .sheet("入库导入")
                .doWrite(templateData);
    }
```

需要添加 `ArrayList` 导入：
```java
import java.util.ArrayList;
```

- [ ] **Step 3: 添加预览导入接口**

```java
    @PostMapping("/import/preview")
    @Operation(summary = "预览入库导入数据")
    public Result<StockInImportPreviewVO> previewImport(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件格式
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return Result.error("仅支持 .xlsx 格式的 Excel 文件");
            }

            StockInImportPreviewVO preview = stockInService.previewImport(file);
            return Result.success(preview);
        } catch (IOException e) {
            log.error("解析导入文件失败", e);
            return Result.error("解析文件失败：" + e.getMessage());
        }
    }
```

需要添加 `@Slf4j` 注解到类上（如果还没有）：
```java
@Slf4j
@RestController
@RequestMapping("/api/stock-in")
@RequiredArgsConstructor
public class StockInController {
```

以及导入：
```java
import lombok.extern.slf4j.Slf4j;
```

- [ ] **Step 4: 添加确认导入接口**

```java
    @PostMapping("/import/confirm")
    @Operation(summary = "确认入库导入")
    public Result<Map<String, Object>> confirmImport(
            @RequestBody StockInImportConfirmDTO dto,
            HttpServletRequest request) {
        // 验证数据
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.error("导入数据不能为空");
        }

        // 获取当前用户
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);

        // 执行导入
        int successCount = stockInService.confirmImport(dto, userId);

        // 记录操作日志
        operationLogUtil.log(request, userId, username, "stock", "入库",
            "批量入库导入", "成功导入 " + successCount + " 条入库记录");

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("message", "成功导入 " + successCount + " 条入库记录");
        return Result.success(result);
    }
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/rmm/controller/StockInController.java
git commit -m "feat(controller): add batch import endpoints for stock-in"
```

---

## Chunk 4: 前端 API 层

### Task 4.1: 添加导入相关 API

**Files:**
- Modify: `frontend/src/api/stock.js`

- [ ] **Step 1: 在 stock.js 末尾添加导入 API**

```javascript
// 下载入库导入模板
export function downloadStockInTemplate() {
  return request.get('/stock-in/template', {
    responseType: 'blob'
  })
}

// 预览入库导入数据
export function previewStockInImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/stock-in/import/preview', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 确认入库导入
export function confirmStockInImport(items) {
  return request.post('/stock-in/import/confirm', { items })
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/stock.js
git commit -m "feat(api): add batch import API functions for stock-in"
```

---

## Chunk 5: 前端 UI 组件

### Task 5.1: 添加下载模板和批量导入按钮

**Files:**
- Modify: `frontend/src/views/stock-in/index.vue`

- [ ] **Step 1: 在工具栏添加按钮**

在 `<el-button type="warning" @click="handleExport">` 后面添加两个按钮：

```vue
          <el-button type="info" @click="handleDownloadTemplate">下载模板</el-button>
          <el-button type="primary" @click="handleOpenImport">批量导入</el-button>
```

- [ ] **Step 2: 添加导入对话框模板**

在现有 `</el-dialog>` 后面添加导入对话框：

```vue
    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入入库信息" width="900">
      <!-- 步骤 1: 上传文件 -->
      <div v-if="importStep === 1">
        <el-upload
          ref="importUploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          drag
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .xlsx 格式，请先下载模板填写数据</div>
          </template>
        </el-upload>
      </div>

      <!-- 步骤 2: 预览数据 -->
      <div v-else-if="importStep === 2" v-loading="importLoading">
        <el-alert
          :title="`共 ${importPreview.totalCount} 条数据，有效 ${importPreview.validCount} 条，无效 ${importPreview.invalidCount} 条`"
          :type="importPreview.invalidCount > 0 ? 'warning' : 'success'"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-table :data="importPreview.items" border max-height="400">
          <el-table-column prop="rowNum" label="行号" width="70" />
          <el-table-column prop="materialCode" label="物质编码" width="120" />
          <el-table-column prop="materialName" label="物质名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="batchNo" label="批号" width="120" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column prop="expiryDate" label="有效期" width="110" />
          <el-table-column prop="locationName" label="位置" width="100" />
          <el-table-column prop="reasonText" label="原因" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.valid ? 'success' : 'danger'">
                {{ row.valid ? '有效' : '无效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="错误信息" min-width="200">
            <template #default="{ row }">
              <span v-if="row.errors && row.errors.length" class="error-text">
                {{ row.errors.join('；') }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 步骤 3: 导入结果 -->
      <div v-else-if="importStep === 3">
        <el-result
          icon="success"
          title="导入成功"
          :sub-title="`成功导入 ${importResult.successCount} 条入库记录`"
        />
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          v-if="importStep === 1"
          type="primary"
          :disabled="!importFile"
          :loading="importLoading"
          @click="handlePreviewImport"
        >
          预览数据
        </el-button>
        <el-button
          v-if="importStep === 2"
          @click="importStep = 1"
        >
          重新上传
        </el-button>
        <el-button
          v-if="importStep === 2"
          type="primary"
          :disabled="importPreview.invalidCount > 0"
          :loading="importLoading"
          @click="handleConfirmImport"
        >
          确认导入
        </el-button>
        <el-button
          v-if="importStep === 3"
          type="primary"
          @click="handleImportComplete"
        >
          完成
        </el-button>
      </template>
    </el-dialog>
```

- [ ] **Step 3: 添加图标导入**

在 `<script setup>` 顶部添加图标导入：
```javascript
import { UploadFilled } from '@element-plus/icons-vue'
```

- [ ] **Step 4: 添加导入 API 导入**

修改 import 语句：
```javascript
import { getStockInList, createStockIn, exportStockIn, downloadStockInTemplate, previewStockInImport, confirmStockInImport } from '@/api/stock'
```

- [ ] **Step 5: 添加响应式数据**

在现有响应式数据后添加：
```javascript
// 批量导入相关
const importDialogVisible = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const importLoading = ref(false)
const importPreview = ref({ items: [], totalCount: 0, validCount: 0, invalidCount: 0 })
const importResult = ref({ successCount: 0 })
const importUploadRef = ref()
```

- [ ] **Step 6: 添加下载模板方法**

在 `viewCertificate` 方法后添加：
```javascript
// 下载导入模板
const handleDownloadTemplate = async () => {
  try {
    const blob = await downloadStockInTemplate()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '入库导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}
```

- [ ] **Step 7: 添加导入相关方法**

```javascript
// 打开导入对话框
const handleOpenImport = () => {
  importStep.value = 1
  importFile.value = null
  importPreview.value = { items: [], totalCount: 0, validCount: 0, invalidCount: 0 }
  importResult.value = { successCount: 0 }
  if (importUploadRef.value) {
    importUploadRef.value.clearFiles()
  }
  importDialogVisible.value = true
}

// 文件选择变化
const handleFileChange = (uploadFile) => {
  importFile.value = uploadFile.raw
}

// 超出文件数量限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

// 预览导入数据
const handlePreviewImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    const res = await previewStockInImport(importFile.value)
    importPreview.value = res.data
    importStep.value = 2
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '预览失败')
  } finally {
    importLoading.value = false
  }
}

// 确认导入
const handleConfirmImport = async () => {
  // 只导入有效数据
  const validItems = importPreview.value.items
    .filter(item => item.valid)
    .map(item => ({
      materialId: item.materialId,
      batchNo: item.batchNo,
      quantity: item.quantity,
      expiryDate: item.expiryDate,
      locationId: item.locationId,
      reason: item.reasonCode,
      remarks: item.remarks
    }))

  importLoading.value = true
  try {
    const res = await confirmStockInImport(validItems)
    importResult.value = res.data
    importStep.value = 3
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// 导入完成
const handleImportComplete = () => {
  importDialogVisible.value = false
  fetchData()
}
```

- [ ] **Step 8: 添加样式**

在 `<style scoped>` 块中添加：
```css
.error-text {
  color: #f56c6c;
}
```

- [ ] **Step 9: 验证前端编译**

```bash
cd frontend && npm run build
```

- [ ] **Step 10: 提交**

```bash
git add frontend/src/views/stock-in/index.vue
git commit -m "feat(frontend): add batch import UI for stock-in module"
```

---

## Chunk 6: 集成测试

### Task 6.1: 功能测试

**Files:**
- 无

- [ ] **Step 1: 重启服务**

```bash
./scripts/start-all.sh restart
```

- [ ] **Step 2: 测试下载模板**

手动测试步骤：
1. 访问 http://localhost:3002
2. 登录系统（admin / admin123）
3. 进入"库存管理" -> "入库登记"
4. 点击"下载模板"按钮
5. 验证下载的 Excel 文件包含正确的列标题和示例数据

- [ ] **Step 3: 测试正常导入流程**

手动测试步骤：
1. 打开下载的模板
2. 填写 2-3 行有效数据（使用系统中存在的标准物质编码和位置名称）
3. 保存文件
4. 点击"批量导入"按钮
5. 上传填好的文件
6. 验证预览页面显示正确的数据，所有行状态为"有效"
7. 点击"确认导入"
8. 验证导入成功，显示成功数量
9. 验证入库记录列表中出现新导入的记录

- [ ] **Step 4: 测试错误数据校验**

手动测试步骤：
1. 创建包含错误数据的 Excel 文件：
   - 一行使用不存在的标准物质编码
   - 一行使用不存在的位置名称
   - 一行使用错误的入库原因
   - 一行入库数量为 0
2. 上传文件
3. 验证预览页面正确标注错误行和错误信息
4. 验证"确认导入"按钮被禁用（因为有无效数据）

- [ ] **Step 5: 测试 API 健康检查**

```bash
# 测试模板下载接口
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer <token>" http://localhost:8080/api/stock-in/template
# 预期输出: 200
```

- [ ] **Step 6: 提交最终版本**

```bash
git add -A
git commit -m "feat: complete stock-in batch import feature"
```

---

## 完成清单

- [ ] 后端 DTO 类创建完成（StockInImportDTO, StockInImportPreviewVO, StockInImportConfirmDTO）
- [ ] 后端 Service 方法添加完成（previewImport, confirmImport）
- [ ] 后端 Controller 接口添加完成（/template, /import/preview, /import/confirm）
- [ ] 前端 API 函数添加完成
- [ ] 前端 UI 组件添加完成
- [ ] 功能测试通过
- [ ] 所有代码提交到 Git

---

## 回滚计划

如果功能出现问题，可以按以下步骤回滚：

### 后端回滚
```bash
git revert <commit-hash>  # 按提交顺序逐个回滚
```

### 前端回滚
```bash
git revert <commit-hash>
```

---

## 注意事项

1. **模板格式**：模板必须包含示例数据行，方便用户理解填写方式
2. **校验严格性**：位置名称必须完全匹配，入库原因必须是预设值
3. **事务一致性**：确认导入使用 @Transactional 保证原子性
4. **性能考虑**：预览时预加载标准物质和位置映射，避免 N+1 查询
5. **批量导入限制**：建议单次导入不超过 100 条记录

