<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId" placeholder="全部" clearable style="width: 200px">
            <el-option v-for="item in categoryList" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
          <el-button type="info" @click="handleDownloadTemplate">下载模板</el-button>
          <el-button type="primary" @click="handleOpenImport">批量导入</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="编号" min-width="120" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="casNumber" label="CAS号" min-width="110" />
        <el-table-column prop="categoryName" label="分类" min-width="100" />
        <el-table-column prop="specification" label="规格" min-width="80" />
        <el-table-column prop="supplierName" label="供应商" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑标准物质' : '新增标准物质'" width="700">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="编号" prop="code">
              <el-input v-model="form.code" placeholder="请输入标准物质编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入标准物质名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="英文名称">
              <el-input v-model="form.englishName" placeholder="请输入英文名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="CAS号" prop="casNumber">
              <el-input v-model="form.casNumber" placeholder="请输入CAS号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="item in categoryList" :key="item.id" :label="item.label" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%">
                <el-option v-for="item in supplierList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="包装形式">
              <el-input v-model="form.packageForm" placeholder="请输入包装形式" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="基质">
              <el-input v-model="form.matrix" placeholder="请输入基质" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入标准物质" width="800" :close-on-click-modal="false">
      <!-- 步骤1：上传文件 -->
      <div v-if="importStep === 1">
        <el-upload
          drag
          accept=".xlsx"
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">只能上传 .xlsx 文件，请先下载模板填写数据</div>
          </template>
        </el-upload>
      </div>

      <!-- 步骤2：预览数据 -->
      <div v-else-if="importStep === 2">
        <el-alert
          :title="`共 ${importPreview?.totalCount || 0} 条数据，有效 ${importPreview?.validCount || 0} 条，无效 ${importPreview?.invalidCount || 0} 条`"
          :type="importPreview?.invalidCount > 0 ? 'warning' : 'success'"
          show-icon
          style="margin-bottom: 16px"
        />
        <el-table :data="importPreview?.items || []" border max-height="400">
          <el-table-column prop="rowNum" label="行号" width="70" />
          <el-table-column prop="code" label="编号" width="100" />
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="specification" label="规格" width="80" />
          <el-table-column prop="supplierName" label="供应商" width="100" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.valid ? 'success' : 'danger'">{{ row.valid ? '有效' : '无效' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="错误信息" min-width="150">
            <template #default="{ row }">
              <span v-if="row.errors?.length" style="color: #f56c6c">{{ row.errors.join('；') }}</span>
              <span v-else style="color: #67c23a">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 步骤3：导入结果 -->
      <div v-else-if="importStep === 3">
        <el-result
          icon="success"
          title="导入完成"
          :sub-title="`成功导入 ${importResult || 0} 条数据`"
        />
      </div>

      <template #footer>
        <el-button v-if="importStep === 1" @click="importDialogVisible = false">取消</el-button>
        <el-button v-if="importStep === 1" type="primary" @click="handlePreviewImport" :disabled="!importFile">下一步</el-button>
        <el-button v-if="importStep === 2" @click="importStep = 1">上一步</el-button>
        <el-button v-if="importStep === 2" type="primary" @click="handleConfirmImport">确认导入</el-button>
        <el-button v-if="importStep === 3" type="primary" @click="handleImportComplete">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getMaterialList, createMaterial, updateMaterial, deleteMaterial, downloadMaterialTemplate, previewMaterialImport, confirmMaterialImport } from '@/api/material'
import { getCategoryTree } from '@/api/category'
import { getSupplierList } from '@/api/supplier'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const categoryList = ref([])
const supplierList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

// 批量导入相关
const importDialogVisible = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const importPreview = ref(null)
const importResult = ref(null)

const queryParams = reactive({ current: 1, size: 10, name: '', categoryId: null })
const form = reactive({
  code: '',
  name: '',
  englishName: '',
  casNumber: '',
  categoryId: null,
  specification: '',
  matrix: '',
  packageForm: '',
  supplierId: null
})
const rules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  specification: [{ required: true, message: '请输入规格', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMaterialList(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryTree()
    categoryList.value = flattenTree(res.data || [])
  } catch (e) {}
}

const fetchSuppliers = async () => {
  try {
    const res = await getSupplierList({ current: 1, size: 1000 })
    console.log('供应商列表:', res)
    supplierList.value = res.data?.records || []
  } catch (e) {
    console.error('获取供应商列表失败:', e)
  }
}

const flattenTree = (tree, result = []) => {
  tree.forEach(node => {
    result.push({ id: node.id, label: node.name })
    if (node.children?.length) flattenTree(node.children, result)
  })
  return result
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, {
    code: '',
    name: '',
    englishName: '',
    casNumber: '',
    categoryId: null,
    specification: '',
    matrix: '',
    packageForm: '',
    supplierId: null
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateMaterial(editId.value, form)
  } else {
    await createMaterial(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该标准物质？')
  await deleteMaterial(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

// 下载导入模板
const handleDownloadTemplate = async () => {
  try {
    const res = await downloadMaterialTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '标准物质导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}

// 打开导入对话框
const handleOpenImport = () => {
  importStep.value = 1
  importFile.value = null
  importPreview.value = null
  importResult.value = null
  importDialogVisible.value = true
}

// 文件选择变化
const handleFileChange = (file) => {
  importFile.value = file.raw
  return false // 阻止自动上传
}

// 预览导入数据
const handlePreviewImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  try {
    const res = await previewMaterialImport(importFile.value)
    importPreview.value = res.data
    importStep.value = 2
  } catch (e) {
    ElMessage.error('预览失败：' + (e.message || '未知错误'))
  }
}

// 确认导入
const handleConfirmImport = async () => {
  if (!importPreview.value?.items?.length) {
    ElMessage.warning('没有可导入的数据')
    return
  }
  const validItems = importPreview.value.items.filter(item => item.valid)
  if (validItems.length === 0) {
    ElMessage.warning('没有有效的数据可导入')
    return
  }
  try {
    const res = await confirmMaterialImport(validItems)
    importResult.value = res.data
    importStep.value = 3
    fetchData()
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '未知错误'))
  }
}

// 完成导入
const handleImportComplete = () => {
  importDialogVisible.value = false
}

onMounted(() => {
  fetchData()
  fetchCategories()
  fetchSuppliers()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
</style>
