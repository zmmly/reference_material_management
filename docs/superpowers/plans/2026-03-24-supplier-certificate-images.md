# 供应商证件照片上传功能实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为供应商管理功能增加证件照片上传能力，支持多张照片上传、预览和管理。

**Architecture:** 在 supplier 表添加 certificate_images 字段（TEXT 类型）存储 JSON 数组格式的文件路径；后端复用现有 UploadController；前端使用 Element Plus Upload 组件实现多文件上传。

**Tech Stack:** MySQL 8.0, Spring Boot 3.2, MyBatis-Plus, Vue 3, Element Plus

---

## 文件结构

### 新增文件
无

### 修改文件
- `database/migrations/20260324_add_certificate_images_to_supplier.sql` - 数据库迁移脚本
- `backend/src/main/java/com/rmm/entity/Supplier.java` - 添加 certificateImages 字段
- `frontend/src/views/basic/supplier/index.vue` - 添加证件照片上传组件
- `backend/src/main/java/com/rmm/controller/UploadController.java` - 增强安全验证（可选）

---

## Chunk 1: 数据库修改

### Task 1.1: 创建数据库迁移脚本

**Files:**
- Create: `database/migrations/20260324_add_certificate_images_to_supplier.sql`

- [ ] **Step 1: 创建迁移脚本文件**

创建文件 `database/migrations/20260324_add_certificate_images_to_supplier.sql`：

```sql
-- 为 supplier 表添加证件照片字段
-- 执行时间：2026-03-24

ALTER TABLE `supplier`
ADD COLUMN `certificate_images` TEXT COMMENT '证件照片路径（JSON数组）' AFTER `address`;
```

- [ ] **Step 2: 执行迁移脚本**

```bash
# 连接到 MySQL 容器（macOS Docker 环境）
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/migrations/20260324_add_certificate_images_to_supplier.sql

# 或者如果是 Windows 本地 MySQL
"/c/Program Files/MySQL/MySQL Shell 8.0/bin/mysqlsh.exe" --sql --uri root:123456@localhost:3306/reference_material_management < database/migrations/20260324_add_certificate_images_to_supplier.sql
```

Expected output: 无错误信息

- [ ] **Step 3: 验证字段添加成功**

```bash
# 连接数据库验证
docker exec -it mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 -e "USE reference_material_management; DESC supplier;"
```

Expected output:
```
+---------------------+--------------+------+-----+---------+----------------+
| Field               | Type         | Null | Key | Default | Extra          |
+---------------------+--------------+------+-----+---------+----------------+
| id                  | bigint       | NO   | PRI | NULL    | auto_increment |
| name                | varchar(200) | NO   |     | NULL    |                |
| contact             | varchar(50)  | YES  |     | NULL    |                |
| phone               | varchar(20)  | YES  |     | NULL    |                |
| address             | varchar(500) | YES  |     | NULL    |                |
| certificate_images  | text         | YES  |     | NULL    |                |
| status              | tinyint      | YES  |     | 1       |                |
| create_time         | datetime     | YES  |     | NULL    |                |
| update_time         | datetime     | YES  |     | NULL    |                |
+---------------------+--------------+------+-----+---------+----------------+
```

- [ ] **Step 4: 提交数据库修改**

```bash
git add database/migrations/20260324_add_certificate_images_to_supplier.sql
git commit -m "feat(db): add certificate_images field to supplier table"
```

---

## Chunk 2: 后端修改

### Task 2.1: 修改 Supplier 实体类

**Files:**
- Modify: `backend/src/main/java/com/rmm/entity/Supplier.java`

- [ ] **Step 1: 添加 certificateImages 字段到实体类**

修改 `backend/src/main/java/com/rmm/entity/Supplier.java`，在 `address` 字段后添加：

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

- [ ] **Step 2: 编译验证**

```bash
cd backend
mvn compile -q
```

Expected output: 编译成功，无错误

- [ ] **Step 3: 提交实体类修改**

```bash
git add backend/src/main/java/com/rmm/entity/Supplier.java
git commit -m "feat(entity): add certificateImages field to Supplier entity"
```

### Task 2.2: 增强 UploadController 安全验证（可选但推荐）

**Files:**
- Modify: `backend/src/main/java/com/rmm/controller/UploadController.java`

- [ ] **Step 1: 添加文件类型白名单常量**

在 `UploadController.java` 类的开头添加：

```java
@Slf4j
@Tag(name = "文件上传", description = "文件上传管理")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    // 新增：允许的文件扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS =
        Set.of(".jpg", ".jpeg", ".png", ".pdf", ".doc", ".docx", ".xls", ".xlsx");

    @Value("${upload.path:uploads}")
    private String configuredUploadPath;

    // ... 其他代码
}
```

需要导入：
```java
import java.util.Set;
import java.util.HashSet;
```

- [ ] **Step 2: 在 upload 方法中添加文件类型验证**

在 `upload()` 方法中，获取文件扩展名后添加验证：

```java
@Operation(summary = "上传文件")
@PostMapping
public Result<String> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "type", defaultValue = "certificate") String type) {
    if (file.isEmpty()) {
        return Result.error("文件不能为空");
    }

    try {
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 新增：验证文件类型
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return Result.error("不支持的文件类型，仅支持 JPG、PNG、PDF、DOC、XLS 等格式");
        }

        // ... 其余代码保持不变
```

- [ ] **Step 3: 在 preview 方法中添加路径遍历防护**

修改 `preview()` 方法：

```java
@Operation(summary = "预览文件")
@GetMapping("/preview")
public void preview(@RequestParam String path, HttpServletResponse response) throws IOException {
    if (path == null || path.isEmpty()) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "文件路径不能为空");
        return;
    }

    // 新增：路径安全验证，防止路径遍历攻击
    Path uploadPathObj = Paths.get(uploadPath).normalize();
    Path resolvedPath = uploadPathObj.resolve(path).normalize();

    if (!resolvedPath.startsWith(uploadPathObj)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法路径");
        return;
    }

    File file = resolvedPath.toFile();

    // ... 其余代码保持不变
```

需要导入：
```java
import java.nio.file.Path;
import java.nio.file.Paths;
```

- [ ] **Step 4: 编译验证**

```bash
cd backend
mvn compile -q
```

Expected output: 编译成功，无错误

- [ ] **Step 5: 提交安全增强**

```bash
git add backend/src/main/java/com/rmm/controller/UploadController.java
git commit -m "security(upload): add file type whitelist and path traversal protection"
```

---

## Chunk 3: 前端修改

### Task 3.1: 添加证件照片上传组件

**Files:**
- Modify: `frontend/src/views/basic/supplier/index.vue`

- [ ] **Step 1: 在模板中添加证件照片上传表单项**

在 `frontend/src/views/basic/supplier/index.vue` 中，在状态表单项前添加：

```vue
<el-form-item label="证件照片">
  <el-upload
    v-model:file-list="certificateFileList"
    action="/api/upload"
    list-type="picture-card"
    :headers="{ Authorization: 'Bearer ' + token }"
    :data="{ type: 'certificate' }"
    :on-success="handleUploadSuccess"
    :on-error="handleUploadError"
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
```

- [ ] **Step 2: 在 script 中导入所需依赖**

修改 `<script setup>` 部分，导入图标和工具函数：

```javascript
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSupplierList, createSupplier, updateSupplier, deleteSupplier } from '@/api/supplier'
import { getToken } from '@/utils/auth'
```

- [ ] **Step 3: 添加响应式数据**

在现有响应式数据后添加：

```javascript
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

// 新增：证件照片相关数据
const certificateFileList = ref([])
const token = getToken()
```

- [ ] **Step 4: 修改 form 对象，添加 certificateImages 字段**

```javascript
const form = reactive({
  name: '',
  contact: '',
  phone: '',
  address: '',
  certificateImages: '',  // 新增字段
  status: 1
})
```

- [ ] **Step 5: 添加上传相关方法**

在 `handleSubmit` 方法前添加：

```javascript
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

// 上传失败回调
const handleUploadError = (error, file) => {
  if (error.message?.includes('timeout')) {
    ElMessage.error('上传超时，请重试')
  } else if (error.message?.includes('Network Error')) {
    ElMessage.error('网络错误，请检查网络连接')
  } else {
    ElMessage.error(error.message || '上传失败')
  }
}

// 删除文件回调
const handleUploadRemove = (file, fileList) => {
  // fileList 自动更新，无需额外处理
}

// 预览文件
const handlePreview = (file) => {
  const path = file.response?.data || file.url
  if (path) {
    window.open(`/api/upload/preview?path=${encodeURIComponent(path)}`, '_blank')
  }
}
```

- [ ] **Step 6: 修改 handleAdd 方法，重置证件照片列表**

```javascript
const handleAdd = () => {
  editId.value = null
  certificateFileList.value = []  // 新增：重置文件列表
  Object.assign(form, {
    name: '',
    contact: '',
    phone: '',
    address: '',
    certificateImages: '',  // 新增字段
    status: 1
  })
  dialogVisible.value = true
}
```

- [ ] **Step 7: 修改 handleEdit 方法，加载已上传的证件照片**

```javascript
const handleEdit = (row) => {
  editId.value = row.id

  // 新增：解析证件照片 JSON
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

  Object.assign(form, row)
  dialogVisible.value = true
}
```

- [ ] **Step 8: 修改 handleSubmit 方法，处理证件照片数据**

```javascript
const handleSubmit = async () => {
  await formRef.value.validate()

  // 新增：提取文件路径数组
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
```

- [ ] **Step 9: 添加样式优化**

在 `<style scoped>` 部分添加：

```css
<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }

/* 新增：证件照片上传样式 */
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

- [ ] **Step 10: 验证前端代码**

```bash
cd frontend
npm run build
```

Expected output: 编译成功，无错误

- [ ] **Step 11: 提交前端修改**

```bash
git add frontend/src/views/basic/supplier/index.vue
git commit -m "feat(frontend): add certificate image upload for supplier management"
```

---

## Chunk 4: 集成测试

### Task 4.1: 功能测试

**Files:**
- 无

- [ ] **Step 1: 启动服务**

```bash
# 启动后端和前端
./scripts/start-all.sh restart
```

Expected output: 服务启动成功

- [ ] **Step 2: 测试上传功能**

手动测试步骤：
1. 访问 http://localhost:3002
2. 登录系统（admin / admin123）
3. 进入"基础数据" -> "供应商管理"
4. 点击"新增"按钮
5. 填写供应商信息
6. 点击"证件照片"上传区域
7. 选择一张图片文件（JPG、PNG 或 PDF）
8. 验证文件上传成功，显示缩略图
9. 点击"确定"保存供应商
10. 在列表中找到刚创建的供应商，点击"编辑"
11. 验证证件照片正确显示

- [ ] **Step 3: 测试预览功能**

手动测试步骤：
1. 在编辑供应商弹窗中
2. 点击已上传的证件照片
3. 验证在新窗口中打开文件预览

- [ ] **Step 4: 测试删除功能**

手动测试步骤：
1. 在编辑供应商弹窗中
2. 点击证件照片右上角的删除图标
3. 验证照片从列表中移除
4. 点击"确定"保存
5. 重新编辑该供应商，验证照片已被删除

- [ ] **Step 5: 测试边界条件**

手动测试场景：
1. 上传超过 10MB 的文件 -> 应该提示错误
2. 上传不支持的格式（如 .txt） -> 应该提示错误
3. 上传超过 10 张照片 -> 应该限制上传
4. 不上传证件照片直接保存 -> 应该成功保存

- [ ] **Step 6: 创建测试记录文档**

创建文件 `docs/test-results/2026-03-24-supplier-certificate-upload-test.md`：

```markdown
# 供应商证件照片上传功能测试报告

## 测试环境
- 日期：2026-03-24
- 后端版本：1.0.0
- 前端版本：1.0.0
- 浏览器：Chrome 最新版

## 测试结果

### 功能测试
- [ ] 单张图片上传
- [ ] 多张图片上传
- [ ] PDF 文件上传
- [ ] 图片预览
- [ ] 删除照片
- [ ] 编辑时加载已有照片

### 边界测试
- [ ] 超大文件（>10MB）
- [ ] 不支持的格式
- [ ] 超过 10 张照片

### 安全测试
- [ ] 未登录上传（应失败）
- [ ] 未登录预览（应失败）

## 问题记录
（记录测试中发现的问题）
```

---

## 完成清单

- [ ] 数据库字段添加成功
- [ ] 后端实体类修改完成
- [ ] 后端安全增强完成（可选）
- [ ] 前端上传组件添加完成
- [ ] 功能测试通过
- [ ] 提交所有代码到 Git

---

## 回滚计划

如果功能出现问题，可以按以下步骤回滚：

### 数据库回滚
```sql
ALTER TABLE `supplier` DROP COLUMN `certificate_images`;
```

### 后端回滚
```bash
git revert <commit-hash>
```

### 前端回滚
```bash
git revert <commit-hash>
```

---

## 注意事项

1. **文件存储路径**：确保配置的 `upload.path` 目录存在且有写入权限
2. **磁盘空间**：监控 `/var/rmm/uploads/certificate/` 目录的磁盘空间使用情况
3. **安全性**：生产环境务必实施 Task 2.2 的安全增强措施
4. **性能**：如果证件照片很多，考虑实施延迟清理策略（参见设计文档）
5. **备份**：定期备份上传的文件目录
