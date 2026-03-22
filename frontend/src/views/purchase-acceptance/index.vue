<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待验收" name="pending">
          <el-form :inline="true" :model="pendingQueryParams" class="search-form">
            <el-form-item label="采购单号">
              <el-input v-model="pendingQueryParams.purchaseNo" placeholder="采购单号" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item label="标准物质">
              <el-input v-model="pendingQueryParams.materialName" placeholder="标准物质名称" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handlePendingSearch">查询</el-button>
              <el-button @click="handlePendingReset">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="pendingList" v-loading="loading" border>
            <el-table-column prop="purchaseNo" label="采购单号" min-width="130" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="specification" label="规格" min-width="100" />
            <el-table-column prop="batchNumber" label="批号" min-width="100" />
            <el-table-column prop="quantity" label="数量" min-width="70" />
            <el-table-column prop="unit" label="单位" min-width="60" />
            <el-table-column prop="supplierName" label="供应商" min-width="120" />
            <el-table-column prop="acceptanceResultText" label="验收状态" min-width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.acceptanceResult)">{{ row.acceptanceResultText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="140" />
            <el-table-column label="操作" min-width="100" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" size="small" @click="handleStart(row)">开始验收</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pendingQueryParams.current"
            v-model:page-size="pendingQueryParams.size"
            :total="pendingTotal"
            layout="total, sizes, prev, pager, next"
            @change="handlePendingPageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="验收记录" name="history">
          <el-form :inline="true" :model="historyQueryParams" class="search-form">
            <el-form-item label="采购单号">
              <el-input v-model="historyQueryParams.purchaseNo" placeholder="采购单号" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item label="标准物质">
              <el-input v-model="historyQueryParams.materialName" placeholder="标准物质名称" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item label="验收结果">
              <el-select v-model="historyQueryParams.result" placeholder="全部" clearable style="width: 120px">
                <el-option label="通过" :value="1" />
                <el-option label="拒绝" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleHistorySearch">查询</el-button>
              <el-button @click="handleHistoryReset">重置</el-button>
              <el-button type="warning" @click="handleExport" :loading="exporting">导出Excel</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="historyList" v-loading="loading" border>
            <el-table-column prop="purchaseNo" label="采购单号" min-width="130" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="specification" label="规格" min-width="100" />
            <el-table-column prop="batchNumber" label="批号" min-width="100" />
            <el-table-column prop="quantity" label="数量" min-width="70" />
            <el-table-column prop="unit" label="单位" min-width="60" />
            <el-table-column prop="supplierName" label="供应商" min-width="120" />
            <el-table-column prop="acceptanceResultText" label="验收状态" min-width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.acceptanceResult)">{{ row.acceptanceResultText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="acceptanceUserName" label="验收人" min-width="80" />
            <el-table-column prop="acceptanceDate" label="验收日期" min-width="140" />
            <el-table-column label="操作" min-width="80" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="historyQueryParams.current"
            v-model:page-size="historyQueryParams.size"
            :total="historyTotal"
            layout="total, sizes, prev, pager, next"
            @change="handleHistoryPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 验收弹窗 -->
    <el-dialog v-model="acceptDialogVisible" title="采购验收" width="800">
      <div class="acceptance-form">
        <el-descriptions :column="2" border class="info-section">
          <el-descriptions-item label="采购单号">{{ currentAcceptance?.purchaseNo }}</el-descriptions-item>
          <el-descriptions-item label="标准物质">{{ currentAcceptance?.materialName }} ({{ currentAcceptance?.materialCode }})</el-descriptions-item>
          <el-descriptions-item label="规格">{{ currentAcceptance?.specification }}</el-descriptions-item>
          <el-descriptions-item label="批号">{{ currentAcceptance?.batchNumber }}</el-descriptions-item>
          <el-descriptions-item label="采购数量">{{ currentAcceptance?.quantity }} {{ currentAcceptance?.unit }}</el-descriptions-item>
          <el-descriptions-item label="实际到货数量">
            <el-input-number v-if="acceptDialogVisible" v-model="acceptForm.actualQuantity" :min="0" :precision="2" :placeholder="currentAcceptance?.quantity" style="width: 120px" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentAcceptance?.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="预估单价">¥{{ currentAcceptance?.estimatedPrice }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentAcceptance?.totalAmount }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">验收信息</el-divider>

        <el-form ref="acceptFormRef" :model="acceptForm" :rules="acceptRules" label-width="120px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="外包装是否完好" prop="packageIntact">
                <el-radio-group v-model="acceptForm.packageIntact">
                  <el-radio :label="1">是</el-radio>
                  <el-radio :label="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="标签是否完整" prop="labelComplete">
                <el-radio-group v-model="acceptForm.labelComplete">
                  <el-radio :label="1">是</el-radio>
                  <el-radio :label="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="有无破损" prop="hasDamage">
                <el-radio-group v-model="acceptForm.hasDamage">
                  <el-radio :label="0">无</el-radio>
                  <el-radio :label="1">有</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收日期">
                <el-input v-model="todayDate" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="有效期" prop="expiryDate">
                <el-date-picker v-model="acceptForm.expiryDate" type="date" placeholder="选择有效期" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="存放位置" prop="locationId">
                <el-select v-model="acceptForm.locationId" placeholder="请选择" filterable style="width: 100%">
                  <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="验收人">
                <el-input v-model="userName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="验收结果" prop="result">
                <el-radio-group v-model="acceptForm.result">
                  <el-radio :label="1">通过</el-radio>
                  <el-radio :label="2">拒绝</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="acceptForm.remark" type="textarea" :rows="3" placeholder="请输入验收备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="acceptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAcceptance">提交验收</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="验收详情" width="800">
      <div v-if="currentAcceptance">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="采购单号">{{ currentAcceptance.purchaseNo }}</el-descriptions-item>
          <el-descriptions-item label="验收状态">
            <el-tag :type="statusType(currentAcceptance.acceptanceResult)">{{ currentAcceptance.acceptanceResultText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标准物质">{{ currentAcceptance.materialName }} ({{ currentAcceptance.materialCode }})</el-descriptions-item>
          <el-descriptions-item label="规格">{{ currentAcceptance.specification }}</el-descriptions-item>
          <el-descriptions-item label="批号">{{ currentAcceptance.batchNumber }}</el-descriptions-item>
          <el-descriptions-item label="采购数量">{{ currentAcceptance.quantity }} {{ currentAcceptance.unit }}</el-descriptions-item>
          <el-descriptions-item label="实际到货数量">{{ currentAcceptance.actualQuantity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentAcceptance.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentAcceptance.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ formatDate(currentAcceptance.expiryDate) }}</el-descriptions-item>
          <el-descriptions-item label="存放位置">{{ currentAcceptance.locationName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="外包装完好">{{ currentAcceptance.packageIntact === 1 ? '是' : (currentAcceptance.packageIntact === 0 ? '否' : '-') }}</el-descriptions-item>
          <el-descriptions-item label="标签完整">{{ currentAcceptance.labelComplete === 1 ? '是' : (currentAcceptance.labelComplete === 0 ? '否' : '-') }}</el-descriptions-item>
          <el-descriptions-item label="有无破损">{{ currentAcceptance.hasDamage === 1 ? '有' : (currentAcceptance.hasDamage === 0 ? '无' : '-') }}</el-descriptions-item>
          <el-descriptions-item label="验收日期">{{ formatDate(currentAcceptance.acceptanceDate) }}</el-descriptions-item>
          <el-descriptions-item label="验收人">{{ currentAcceptance.acceptanceUserName }}</el-descriptions-item>
          <el-descriptions-item label="验收备注" :span="2">{{ currentAcceptance.acceptanceRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAcceptanceList, getAcceptance, startAcceptance, submitAcceptance, exportAcceptance } from '@/api/purchaseAcceptance'
import { getAllLocations } from '@/api/location'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
const activeTab = ref('pending')
const allAcceptances = ref([])
const pendingList = ref([])
const historyList = ref([])
const pendingTotal = ref(0)
const historyTotal = ref(0)
const acceptDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentAcceptance = ref(null)
const acceptFormRef = ref()
const locationList = ref([])

// 待验收查询参数
const pendingQueryParams = reactive({
  current: 1, size: 10, purchaseNo: '', materialName: ''
})

// 验收记录查询参数
const historyQueryParams = reactive({
  current: 1, size: 10, purchaseNo: '', materialName: '', result: null
})

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '')
const todayDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
})


const acceptForm = reactive({
  packageIntact: null,
  labelComplete: null,
  hasDamage: null,
  actualQuantity: null,
  expiryDate: null,
  locationId: null,
  result: null,
  remark: ''
})

const acceptRules = {
  packageIntact: [{ required: true, message: '请选择外包装是否完好', trigger: 'change' }],
  labelComplete: [{ required: true, message: '请选择标签是否完整', trigger: 'change' }],
  hasDamage: [{ required: true, message: '请选择有无破损', trigger: 'change' }],
  result: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择有效期', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择存放位置', trigger: 'change' }]
}

const fetchData = async () => {
  // 根据当前标签页加载对应数据
  if (activeTab.value === 'pending') {
    await fetchPendingAcceptances()
  } else {
    await fetchHistoryAcceptances()
  }
}

const fetchPendingAcceptances = async () => {
  loading.value = true
  try {
    const res = await getAcceptanceList({
      ...pendingQueryParams,
      result: 0
    })
    pendingList.value = res.data?.records || []
    pendingTotal.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchHistoryAcceptances = async () => {
  loading.value = true
  try {
    const res = await getAcceptanceList({
      ...historyQueryParams,
      result: historyQueryParams.result || undefined
    })
    historyList.value = res.data?.records || []
    historyTotal.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handlePendingSearch = () => {
  pendingQueryParams.current = 1
  fetchPendingAcceptances()
}

const handlePendingReset = () => {
  Object.assign(pendingQueryParams, {
    current: 1, size: 10, purchaseNo: '', materialName: ''
  })
  fetchPendingAcceptances()
}

const handlePendingPageChange = () => {
  fetchPendingAcceptances()
}

const handleHistorySearch = () => {
  historyQueryParams.current = 1
  fetchHistoryAcceptances()
}

const handleHistoryReset = () => {
  Object.assign(historyQueryParams, {
    current: 1, size: 10, purchaseNo: '', materialName: '', result: null
  })
  fetchHistoryAcceptances()
}

const handleHistoryPageChange = () => {
  fetchHistoryAcceptances()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const blob = await exportAcceptance({
      purchaseNo: historyQueryParams.purchaseNo,
      materialName: historyQueryParams.materialName,
      result: historyQueryParams.result
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `采购验收记录_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const fetchLocations = async () => {
  try {
    const res = await getAllLocations()
    locationList.value = res.data || []
  } catch (e) {
    console.error('获取位置列表失败:', e)
  }
}

const handleStart = async (row) => {
  try {
    await ElMessageBox.confirm('确定开始验收？')
    await startAcceptance(row.id)
    ElMessage.success('已开始验收')

    // 获取验收详情并显示弹窗
    const res = await getAcceptance(row.id)
    currentAcceptance.value = res.data
    acceptDialogVisible.value = true
  } catch (error) {
    if (error !== 'cancel') {
      console.error('开始验收失败:', error)
    }
  }
}

const handleSubmitAcceptance = async () => {
  await acceptFormRef.value.validate()
  await submitAcceptance(currentAcceptance.value.id, acceptForm)

  if (acceptForm.result === 1) {
    ElMessage.success('验收通过，已自动生成入库申请')
  } else {
    ElMessage.success('验收已拒绝')
  }

  acceptDialogVisible.value = false
  Object.assign(acceptForm, {
    packageIntact: null,
    labelComplete: null,
    hasDamage: null,
    actualQuantity: null,
    expiryDate: null,
    locationId: null,
    result: null,
    remark: ''
  })
  fetchData()
}

const handleView = async (row) => {
  const res = await getAcceptance(row.id)
  currentAcceptance.value = res.data
  viewDialogVisible.value = true
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

const formatDate = (date) => {
  if (!date) return '-'
  return date.substring(0, 16).replace('T', ' ')
}

// 监听tab切换，重新加载数据
watch(activeTab, () => {
  fetchData()
})

onMounted(() => {
  fetchData()
  fetchLocations()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 16px;
}

.acceptance-form {
  padding: 0 20px;
}

.info-section {
  margin-bottom: 20px;
}

.el-divider {
  margin: 20px 0;
}
</style>
