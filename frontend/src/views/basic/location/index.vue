<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="位置名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="位置编码" width="120" />
        <el-table-column prop="name" label="位置名称" />
        <el-table-column prop="temperature" label="温度要求" width="100" />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑位置' : '新增位置'" width="500">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="位置编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="位置名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="温度要求">
          <el-select v-model="form.temperature" placeholder="请选择" style="width: 100%">
            <el-option label="-20℃" value="-20℃" />
            <el-option label="2-8℃" value="2-8℃" />
            <el-option label="常温" value="常温" />
            <el-option label="阴凉干燥" value="阴凉干燥" />
            <el-option label="10-30℃" value="10-30℃" />
          </el-select>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
import { ElMessage } from 'element-plus'
import { getLocationList, createLocation, updateLocation } from '@/api/location'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, name: '' })
const form = reactive({ code: '', name: '', temperature: '', capacity: null, description: '' })
const rules = {
  code: [{ required: true, message: '请输入位置编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入位置名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLocationList(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { code: '', name: '', temperature: '', capacity: null, description: '' })
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
    await updateLocation(editId.value, form)
  } else {
    await createLocation(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
