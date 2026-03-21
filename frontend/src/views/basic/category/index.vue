<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-button type="primary" @click="handleAdd()">新增顶级分类</el-button>
      </template>

      <el-table :data="tableData" v-loading="loading" row-key="id" border default-expand-all>
        <el-table-column prop="name" label="分类名称" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" size="small" @click="handleAdd(row)">添加子级</el-button>
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="parentId ? '添加子分类' : (editId ? '编辑分类' : '新增分类')" width="400">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" />
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
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const parentId = ref(null)
const formRef = ref()

const form = reactive({ name: '' })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryTree()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = (row) => {
  editId.value = null
  parentId.value = row?.id || 0
  form.name = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  parentId.value = null
  form.name = row.name
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateCategory(editId.value, { name: form.name })
  } else {
    await createCategory({ name: form.name, parentId: parentId.value })
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该分类？')
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
