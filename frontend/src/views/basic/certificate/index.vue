<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="标准物质">
          <el-input v-model="queryParams.keyword" placeholder="物质名称" clearable />
        </el-form-item>
        <el-form-item label="批号">
          <el-input v-model="queryParams.batchNo" placeholder="请输入批号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">上传证书</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="materialName" label="标准物质" min-width="180" />
        <el-table-column prop="batchNo" label="批号" min-width="120" />
        <el-table-column prop="fileName" label="证书文件" min-width="150">
          <template #default="{ row }">
            <span>{{ row.fileName || '证书文件' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" min-width="100" />
        <el-table-column prop="createTime" label="上传时间" min-width="160" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="viewCertificate(row.filePath)">查看</el-button>
              <el-button type="warning" size="small" @click="handleReplace(row)">替换</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/替换证书对话框 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '替换证书' : '上传证书'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标准物质" prop="materialId">
          <el-select v-model="form.materialId" placeholder="请选择标准物质" filterable :disabled="!!editId" style="width: 100%">
            <el-option v-for="m in materialList" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="请输入批号" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="证书文件" prop="filePath">
          <el-upload
            v-model:file-list="fileList"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :limit="1"
            :on-exceed="handleExceed"
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、JPG、PNG、DOC 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="!form.filePath">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCertificateList, createCertificate, updateCertificate, deleteCertificate } from '@/api/certificate'
import { getAllMaterials } from '@/api/material'
import { getToken } from '@/utils/auth'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()
const fileList = ref([])
const materialList = ref([])

const uploadUrl = '/api/upload?type=certificate'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

const queryParams = reactive({ current: 1, size: 10, keyword: '', batchNo: '' })
const form = reactive({ materialId: null, batchNo: '', filePath: '', fileName: '' })
const rules = {
  materialId: [{ required: true, message: '请选择标准物质', trigger: 'change' }],
  batchNo: [{ required: true, message: '请输入批号', trigger: 'blur' }],
  filePath: [{ required: true, message: '请上传证书文件', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCertificateList(queryParams)
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
  } catch (e) {
    // fallback: if getAllMaterials not available, leave empty
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { materialId: null, batchNo: '', filePath: '', fileName: '' })
  fileList.value = []
  dialogVisible.value = true
}

const handleReplace = (row) => {
  editId.value = row.id
  Object.assign(form, {
    materialId: row.materialId,
    batchNo: row.batchNo,
    filePath: row.filePath,
    fileName: row.fileName || ''
  })
  fileList.value = row.filePath ? [{ name: row.fileName || '证书文件', url: row.filePath }] : []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateCertificate(editId.value, {
      filePath: form.filePath,
      fileName: form.fileName
    })
    ElMessage.success('证书已替换')
  } else {
    await createCertificate(form)
    ElMessage.success('证书上传成功')
  }
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该证书？删除后相关入库记录将无法查看证书。', '提示', { type: 'warning' })
  await deleteCertificate(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const viewCertificate = (path) => {
  window.open(`/api/upload/preview?path=${encodeURIComponent(path)}`, '_blank')
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    form.filePath = response.data
    // 从上传的文件名中提取原始文件名
    const fileItem = fileList.value[fileList.value.length - 1]
    if (fileItem) {
      form.fileName = fileItem.name
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已上传的文件')
}

onMounted(() => {
  fetchData()
  fetchMaterials()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
.el-upload__tip { color: #999; font-size: 12px; }
</style>
