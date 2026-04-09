<template>
  <div class="page-container">
    <el-card>
      <!-- 搜索条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="申请人">
          <el-input v-model="searchForm.applicantName" placeholder="请输入申请人姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物质编号">
          <el-input v-model="searchForm.materialCode" placeholder="请输入物质编号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="物质名称">
          <el-input v-model="searchForm.materialName" placeholder="请输入物质名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的申请" name="my">
          <el-table :data="myData" v-loading="myLoading" border>
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
          <el-pagination
            v-model:current-page="myPage.current"
            v-model:page-size="myPage.size"
            :total="myPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="fetchMyData"
            @current-change="fetchMyData"
          />
        </el-tab-pane>

        <el-tab-pane label="待审批" name="pending" v-if="canApprove">
          <el-table :data="pendingData" v-loading="pendingLoading" border>
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
          <el-pagination
            v-model:current-page="pendingPage.current"
            v-model:page-size="pendingPage.size"
            :total="pendingPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="fetchPendingData"
            @current-change="fetchPendingData"
          />
        </el-tab-pane>

        <el-tab-pane label="已审批" name="approved" v-if="canApprove">
          <el-table :data="approvedData" v-loading="approvedLoading" border>
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
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审批人" min-width="100" show-overflow-tooltip />
            <el-table-column prop="applyTime" label="申请时间" min-width="150" />
            <el-table-column prop="approveTime" label="审批时间" min-width="150" />
            <el-table-column prop="rejectReason" label="拒绝原因" min-width="150" show-overflow-tooltip />
          </el-table>
          <el-pagination
            v-model:current-page="approvedPage.current"
            v-model:page-size="approvedPage.size"
            :total="approvedPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
            @size-change="fetchApprovedData"
            @current-change="fetchApprovedData"
          />
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
const activeTab = ref('my')

// 搜索条件
const searchForm = reactive({
  applicantName: '',
  materialCode: '',
  materialName: ''
})

// 获取搜索参数（排除 applicantName 对"我的申请"tab无效的情况，仍传递给后端）
const getSearchParams = () => ({
  applicantName: searchForm.applicantName || undefined,
  materialCode: searchForm.materialCode || undefined,
  materialName: searchForm.materialName || undefined
})

// 我的申请
const myLoading = ref(false)
const myData = ref([])
const myPage = reactive({ current: 1, size: 10, total: 0 })

const fetchMyData = async () => {
  myLoading.value = true
  try {
    const res = await getStockOutList({
      current: myPage.current,
      size: myPage.size,
      applicantId: userStore.userInfo?.id,
      ...getSearchParams()
    })
    myData.value = res.data?.records || []
    myPage.total = res.data?.total || 0
  } finally {
    myLoading.value = false
  }
}

// 待审批
const pendingLoading = ref(false)
const pendingData = ref([])
const pendingPage = reactive({ current: 1, size: 10, total: 0 })

const fetchPendingData = async () => {
  pendingLoading.value = true
  try {
    const res = await getStockOutList({
      current: pendingPage.current,
      size: pendingPage.size,
      status: 0,
      ...getSearchParams()
    })
    pendingData.value = res.data?.records || []
    pendingPage.total = res.data?.total || 0
  } finally {
    pendingLoading.value = false
  }
}

// 已审批（状态为已通过1或已拒绝2）
const approvedLoading = ref(false)
const approvedData = ref([])
const approvedPage = reactive({ current: 1, size: 10, total: 0 })

const fetchApprovedData = async () => {
  approvedLoading.value = true
  try {
    // 先查已通过
    const res1 = await getStockOutList({
      current: 1,
      size: 999,
      status: 1,
      ...getSearchParams()
    })
    const passed = res1.data?.records || []
    // 再查已拒绝
    const res2 = await getStockOutList({
      current: 1,
      size: 999,
      status: 2,
      ...getSearchParams()
    })
    const rejected = res2.data?.records || []
    // 合并并按审批时间排序
    const all = [...passed, ...rejected].sort((a, b) => {
      const ta = a.approveTime ? new Date(a.approveTime).getTime() : 0
      const tb = b.approveTime ? new Date(b.approveTime).getTime() : 0
      return tb - ta
    })
    approvedPage.total = all.length
    const start = (approvedPage.current - 1) * approvedPage.size
    approvedData.value = all.slice(start, start + approvedPage.size)
  } finally {
    approvedLoading.value = false
  }
}

const canApprove = computed(() => {
  const roleCode = userStore.userInfo?.roleCode
  return roleCode === 'ADMIN' || roleCode === 'MANAGER'
})

// 搜索
const handleSearch = () => {
  refreshCurrentTab()
}

const handleReset = () => {
  searchForm.applicantName = ''
  searchForm.materialCode = ''
  searchForm.materialName = ''
  refreshCurrentTab()
}

// 切换tab时刷新对应数据
const handleTabChange = (tab) => {
  refreshCurrentTab()
}

const refreshCurrentTab = () => {
  if (activeTab.value === 'my') {
    myPage.current = 1
    fetchMyData()
  } else if (activeTab.value === 'pending') {
    pendingPage.current = 1
    fetchPendingData()
  } else if (activeTab.value === 'approved') {
    approvedPage.current = 1
    fetchApprovedData()
  }
}

// 审批后刷新当前tab
const refreshAfterAction = () => {
  if (activeTab.value === 'pending') {
    fetchPendingData()
  } else if (activeTab.value === 'my') {
    fetchMyData()
  }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定撤回该申请？')
  await cancelStockOut(row.id)
  ElMessage.success('已撤回')
  refreshAfterAction()
}

const handleApprove = async (row, approved) => {
  if (approved) {
    await ElMessageBox.confirm('确定通过该申请？')
    await approveStockOut(row.id, true, '')
    ElMessage.success('已通过')
    refreshAfterAction()
  } else {
    currentRow.value = row
    rejectReason.value = ''
    rejectDialogVisible.value = true
  }
}

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  await approveStockOut(currentRow.value.id, false, rejectReason.value)
  ElMessage.success('已拒绝')
  rejectDialogVisible.value = false
  refreshAfterAction()
}

// 编辑出库申请
const editDialogVisible = ref(false)
const editForm = reactive({
  id: null,
  reason: '',
  purpose: ''
})

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
  refreshAfterAction()
}

// 删除出库申请
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该出库申请？', '删除确认', { type: 'warning' })
  await deleteStockOut(row.id)
  ElMessage.success('删除成功')
  refreshAfterAction()
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')
const statusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已撤回' }[s] || '未知')
const reasonText = (r) => ({
  EXPERIMENT: '实验使用', EXPIRED: '过期销毁', SCRAP: '报废', TRANSFER_OUT: '调拨出', DONATE: '赠送', OTHER: '其他'
}[r] || r)

onMounted(() => fetchMyData())
</script>

<style scoped>
.page-container { padding: 20px; }
.action-buttons {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
