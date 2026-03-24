# 供应商证件照片上传功能设计

## 概述

为供应商管理功能增加证件照片上传能力，支持上传多张证件照片（营业执照、生产许可证、质量认证等），在供应商详情中显示和管理这些照片。

## 需求分析

### 功能需求
- 支持上传多张证件照片
- 不需要证件分类或有效期管理
- 在供应商编辑/查看弹窗中显示照片
- 支持文件预览和删除
- 支持常见图片格式和 PDF

### 非功能需求
- 文件大小限制：单个文件不超过 10MB
- 支持格式：JPG、JPEG、PNG、PDF
- 响应时间：文件上传不超过 5 秒
- 存储路径：可配置，默认使用 `/var/rmm/uploads/certificate/`

## 技术方案

### 数据库设计

#### 修改 supplier 表

在 `supplier` 表中添加 `certificate_images` 字段：

```sql
ALTER TABLE `supplier`
ADD COLUMN `certificate_images` TEXT COMMENT '证件照片路径（JSON数组）' AFTER `address`;
```

**字段说明：**
- 类型：TEXT
- 存储格式：JSON 数组字符串
- 示例值：`["/certificate/2024/03/abc123.jpg", "/certificate/2024/03/def456.pdf"]`
- 允许为空：NULL（表示没有上传证件照片）

**存储格式规范：**
```json
[
  "/certificate/2024/03/abc123.jpg",
  "/certificate/2024/03/def456.pdf",
  "/certificate/2024/03/ghi789.png"
]
```

每个元素是文件的相对路径，基于配置的 `upload.path`。

### 后端设计

#### 实体类修改

**文件：** `backend/src/main/java/com/rmm/entity/Supplier.java`

```java
package com.rmm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String contact;
    private String phone;
    private String address;

    // 新增字段：证件照片路径（JSON数组字符串）
    private String certificateImages;

    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**说明：**
- `certificateImages` 字段存储 JSON 字符串
- MyBatis-Plus 自动映射，无需额外配置
- 前端负责 JSON 序列化和反序列化

#### API 接口

**复用现有接口，无需新增：**

1. **文件上传**
   - POST `/api/upload`
   - 参数：`file` (文件), `type` = "certificate"
   - 返回：文件相对路径
   - 已实现，直接使用

2. **文件预览**
   - GET `/api/upload/preview?path={文件路径}`
   - 返回：文件内容（支持在线预览）
   - 已实现，直接使用

3. **供应商 CRUD**
   - GET/POST/PUT/DELETE `/api/basic/supplier`
   - 无需修改，自动处理 `certificateImages` 字段

### 前端设计

#### 组件结构

**文件：** `frontend/src/views/basic/supplier/index.vue`

在供应商编辑弹窗中添加证件照片上传组件：

```vue
<template>
  <!-- 表单中添加 -->
  <el-form-item label="证件照片">
    <el-upload
      v-model:file-list="certificateFileList"
      action="/api/upload"
      list-type="picture-card"
      :headers="{ Authorization: 'Bearer ' + token }"
      :data="{ type: 'certificate' }"
      :on-success="handleUploadSuccess"
      :on-remove="handleUploadRemove"
      :on-preview="handlePreview"
      :before-upload="beforeUpload"
      accept=".jpg,.jpeg,.png,.pdf"
      :limit="10"
    >
      <el-icon><Plus /></el-icon>
      <template #tip>
        <div class="el-upload__tip">
          支持 JPG、PNG、PDF 格式，单个文件不超过 10MB，最多 10 张
        </div>
      </template>
    </el-upload>
  </el-form-item>
</template>
```

#### 数据管理

```javascript
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'

// 响应式数据
const certificateFileList = ref([])
const token = getToken()

// 表单数据
const form = reactive({
  name: '',
  contact: '',
  phone: '',
  address: '',
  certificateImages: '',  // 新增字段
  status: 1
})

// 上传前验证
const beforeUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png'].includes(file.type)
  const isPDF = file.type === 'application/pdf'

  if (!isImage && !isPDF) {
    ElMessage.error('只能上传 JPG、PNG 或 PDF 格式的文件')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }

  return true
}

// 上传成功回调
const handleUploadSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
    // 从列表中移除失败的文件
    const index = fileList.indexOf(file)
    if (index > -1) {
      fileList.splice(index, 1)
    }
  }
}

// 删除文件回调
const handleUploadRemove = (file, fileList) => {
  // fileList 自动更新
}

// 预览文件
const handlePreview = (file) => {
  const path = file.response?.data || file.url
  if (path) {
    window.open(`/api/upload/preview?path=${encodeURIComponent(path)}`, '_blank')
  }
}

// 编辑时加载文件列表
const handleEdit = (row) => {
  editId.value = row.id

  // 解析证件照片 JSON
  if (row.certificateImages) {
    try {
      const imagePaths = JSON.parse(row.certificateImages)
      certificateFileList.value = imagePaths.map((path, index) => ({
        name: `证件${index + 1}`,
        url: path,
        response: { data: path }  // 已上传的文件需要这个字段
      }))
    } catch (e) {
      console.error('解析证件照片失败:', e)
      certificateFileList.value = []
    }
  } else {
    certificateFileList.value = []
  }

  // 填充表单其他字段...
  Object.assign(form, row)
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()

  // 提取文件路径数组
  const imagePaths = certificateFileList.value
    .map(file => file.response?.data || file.url)
    .filter(Boolean)

  // 序列化为 JSON 字符串
  form.certificateImages = imagePaths.length > 0 ? JSON.stringify(imagePaths) : ''

  try {
    if (editId.value) {
      await updateSupplier(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createSupplier(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 重置表单
const handleAdd = () => {
  editId.value = null
  certificateFileList.value = []
  Object.assign(form, {
    name: '',
    contact: '',
    phone: '',
    address: '',
    certificateImages: '',
    status: 1
  })
  dialogVisible.value = true
}
</script>
```

#### 样式优化

```vue
<style scoped>
:deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 120px;
  height: 120px;
}

.el-upload__tip {
  color: #999;
  font-size: 12px;
  margin-top: 7px;
}
</style>
```

## 文件存储结构

上传的证件照片按以下结构组织：

```
{upload.path}/
└── certificate/
    └── 2024/
        └── 03/
            ├── abc123.jpg
            ├── def456.pdf
            └── ghi789.png
```

- 按类型（certificate）分类存储
- 按年月组织目录结构
- 文件名使用 UUID 避免冲突

## 数据流

### 上传流程

```
1. 用户选择文件
   ↓
2. 前端验证文件格式和大小
   ↓
3. POST /api/upload?type=certificate
   ↓
4. UploadController 保存文件到磁盘
   ↓
5. 返回文件相对路径
   ↓
6. 前端将路径添加到 fileList
   ↓
7. 用户提交表单时，将 fileList 转为 JSON 字符串
   ↓
8. POST/PUT /api/basic/supplier
   ↓
9. SupplierService 保存到数据库
```

### 预览流程

```
1. 用户点击图片预览
   ↓
2. 获取文件路径
   ↓
3. 打开新窗口：GET /api/upload/preview?path={路径}
   ↓
4. UploadController 读取文件并返回
   ↓
5. 浏览器显示文件内容
```

## 错误处理

### 上传失败
- **文件格式错误**：提示"只能上传 JPG、PNG 或 PDF 格式的文件"
- **文件过大**：提示"文件大小不能超过 10MB"
- **上传接口失败**：显示错误信息，从列表移除文件

### 数据库错误
- **JSON 解析失败**：捕获异常，返回空数组
- **字段过长**：TEXT 类型最大 65535 字节，足够存储约 2000 个文件路径

## 安全考虑

### 文件上传安全
- 限制文件类型（白名单）
- 限制文件大小（10MB）
- 文件名使用 UUID，避免路径遍历攻击
- 上传需要认证（Authorization header）

### 访问控制
- 文件预览需要认证（复用现有认证机制）
- 供应商数据修改需要权限验证

## 测试计划

### 单元测试
- 文件上传接口测试
- 文件大小和格式验证测试
- JSON 序列化/反序列化测试

### 集成测试
- 上传证件照片 → 保存供应商 → 查询验证
- 删除证件照片 → 更新供应商 → 查询验证
- 文件预览功能测试

### 手动测试场景
1. 新建供应商，不上传证件照片
2. 新建供应商，上传 1 张照片
3. 新建供应商，上传多张照片（5-10张）
4. 编辑供应商，添加新的证件照片
5. 编辑供应商，删除已有的证件照片
6. 预览不同格式的文件（JPG、PNG、PDF）
7. 上传超大文件（>10MB）
8. 上传不支持的文件格式

## 实施计划

### Phase 1: 数据库修改
- [ ] 执行 ALTER TABLE 语句
- [ ] 验证字段添加成功

### Phase 2: 后端修改
- [ ] 修改 Supplier.java 实体类
- [ ] 重启后端服务
- [ ] 测试 API 接口

### Phase 3: 前端修改
- [ ] 修改 supplier/index.vue
- [ ] 添加文件上传组件
- [ ] 实现数据转换逻辑
- [ ] 测试文件上传和预览

### Phase 4: 测试验证
- [ ] 功能测试
- [ ] 边界条件测试
- [ ] 文件预览测试
- [ ] 性能测试

## 性能考虑

### 数据库性能
- JSON 字段不影响查询性能（不用于 WHERE 条件）
- TEXT 字段最大 65535 字节，足够存储
- 查询供应商列表时，`certificateImages` 字段占用较少

### 文件存储性能
- 文件按年月分目录，避免单目录文件过多
- 文件名使用 UUID，避免冲突
- 文件存储路径可配置，支持迁移到专用存储

### 前端性能
- 使用懒加载，仅在需要时加载图片
- 限制最多上传 10 张照片
- 图片预览使用浏览器原生能力

## 扩展性考虑

### 未来可能的需求
1. **证件类型分类** - 可扩展为 JSON 对象数组，每项包含 type 和 path
2. **证件有效期** - 可扩展为 JSON 对象数组，每项包含 path 和 expiryDate
3. **文件描述** - 可扩展为 JSON 对象数组，每项包含 path 和 description

**扩展示例：**
```json
[
  {
    "path": "/certificate/2024/03/abc.jpg",
    "type": "营业执照",
    "expiryDate": "2025-12-31",
    "description": "营业执照副本"
  }
]
```

### 向后兼容
- 当前的字符串数组格式可以直接升级为对象数组
- 如果字段是字符串数组，前端按无类型/无有效期处理
- 如果字段是对象数组，前端显示类型和有效期信息

## 风险评估

### 技术风险
- **低**：使用成熟技术，无复杂依赖
- **低**：复用现有文件上传接口，已验证稳定

### 业务风险
- **低**：功能简单，不影响核心业务流程
- **低**：数据存储在 TEXT 字段，可随时扩展

### 兼容性风险
- **低**：新增字段，不影响现有数据
- **低**：前端组件使用 Element Plus，浏览器兼容性好

## 验收标准

### 功能验收
- ✅ 可以上传证件照片（JPG、PNG、PDF）
- ✅ 可以预览已上传的照片
- ✅ 可以删除已上传的照片
- ✅ 保存供应商时，照片路径正确存储到数据库
- ✅ 编辑供应商时，已上传的照片正确显示
- ✅ 文件大小和格式限制生效

### 性能验收
- ✅ 单个文件上传时间 < 5 秒（10MB 文件）
- ✅ 查询供应商列表性能无明显下降

### 安全验收
- ✅ 未登录用户无法上传文件
- ✅ 未登录用户无法预览文件
- ✅ 文件格式限制生效
- ✅ 文件大小限制生效

## 维护说明

### 日常维护
- 定期检查 `/var/rmm/uploads/certificate/` 目录磁盘空间
- 定期备份上传的文件
- 监控文件上传失败率

### 故障排查
- 文件上传失败：检查目录权限、磁盘空间
- 文件预览失败：检查文件路径、文件是否存在
- JSON 解析失败：检查数据库字段内容格式

## 相关文档

- [文件上传配置说明](../../upload-configuration.md)
- [备份配置说明](../../backup-configuration.md)
- [部署配置指南](../../deployment-configuration.md)
