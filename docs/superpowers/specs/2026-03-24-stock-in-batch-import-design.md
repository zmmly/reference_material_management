# 入库登记批量导入功能设计

## 概述

为入库登记页面添加批量导入功能，支持下载导入模板和批量导入入库信息。

## 需求确认

1. **导入字段**：标准物质编码、批号、入库数量、有效期、存放位置、入库原因、备注
2. **错误处理**：先预览再确认模式
3. **字段填写方式**：标准物质用编码，位置和原因用文字
4. **证书支持**：批量导入不处理证书

## 前端设计

### 新增 UI 元素

在入库登记页面工具栏新增两个按钮：
- **下载模板** - 下载 Excel 导入模板
- **批量导入** - 打开导入对话框

### 导入对话框流程

```
步骤1: 上传文件
    └── 支持 .xlsx 格式
    ↓
步骤2: 预览数据
    └── 表格展示解析结果
    └── 错误行标红显示
    └── 显示成功/失败数量统计
    ↓
步骤3: 确认导入 / 取消
    └── 有错误时禁用确认按钮
    └── 确认后执行导入
    └── 显示导入结果
```

### 文件修改

**新增文件：**
- `frontend/src/api/import.js` - 导入相关 API

**修改文件：**
- `frontend/src/views/stock-in/index.vue` - 添加按钮和导入对话框

## 后端设计

### API 接口

| 接口 | 方法 | Content-Type | 说明 |
|------|------|--------------|------|
| `/api/stock-in/template` | GET | - | 下载导入模板 |
| `/api/stock-in/import/preview` | POST | multipart/form-data | 预览导入数据 |
| `/api/stock-in/import/confirm` | POST | application/json | 确认导入 |

### 请求/响应格式

**1. 下载模板**
```
GET /api/stock-in/template
Response: Excel 文件流
```

**2. 预览导入**
```
POST /api/stock-in/import/preview
Request: MultipartFile file
Response: {
  "code": 200,
  "data": {
    "items": [
      {
        "rowNum": 2,
        "materialCode": "RM001",
        "materialId": 1,
        "materialName": "标准物质A",
        "batchNo": "BATCH001",
        "quantity": 5,
        "expiryDate": "2025-12-31",
        "locationId": 1,
        "locationName": "仓库A",
        "reason": "新购入",
        "reasonCode": "PURCHASE",
        "remarks": "备注信息",
        "valid": true,
        "errors": []
      },
      {
        "rowNum": 3,
        "materialCode": "RM999",
        "batchNo": "BATCH002",
        "quantity": 0,
        "locationName": "仓库A",
        "reason": "新购入",
        "valid": false,
        "errors": ["标准物质编码不存在", "入库数量必须大于0"]
      }
    ],
    "totalCount": 2,
    "validCount": 1,
    "invalidCount": 1
  }
}
```

**3. 确认导入**
```
POST /api/stock-in/import/confirm
Request: {
  "items": [
    {
      "materialId": 1,
      "batchNo": "BATCH001",
      "quantity": 5,
      "expiryDate": "2025-12-31",
      "locationId": 1,
      "reason": "PURCHASE",
      "remarks": "备注信息"
    }
  ]
}
Response: {
  "code": 200,
  "data": {
    "successCount": 5,
    "message": "成功导入 5 条入库记录"
  }
}
```

### 模板格式

| 列名 | 必填 | 格式说明 |
|------|------|----------|
| 标准物质编码* | 是 | 系统中的标准物质编码 |
| 批号 | 是 | 文本 |
| 入库数量 | 是 | 整数，≥1 |
| 有效期 | 否 | 日期：YYYY-MM-DD |
| 存放位置* | 是 | 位置名称（完全匹配） |
| 入库原因* | 是 | 新购入/盘盈/归还/调拨入/其他 |
| 备注 | 否 | 文本 |

*第二行为示例数据行

### 校验规则

| 字段 | 校验规则 |
|------|----------|
| 标准物质编码 | 必填，必须存在于 reference_material 表的 code 字段 |
| 批号 | 必填，非空 |
| 入库数量 | 必填，整数，≥1 |
| 有效期 | 可选，日期格式 YYYY-MM-DD |
| 存放位置 | 必填，必须与 location 表的 name 完全匹配 |
| 入库原因 | 必填，必须是：新购入/盘盈/归还/调拨入/其他 |
| 备注 | 可选 |

### 文件修改

**修改文件：**
- `backend/src/main/java/com/rmm/controller/StockInController.java` - 添加导入相关接口
- `backend/src/main/java/com/rmm/service/StockInService.java` - 添加导入业务逻辑

**新增文件：**
- `backend/src/main/java/com/rmm/dto/StockInImportDTO.java` - 导入数据 DTO
- `backend/src/main/java/com/rmm/dto/StockInImportPreviewVO.java` - 预览响应 VO
- `backend/src/main/java/com/rmm/dto/StockInImportConfirmDTO.java` - 确认导入请求 DTO

## 数据流程

```
用户上传 Excel
    ↓
后端解析 Excel（EasyExcel）
    ↓
逐行校验数据
    ├── 查询标准物质（by code）
    ├── 查询存放位置（by name）
    └── 校验入库原因（枚举值）
    ↓
返回预览结果（含校验错误）
    ↓
用户确认导入
    ↓
批量创建入库记录
    ├── 复用现有 create 方法逻辑
    └── 生成内部编码、创建库存记录
```

## 错误处理

1. **文件格式错误**：返回错误提示
2. **数据校验错误**：在预览结果中标注，允许用户修改后重新上传
3. **导入过程错误**：事务回滚，返回错误信息

## 技术实现

- 使用 EasyExcel 读取 Excel 文件（与导出保持一致）
- 使用 Spring MultipartFile 处理文件上传
- 导入操作使用 @Transactional 保证事务一致性
- 内部编码生成复用现有逻辑
