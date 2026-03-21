<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeType" @tab-change="fetchData">
        <el-tab-pane label="入库原因" name="STOCK_IN_REASON" />
        <el-tab-pane label="出库原因" name="STOCK_OUT_REASON" />
        <el-tab-pane label="储存条件" name="STORAGE_CONDITION" />
      </el-tabs>

      <el-button type="primary" style="margin-bottom: 16px" @click="handleAdd">新增</el-button>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="编码" min-width="100" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="sortOrder" label="排序" min-width="70" />
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑' : '新增'" width="400">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="60px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" style="width: 100%" />
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getMetadataByType, createMetadata, updateMetadata } from '@/api/metadata'

const activeType = ref('STOCK_IN_REASON')
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const form = reactive({ code: '', name: '', sortOrder: 1 })
const rules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMetadataByType(activeType.value)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { code: '', name: '', sortOrder: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  const data = { ...form, type: activeType.value }
  if (editId.value) {
    await updateMetadata(editId.value, data)
  } else {
    await createMetadata(data)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

fetchData()
</script>

<style scoped>.page-container { padding: 20px; }</style>
