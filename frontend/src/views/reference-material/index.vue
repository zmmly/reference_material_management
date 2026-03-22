<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId" placeholder="全部" clearable>
            <el-option v-for="item in categoryList" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="code" label="编号" min-width="120" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="casNumber" label="CAS号" min-width="110" />
        <el-table-column prop="categoryName" label="分类" min-width="100" />
        <el-table-column prop="specification" label="规格" min-width="80" />
        <el-table-column prop="purityConcentration" label="纯度/浓度" min-width="80" />
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
            <el-form-item label="CAS号">
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
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="请输入规格" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="纯度/浓度" prop="purityConcentration">
              <el-input v-model="form.purityConcentration" placeholder="请输入纯度/浓度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基质">
              <el-input v-model="form.matrix" placeholder="请输入基质" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="包装形式">
              <el-input v-model="form.packageForm" placeholder="请输入包装形式" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { getMaterialList, createMaterial, updateMaterial, deleteMaterial } from '@/api/material'
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

const queryParams = reactive({ current: 1, size: 10, name: '', categoryId: null })
const form = reactive({
  code: '',
  name: '',
  englishName: '',
  casNumber: '',
  categoryId: null,
  specification: '',
  purityConcentration: '',
  matrix: '',
  packageForm: '',
  supplierId: null
})
const rules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  specification: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  purityConcentration: [{ required: true, message: '请输入纯度/浓度', trigger: 'blur' }]
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
    const res = await getAllSuppliers()
    supplierList.value = res.data || []
  } catch (e) {}
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
    purityConcentration: '',
    matrix: '',
    packageForm: ''
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
