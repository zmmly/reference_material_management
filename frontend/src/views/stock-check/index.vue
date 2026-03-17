<template>
  <div class="page-container">
    <el-row :gutter="20">
      <!-- 盘点任务列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>盘点任务</span>
              <el-button type="primary" size="small" @click="handleCreate">新建盘点</el-button>
            </div>
          </template>
          <el-table :data="checkList" v-loading="loading" border size="small" @row-click="handleSelectCheck">
            <el-table-column prop="checkNo" label="盘点单号" width="120" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="checkDate" label="盘点日期" width="100" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 盘点明细 -->
      <el-col :span="16">
        <el-card v-if="selectedCheck">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>
                <span>{{ selectedCheck.checkNo }}</span>
                <el-tag :type="statusType(selectedCheck.status)" style="margin-left: 10px">{{ statusText(selectedCheck.status) }}</el-tag>
                <span style="margin-left: 20px; color: #999">
                  进度: {{ selectedCheck.checkedCount }}/{{ selectedCheck.totalCount }}
                  <span v-if="selectedCheck.differenceCount > 0" style="color: #f56c6c"> (差异: {{ selectedCheck.differenceCount }})</span>
                </span>
              </div>
              <el-button v-if="selectedCheck.status === 0" type="success" @click="handleComplete">完成盘点</el-button>
            </div>
          </template>
          <el-table :data="checkItems" v-loading="itemsLoading" border>
            <el-table-column prop="internalCode" label="内部编码" width="120" />
            <el-table-column prop="materialName" label="标准物质" />
            <el-table-column prop="batchNo" label="批号" width="100" />
            <el-table-column prop="locationName" label="位置" width="100" />
            <el-table-column prop="systemQuantity" label="系统数量" width="90">
              <template #default="{ row }">{{ row.systemQuantity }}{{ row.unit }}</template>
            </el-table-column>
            <el-table-column prop="actualQuantity" label="实盘数量" width="120">
              <template #default="{ row }">
                <span v-if="row.status > 0" :class="{ 'text-danger': row.difference !== 0 }">{{ row.actualQuantity }}</span>
                <el-input-number v-else v-model="row.inputQuantity" :min="0" size="small" style="width: 100px" />
              </template>
            </el-table-column>
            <el-table-column prop="difference" label="差异" width="80">
              <template #default="{ row }">
                <span v-if="row.status > 0" :class="{ 'text-danger': row.difference > 0, 'text-warning': row.difference < 0 }">
                  {{ row.difference > 0 ? '+' : '' }}{{ row.difference }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button link type="primary" size="small" @click="handleCheckItem(row)">确认盘点</el-button>
                </template>
                <template v-if="row.status === 2">
                  <el-button link type="warning" size="small" @click="handleAdjust(row)">调整库存</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-card v-else>
          <el-empty description="请选择盘点任务" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建盘点对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建盘点任务" width="500">
      <el-form ref="formRef" :model="createForm" :rules="rules" label-width="100px">
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="createForm.checkDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="盘点范围" prop="scope">
          <el-select v-model="createForm.scope" placeholder="请选择" style="width: 100%">
            <el-option label="全部库存" value="ALL" />
            <el-option label="按分类" value="CATEGORY" />
            <el-option label="按位置" value="LOCATION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.scope === 'CATEGORY'" label="选择分类" prop="scopeValue">
          <el-select v-model="createForm.scopeValue" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in categoryList" :key="item.id" :label="item.label" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.scope === 'LOCATION'" label="选择位置" prop="scopeValue">
          <el-select v-model="createForm.scopeValue" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整库存对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="调整库存" width="400">
      <el-form label-width="80px">
        <el-form-item label="调整原因">
          <el-input v-model="adjustReason" type="textarea" :rows="3" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">确定调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStockCheckList, getStockCheckItems, createStockCheck,
  checkStockCheckItem, completeStockCheck, adjustStockCheckItem
} from '@/api/stockCheck'
import { getCategoryTree } from '@/api/category'
import { getAllLocations } from '@/api/location'

const loading = ref(false)
const itemsLoading = ref(false)
const checkList = ref([])
const selectedCheck = ref(null)
const checkItems = ref([])
const createDialogVisible = ref(false)
const adjustDialogVisible = ref(false)
const adjustReason = ref('')
const currentItem = ref(null)
const categoryList = ref([])
const locationList = ref([])
const formRef = ref()

const createForm = reactive({
  checkDate: new Date(), scope: 'ALL', scopeValue: null, remarks: ''
})
const rules = {
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
  scope: [{ required: true, message: '请选择盘点范围', trigger: 'change' }]
}

const fetchCheckList = async () => {
  loading.value = true
  try {
    const res = await getStockCheckList({ current: 1, size: 50 })
    checkList.value = res.data?.records || []
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

const fetchLocations = async () => {
  try {
    const res = await getAllLocations()
    locationList.value = res.data || []
  } catch (e) {}
}

const flattenTree = (tree, result = []) => {
  tree.forEach(node => {
    result.push({ id: node.id, label: node.label })
    if (node.children?.length) flattenTree(node.children, result)
  })
  return result
}

const handleSelectCheck = async (row) => {
  selectedCheck.value = row
  itemsLoading.value = true
  try {
    const res = await getStockCheckItems(row.id)
    checkItems.value = (res.data || []).map(item => ({ ...item, inputQuantity: item.systemQuantity }))
  } finally {
    itemsLoading.value = false
  }
}

const handleCreate = () => {
  Object.assign(createForm, { checkDate: new Date(), scope: 'ALL', scopeValue: null, remarks: '' })
  createDialogVisible.value = true
}

const handleCreateSubmit = async () => {
  await formRef.value.validate()
  const data = {
    ...createForm,
    checkDate: createForm.checkDate.toISOString().split('T')[0]
  }
  const res = await createStockCheck(data)
  ElMessage.success('盘点任务创建成功')
  createDialogVisible.value = false
  fetchCheckList()
  if (res.data) handleSelectCheck(res.data)
}

const handleCheckItem = async (row) => {
  await checkStockCheckItem(selectedCheck.value.id, row.id, row.inputQuantity, '')
  ElMessage.success('盘点完成')
  handleSelectCheck(selectedCheck.value)
}

const handleComplete = async () => {
  await ElMessageBox.confirm('确定完成盘点？完成后将不能再修改')
  await completeStockCheck(selectedCheck.value.id)
  ElMessage.success('盘点已完成')
  fetchCheckList()
  selectedCheck.value.status = 1
}

const handleAdjust = (row) => {
  currentItem.value = row
  adjustReason.value = ''
  adjustDialogVisible.value = true
}

const confirmAdjust = async () => {
  if (!adjustReason.value.trim()) {
    ElMessage.warning('请输入调整原因')
    return
  }
  await adjustStockCheckItem(currentItem.value.id, adjustReason.value)
  ElMessage.success('库存已调整')
  adjustDialogVisible.value = false
  handleSelectCheck(selectedCheck.value)
}

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')
const statusText = (s) => ({ 0: '进行中', 1: '已完成', 2: '已作废' }[s] || '未知')

onMounted(() => {
  fetchCheckList()
  fetchCategories()
  fetchLocations()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
</style>
