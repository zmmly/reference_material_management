<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="入库时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="标准物质">
          <el-input v-model="queryParams.materialName" placeholder="名称" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="批号">
          <el-input v-model="queryParams.batchNo" placeholder="批号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="入库原因">
          <el-select v-model="queryParams.reason" placeholder="全部" clearable style="width: 120px">
            <el-option label="新购入" value="PURCHASE" />
            <el-option label="盘盈" value="SURPLUS" />
            <el-option label="归还" value="RETURN" />
            <el-option label="调拨入" value="TRANSFER_IN" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-select v-model="queryParams.operatorId" placeholder="全部" clearable filterable style="width: 120px">
            <el-option v-for="item in userList" :key="item.id" :label="item.realName || item.username" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">入库登记</el-button>
          <el-button type="warning" @click="handleExport" :loading="exporting">导出Excel</el-button>
          <el-button type="info" @click="handleDownloadTemplate">下载模板</el-button>
          <el-button type="primary" @click="handleOpenImport">批量导入</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="materialName" label="标准物质" min-width="160" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批号" min-width="110" />
        <el-table-column prop="internalCode" label="内部编号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.internalCode">{{ row.internalCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="入库数量" min-width="90" />
        <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column prop="expiryDate" label="有效期" min-width="110" />
        <el-table-column prop="locationName" label="存放位置" min-width="110" show-overflow-tooltip />
        <el-table-column prop="reason" label="入库原因" min-width="90">
          <template #default="{ row }">
            {{ reasonText(row.reason) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" min-width="100" show-overflow-tooltip />
        <el-table-column label="证书" min-width="90" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.productCertificate" type="primary" size="small" @click="viewCertificate(row.productCertificate)">查看</el-button>
              <span v-else class="text-muted">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="入库时间" min-width="140" />
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="入库登记" width="600">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标准物质" prop="materialId">
          <el-select v-model="form.materialId" placeholder="请选择标准物质" filterable style="width: 100%">
            <el-option v-for="item in materialList" :key="item.id" :label="`${item.code} - ${item.name} - ${item.supplierName || '无供应商'}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="批号" prop="batchNo">
              <el-input v-model="form.batchNo" placeholder="请输入批号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          v-if="form.batchNo"
          :title="`内部编号将自动生成: ${form.batchNo.toUpperCase()}-NNN`"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="自动根据标准物质填充" filterable disabled style="width: 100%">
                <el-option v-for="item in supplierList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期" prop="expiryDate">
              <el-date-picker v-model="form.expiryDate" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="存放位置" prop="locationId">
              <el-select v-model="form.locationId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库原因" prop="reason">
              <el-select v-model="form.reason" placeholder="请选择" style="width: 100%">
                <el-option label="新购入" value="PURCHASE" />
                <el-option label="盘盈" value="SURPLUS" />
                <el-option label="归还" value="RETURN" />
                <el-option label="调拨入" value="TRANSFER_IN" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="产品证书">
          <el-upload
            v-model:file-list="fileList"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :limit="1"
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
          >
            <el-button type="primary">上传文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、图片、Word 文档，大小不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

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
        <el-tooltip
          v-if="importStep === 2"
          :disabled="importPreview.invalidCount === 0"
          placement="top"
        >
          <template #content>
            <div style="max-width: 300px;">
              存在 {{ importPreview.invalidCount }} 条无效数据，请修正后再导入。<br/>
              无效数据行：{{ importPreview.items.filter(i => !i.valid).map(i => '第' + i.rowNum + '行').join('、') }}
            </div>
          </template>
          <span>
            <el-button
              type="primary"
              :disabled="importPreview.invalidCount > 0"
              :loading="importLoading"
              @click="handleConfirmImport"
            >
              确认导入
            </el-button>
          </span>
        </el-tooltip>
        <el-button
          v-if="importStep === 3"
          type="primary"
          @click="handleImportComplete"
        >
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getStockInList, createStockIn, exportStockIn, downloadStockInTemplate, previewStockInImport, confirmStockInImport } from '@/api/stock'
import { getAllMaterials } from '@/api/material'
import { getAllLocations } from '@/api/location'
import { getAllSuppliers } from '@/api/supplier'
import { getAllUsers } from '@/api/user'
import { getToken } from '@/utils/auth'

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const materialList = ref([])
const locationList = ref([])
const supplierList = ref([])
const userList = ref([])
const formRef = ref()
const fileList = ref([])
const dateRange = ref([])

// 批量导入相关
const importDialogVisible = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const importLoading = ref(false)
const importPreview = ref({ items: [], totalCount: 0, validCount: 0, invalidCount: 0 })
const importResult = ref({ successCount: 0 })
const importUploadRef = ref()

const uploadUrl = '/api/upload?type=certificate'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

const queryParams = reactive({
  current: 1, size: 10, reason: '', materialName: '', batchNo: '', operatorId: null, startDate: '', endDate: ''
})

// 监听日期范围变化
watch(dateRange, (val) => {
  if (val && val.length === 2) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
})

const form = reactive({
  materialId: null, batchNo: '', quantity: 1, supplierId: null,
  expiryDate: null, locationId: null, reason: 'PURCHASE', remarks: '', productCertificate: ''
})

// 监听标准物质选择，自动填充供应商
watch(() => form.materialId, (newMaterialId) => {
  if (newMaterialId) {
    const selectedMaterial = materialList.value.find(item => item.id === newMaterialId)
    if (selectedMaterial && selectedMaterial.supplierId) {
      form.supplierId = selectedMaterial.supplierId
    } else {
      form.supplierId = null
    }
  } else {
    form.supplierId = null
  }
})
const rules = {
  materialId: [{ required: true, message: '请选择标准物质', trigger: 'change' }],
  batchNo: [{ required: true, message: '请输入批号', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择入库原因', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStockInList(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchMaterials = async () => {
  try {
    const res = await getAllMaterials()
    materialList.value = res.data || []
  } catch (e) {}
}

const fetchLocations = async () => {
  try {
    const res = await getAllLocations()
    locationList.value = res.data || []
  } catch (e) {}
}

const fetchSuppliers = async () => {
  try {
    const res = await getAllSuppliers()
    supplierList.value = res.data || []
  } catch (e) {}
}

const fetchUsers = async () => {
  try {
    const res = await getAllUsers()
    userList.value = res.data || []
  } catch (e) {}
}

const handleReset = () => {
  dateRange.value = []
  Object.assign(queryParams, {
    current: 1, size: 10, reason: '', materialName: '', batchNo: '', operatorId: null, startDate: '', endDate: ''
  })
  fetchData()
}

const handleAdd = () => {
  Object.assign(form, {
    materialId: null, batchNo: '', quantity: 1, supplierId: null,
    expiryDate: null, locationId: null, reason: 'PURCHASE', remarks: '', productCertificate: ''
  })
  fileList.value = []
  dialogVisible.value = true
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    form.productCertificate = response.data
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await createStockIn(form)
  ElMessage.success('入库成功')
  dialogVisible.value = false
  fetchData()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const blob = await exportStockIn({
      reason: queryParams.reason,
      materialName: queryParams.materialName,
      batchNo: queryParams.batchNo,
      operatorId: queryParams.operatorId,
      startDate: queryParams.startDate,
      endDate: queryParams.endDate
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `入库记录_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const reasonText = (r) => ({
  PURCHASE: '新购入', SURPLUS: '盘盈', RETURN: '归还', TRANSFER_IN: '调拨入', OTHER: '其他'
}[r] || r)

const viewCertificate = (path) => {
  window.open(`/api/upload/preview?path=${encodeURIComponent(path)}`, '_blank')
}

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

onMounted(() => {
  fetchData()
  fetchMaterials()
  fetchLocations()
  fetchSuppliers()
  fetchUsers()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
.text-muted { color: #909399; }
.error-text { color: #f56c6c; }
</style>
