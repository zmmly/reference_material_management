<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="queryParams.keyword" placeholder="名称/编码/批号" clearable />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-select v-model="queryParams.locationId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="正常" :value="1" />
            <el-option label="即将过期" :value="2" />
            <el-option label="已过期" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="warning" @click="handleBatchOut" :disabled="selectedRows.length === 0">
            批量出库 ({{ selectedRows.length }})
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="60" :selectable="canSelect" />
        <el-table-column prop="materialCode" label="标准物质编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="materialName" label="标准物质名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="casNumber" label="CAS号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批号" min-width="120" />
        <el-table-column prop="internalCode" label="内部编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="quantity" label="库存数量" min-width="90">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.quantity <= 0 }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" min-width="130">
          <template #default="{ row }">
            <span :class="{ 'text-warning': isWarning(row.expiryDate), 'text-danger': isExpired(row.expiryDate) }">
              {{ row.expiryDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="存放位置" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="出库状态" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.hasApprovedOut" type="success" size="small">已出库</el-tag>
            <el-tag v-else-if="row.hasPendingOut" type="warning" size="small">审批中</el-tag>
            <span v-else class="text-muted">未出库</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-tooltip v-if="row.hasPendingOut" content="已有待审批的出库申请" placement="top">
                <el-button type="info" size="small" disabled>出库</el-button>
              </el-tooltip>
              <el-button v-else type="primary" size="small" @click="handleOut(row)" :disabled="row.status !== 1">出库</el-button>
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

    <!-- 批量出库弹窗 -->
    <el-dialog v-model="batchOutDialogVisible" title="批量出库申请" width="500">
      <el-form :model="batchOutForm" label-width="100px">
        <el-form-item label="已选数量">
          <el-tag>{{ selectedRows.length }} 件</el-tag>
        </el-form-item>
        <el-form-item label="出库原因" required>
          <el-select v-model="batchOutForm.reason" placeholder="请选择" style="width: 100%">
            <el-option label="实验使用" value="EXPERIMENT" />
            <el-option label="过期销毁" value="EXPIRED" />
            <el-option label="报废" value="SCRAP" />
            <el-option label="调拨出" value="TRANSFER_OUT" />
            <el-option label="赠送" value="DONATE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="batchOutForm.purpose" type="textarea" :rows="3" placeholder="请输入用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchOutDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchOut" :loading="batchOutLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStockList, batchApplyStockOut } from '@/api/stock'
import { getAllLocations } from '@/api/location'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const locationList = ref([])
const selectedRows = ref([])
const batchOutDialogVisible = ref(false)
const batchOutLoading = ref(false)
const batchOutForm = reactive({ reason: '', purpose: '' })

const queryParams = reactive({ current: 1, size: 10, keyword: '', locationId: null, status: null })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStockList(queryParams)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchLocations = async () => {
  try {
    const res = await getAllLocations()
    locationList.value = res.data || []
  } catch (e) {}
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

// 只能选择状态正常且没有待审批出库申请的库存
const canSelect = (row) => row.status === 1 && !row.hasPendingOut

const handleOut = (row) => router.push({ path: '/stock-out/apply', query: { stockId: row.id } })

const handleBatchOut = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要出库的库存')
    return
  }
  batchOutForm.reason = ''
  batchOutForm.purpose = ''
  batchOutDialogVisible.value = true
}

const confirmBatchOut = async () => {
  if (!batchOutForm.reason) {
    ElMessage.warning('请选择出库原因')
    return
  }
  batchOutLoading.value = true
  try {
    await batchApplyStockOut({
      stockIds: selectedRows.value.map(r => r.id),
      reason: batchOutForm.reason,
      purpose: batchOutForm.purpose
    })
    ElMessage.success('批量出库申请成功')
    batchOutDialogVisible.value = false
    fetchData()
  } finally {
    batchOutLoading.value = false
  }
}

const isWarning = (date) => {
  if (!date) return false
  const d = new Date(date)
  const now = new Date()
  const monthLater = new Date(now.setMonth(now.getMonth() + 1))
  return d <= monthLater && d > new Date()
}

const isExpired = (date) => date && new Date(date) < new Date()

const statusType = (s) => ({ 1: 'success', 2: 'warning', 3: 'danger' }[s] || 'info')
const statusText = (s) => ({ 1: '正常', 2: '即将过期', 3: '已过期' }[s] || '未知')

onMounted(() => {
  fetchData()
  fetchLocations()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.search-form { margin-bottom: 20px; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.text-muted { color: #909399; }
</style>
