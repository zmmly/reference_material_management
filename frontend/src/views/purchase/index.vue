<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的申请" name="my">
          <el-button type="primary" style="margin-bottom: 16px" @click="handleAdd">新建采购申请</el-button>
          <el-table :data="myApplications" v-loading="loading" border>
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="quantity" label="采购数量" width="100" />
            <el-table-column prop="estimatedPrice" label="预估单价" width="100" />
            <el-table-column prop="supplierName" label="供应商" width="150" />
            <el-table-column prop="estimatedArrivalDate" label="预计到货日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applyTime" label="申请时间" width="160" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button link type="warning" @click="handleCancel(row)">撤回</el-button>
                </template>
                <template v-if="row.status === 1">
                  <el-button link type="success" @click="handleReceive(row)">确认到货</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="待审批" name="pending" v-if="canApprove">
          <el-table :data="pendingList" v-loading="loading" border>
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="quantity" label="采购数量" width="100" />
            <el-table-column prop="estimatedPrice" label="预估单价" width="100" />
            <el-table-column prop="supplierName" label="供应商" width="150" />
            <el-table-column prop="reason" label="采购原因" />
            <el-table-column prop="applyTime" label="申请时间" width="160" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="success" @click="handleApprove(row, true)">通过</el-button>
                <el-button link type="danger" @click="handleApprove(row, false)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" title="采购申请" width="600">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="标准物质" prop="materialId">
          <el-select v-model="form.materialId" placeholder="请选择" filterable style="width: 100%">
            <el-option v-for="item in materialList" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预估单价">
              <el-input-number v-model="form.estimatedPrice" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in supplierList" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到货日期">
              <el-date-picker v-model="form.estimatedArrivalDate" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="采购原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>
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
  materialId: null, quantity: 1, estimatedPrice: null,
  supplierId: null, estimatedArrivalDate: null, reason: ''
})
const rules = {
  materialId: [{ required: true, message: '请选择标准物质', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入采购原因', trigger: 'blur' }]
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

const handleAdd = () => {
  Object.assign(form, {
    materialId: null, quantity: 1, estimatedPrice: null,
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
  await ElMessageBox.confirm('确定该采购已到货？将自动生成入库记录')
  await receivePurchase(row.id)
  ElMessage.success('已确认到货')
  fetchData()
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'primary' }[s] || 'info')
const statusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已撤回', 4: '已到货' }[s] || '未知')

onMounted(() => {
  fetchData()
  fetchMaterials()
})
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
