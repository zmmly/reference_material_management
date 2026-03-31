<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的申请" name="my">
          <el-table :data="myApplications" v-loading="loading" border>
            <el-table-column prop="materialCode" label="编号" min-width="130" show-overflow-tooltip />
            <el-table-column prop="materialName" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="casNumber" label="CAS号" min-width="110" show-overflow-tooltip />
            <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
            <el-table-column prop="internalCode" label="内部编号" min-width="110" show-overflow-tooltip />
            <el-table-column prop="quantity" label="申请数量" min-width="90" />
            <el-table-column prop="reason" label="出库原因" min-width="90">
              <template #default="{ row }">{{ reasonText(row.reason) }}</template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途说明" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applyTime" label="申请时间" min-width="150" />
            <el-table-column label="操作" min-width="140" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button v-if="row.status === 0" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                  <el-button v-if="row.status === 0" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                  <el-button v-if="row.status === 0" link type="warning" size="small" @click="handleCancel(row)">撤回</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="待审批" name="pending" v-if="canApprove">
          <el-table :data="pendingList" v-loading="loading" border>
            <el-table-column prop="applicantName" label="申请人" min-width="100" show-overflow-tooltip />
            <el-table-column prop="materialCode" label="编号" min-width="130" show-overflow-tooltip />
            <el-table-column prop="materialName" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="casNumber" label="CAS号" min-width="110" show-overflow-tooltip />
            <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
            <el-table-column prop="internalCode" label="内部编号" min-width="110" show-overflow-tooltip />
            <el-table-column prop="quantity" label="申请数量" min-width="90" />
            <el-table-column prop="reason" label="出库原因" min-width="90">
              <template #default="{ row }">{{ reasonText(row.reason) }}</template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途说明" min-width="150" show-overflow-tooltip />
            <el-table-column prop="applyTime" label="申请时间" min-width="150" />
            <el-table-column label="操作" min-width="140" fixed="right">
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

    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="400">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑出库申请对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑出库申请" width="500">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="出库原因" required>
          <el-select v-model="editForm.reason" placeholder="请选择出库原因" style="width: 100%">
            <el-option label="实验使用" value="EXPERIMENT" />
            <el-option label="过期销毁" value="EXPIRED" />
            <el-option label="报废" value="SCRAP" />
            <el-option label="调拨出" value="TRANSFER_OUT" />
            <el-option label="赠送" value="DONATE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="editForm.purpose" type="textarea" :rows="3" placeholder="请输入用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStockOutList, approveStockOut, cancelStockOut, updateStockOut, deleteStockOut } from '@/api/stock'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('my')
const allApplications = ref([])
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

// 编辑相关
const editDialogVisible = ref(false)
const editForm = reactive({
  id: null,
  reason: '',
  purpose: ''
})

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

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStockOutList({ current: 1, size: 100 })
    allApplications.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定撤回该申请？')
  await cancelStockOut(row.id)
  ElMessage.success('已撤回')
  fetchData()
}

const handleApprove = async (row, approved) => {
  if (approved) {
    await ElMessageBox.confirm('确定通过该申请？')
    await approveStockOut(row.id, true, '')
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
  await approveStockOut(currentRow.value.id, false, rejectReason.value)
  ElMessage.success('已拒绝')
  rejectDialogVisible.value = false
  fetchData()
}

// 编辑出库申请
const handleEdit = (row) => {
  editForm.id = row.id
  editForm.reason = row.reason
  editForm.purpose = row.purpose || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.reason) {
    ElMessage.warning('请选择出库原因')
    return
  }
  await updateStockOut(editForm.id, {
    reason: editForm.reason,
    purpose: editForm.purpose
  })
  ElMessage.success('修改成功')
  editDialogVisible.value = false
  fetchData()
}

// 删除出库申请
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该出库申请？', '删除确认', { type: 'warning' })
  await deleteStockOut(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')
const statusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已撤回' }[s] || '未知')
const reasonText = (r) => ({
  EXPERIMENT: '实验使用', EXPIRED: '过期销毁', SCRAP: '报废', TRANSFER_OUT: '调拨出', DONATE: '赠送', OTHER: '其他'
}[r] || r)

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
