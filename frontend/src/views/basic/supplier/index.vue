<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="供应商名称">
          <el-input v-model="queryParams.keyword" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="name" label="供应商名称" min-width="180" />
        <el-table-column prop="contact" label="联系人" min-width="100" />
        <el-table-column prop="phone" label="联系电话" min-width="120" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="status" label="状态" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑供应商' : '新增供应商'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSupplierList, createSupplier, updateSupplier, deleteSupplier } from '@/api/supplier'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, keyword: '', status: null })
const form = reactive({ name: '', contact: '', phone: '', address: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSupplierList(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { name: '', contact: '', phone: '', address: '', status: 1 })
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
    await updateSupplier(editId.value, form)
    ElMessage.success('更新成功')
  } else {
    await createSupplier(form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该供应商？', '提示', { type: 'warning' })
  await deleteSupplier(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
</style>
