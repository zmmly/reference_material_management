<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="入库原因">
          <el-select v-model="queryParams.reason" placeholder="全部" clearable>
            <el-option label="新购入" value="PURCHASE" />
            <el-option label="盘盈" value="SURPLUS" />
            <el-option label="归还" value="RETURN" />
            <el-option label="调拨入" value="TRANSFER_IN" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="success" @click="handleAdd">入库登记</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="materialName" label="标准物质" min-width="150" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="internalCode" label="内部编码" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="row.internalCode">{{ row.internalCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="入库数量" width="100" />
        <el-table-column prop="supplierName" label="供应商" width="150" />
        <el-table-column prop="expiryDate" label="有效期" width="120" />
        <el-table-column prop="locationName" label="存放位置" width="120" />
        <el-table-column prop="reason" label="入库原因" width="100">
          <template #default="{ row }">
            {{ reasonText(row.reason) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="createTime" label="入库时间" width="160" />
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
            <el-option v-for="item in materialList" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
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
          :title="`内部编码将自动生成: ${form.batchNo.toUpperCase()}-NNN`"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%">
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
import { getStockInList, createStockIn } from '@/api/stock'
import { getAllMaterials } from '@/api/material'
import { getAllLocations } from '@/api/location'
import { getAllSuppliers } from '@/api/supplier'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const materialList = ref([])
const locationList = ref([])
const supplierList = ref([])
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, reason: '' })
const form = reactive({
  materialId: null, batchNo: '', quantity: 1, supplierId: null,
  expiryDate: null, locationId: null, reason: 'PURCHASE', remarks: ''
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

const handleAdd = () => {
  Object.assign(form, {
    materialId: null, batchNo: '', quantity: 1, supplierId: null,
    expiryDate: null, locationId: null, reason: 'PURCHASE', remarks: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await createStockIn(form)
  ElMessage.success('入库成功')
  dialogVisible.value = false
  fetchData()
}

const reasonText = (r) => ({
  PURCHASE: '新购入', SURPLUS: '盘盈', RETURN: '归还', TRANSFER_IN: '调拨入', OTHER: '其他'
}[r] || r)

onMounted(() => {
  fetchData()
  fetchMaterials()
  fetchLocations()
  fetchSuppliers()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
</style>
