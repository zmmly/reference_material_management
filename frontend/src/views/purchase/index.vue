<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的申请" name="my">
          <el-button type="primary" style="margin-bottom: 16px" @click="handleAdd">新建采购申请</el-button>
          <el-table :data="myApplications" v-loading="loading" border>
            <el-table-column prop="purchaseNo" label="采购单号" min-width="130" />
            <el-table-column prop="applicantName" label="申请人" min-width="80" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="specification" label="规格" min-width="100" />
            <el-table-column prop="batchNumber" label="批号" min-width="100" />
            <el-table-column prop="quantity" label="数量" min-width="70" />
            <el-table-column prop="unit" label="单位" min-width="60" />
            <el-table-column prop="estimatedPrice" label="单价" min-width="70" />
            <el-table-column prop="totalAmount" label="金额" min-width="80">
              <template #default="{ row }">
                {{ row.totalAmount ? row.totalAmount.toFixed(2) : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="supplierName" label="供应商" min-width="120" />
            <el-table-column prop="estimatedArrivalDate" label="预计到货" min-width="100" />
            <el-table-column prop="status" label="状态" min-width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applyTime" label="申请时间" min-width="140" />
            <el-table-column label="操作" min-width="120" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <template v-if="row.status === 0">
                    <el-button link type="warning" size="small" @click="handleCancel(row)">撤回</el-button>
                  </template>
                  <template v-if="row.status === 1">
                    <el-button link type="success" size="small" @click="handleReceive(row)">确认到货</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="待审批" name="pending" v-if="canApprove">
          <el-table :data="pendingList" v-loading="loading" border>
            <el-table-column prop="applicantName" label="申请人" min-width="80" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="specification" label="规格" min-width="100" />
            <el-table-column prop="batchNumber" label="批号" min-width="100" />
            <el-table-column prop="quantity" label="数量" min-width="70" />
            <el-table-column prop="unit" label="单位" min-width="60" />
            <el-table-column prop="estimatedPrice" label="单价" min-width="70" />
            <el-table-column prop="totalAmount" label="金额" min-width="80">
              <template #default="{ row }">
                {{ row.totalAmount ? row.totalAmount.toFixed(2) : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="supplierName" label="供应商" min-width="120" />
            <el-table-column prop="reason" label="采购原因" />
            <el-table-column prop="applyTime" label="申请时间" min-width="140" />
            <el-table-column label="操作" min-width="150" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button link type="success" size="small" @click="handleApprove(row, true)">通过</el-button>
                  <el-button link type="danger" size="small" @click="handleApprove(row, false)">拒绝</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" title="采购申请" width="750">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="标准物质" prop="materialId">
              <el-select v-model="form.materialId" placeholder="请选择" filterable style="width: 100%" @change="handleMaterialChange">
                <el-option v-for="item in materialList" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="如：100mg/支" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批号" prop="batchNumber">
              <el-input v-model="form.batchNumber" placeholder="批号" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择" style="width: 100%">
                <el-option label="支" value="支" />
                <el-option label="瓶" value="瓶" />
                <el-option label="盒" value="盒" />
                <el-option label="套" value="套" />
                <el-option label="包" value="包" />
                <el-option label="袋" value="袋" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" @change="calculateAmount" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单价" prop="estimatedPrice">
              <el-input-number v-model="form.estimatedPrice" :precision="2" :min="0" @change="calculateAmount" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金额">
              <el-input v-model="totalAmountDisplay" disabled placeholder="自动计算" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="item in supplierList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到货">
              <el-date-picker v-model="form.estimatedArrivalDate" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="采购原因" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="400">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPurchaseList, getAllPurchaseList, applyPurchase, approvePurchase, cancelPurchase, receivePurchase } from '@/api/purchase'
import { getAllMaterials } from '@/api/material'
import { getAllSuppliers } from '@/api/supplier'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('my')
const allApplications = ref([])
const dialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)
const materialList = ref([])
const supplierList = ref([])
const formRef = ref()

const canApprove = computed(() => {
  const roleCode = userStore.userInfo?.roleCode
  return roleCode === 'ADMIN' || roleCode === 'MANAGER'
})

const myApplications = computed(() => {
  return allApplications.value.filter(item => item.applicantId === userStore.userInfo?.id)
})

const pendingList = computed(() => {
  return allApplications.value.filter(item => item.status === 0)
})

const form = reactive({
  materialId: null, specification: '', batchNumber: '', unit: '支',
  quantity: 1, estimatedPrice: null,
  supplierId: null, estimatedArrivalDate: null, reason: ''
})
const rules = {
  materialId: [{ required: true, message: '请选择标准物质', trigger: 'change' }],
  specification: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  batchNumber: [{ required: true, message: '请输入批号', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }],
  estimatedPrice: [{ required: true, message: '请输入预估单价', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  reason: [{ required: true, message: '请输入采购原因', trigger: 'blur' }]
}

const totalAmountDisplay = computed(() => {
  if (form.quantity && form.estimatedPrice) {
    return (form.quantity * form.estimatedPrice).toFixed(2)
  }
  return ''
})

const calculateAmount = () => {
  // 金额自动计算，无需额外逻辑
}

const handleMaterialChange = (materialId) => {
  const material = materialList.value.find(item => item.id === materialId)
  if (material) {
    // 可根据标准物质信息自动填充规格等
    form.specification = material.specification || ''
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAllPurchaseList({ current: 1, size: 100 })
    allApplications.value = res.data?.records || []
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

const fetchSuppliers = async () => {
  try {
    const res = await getAllSuppliers()
    supplierList.value = res.data || []
  } catch (e) {}
}

const handleAdd = () => {
  Object.assign(form, {
    materialId: null, specification: '', batchNumber: '', unit: '支',
    quantity: 1, estimatedPrice: null,
    supplierId: null, estimatedArrivalDate: null, reason: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await applyPurchase(form)
  ElMessage.success('申请提交成功')
  dialogVisible.value = false
  fetchData()
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定撤回该申请？')
  await cancelPurchase(row.id)
  ElMessage.success('已撤回')
  fetchData()
}

const handleApprove = async (row, approved) => {
  if (approved) {
    await ElMessageBox.confirm('确定通过该采购申请？')
    await approvePurchase(row.id, true, '')
    ElMessage.success('已通过')
    fetchData()
  } else {
    currentRow.value = row
    rejectReason.value = ''
    rejectDialogVisible.value = true
  }
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  await approvePurchase(currentRow.value.id, false, rejectReason.value)
  ElMessage.success('已拒绝')
  rejectDialogVisible.value = false
  fetchData()
}

const handleReceive = async (row) => {
  await ElMessageBox.confirm('确定该采购已到货？将自动生成验货申请单。')
  await receivePurchase(row.id)
  ElMessage.success('已生成验收申请单')
  fetchData()
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'primary', 6: 'success', 7: 'danger' }[s] || 'info')
const statusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已撤回', 4: '待验收', 6: '验收通过', 7: '验收拒绝' }[s] || '未知')

onMounted(() => {
  fetchData()
  fetchMaterials()
  fetchSuppliers()
})
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
