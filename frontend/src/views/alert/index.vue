<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="6" v-for="(stat, key) in stats" :key="key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: stat.color }">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="已忽略" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.type" placeholder="全部" clearable>
            <el-option label="有效期预警" value="EXPIRY_WARNING" />
            <el-option label="有效期紧急" value="EXPIRY_CRITICAL" />
            <el-option label="库存不足" value="STOCK_LOW" />
            <el-option label="长期未使用" value="UNUSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleTriggerCheck">手动检查</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="type" label="预警类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="标准物质" />
        <el-table-column prop="internalCode" label="内部编码" width="120" />
        <el-table-column prop="content" label="预警内容" />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" effect="dark" size="small">
              {{ levelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预警时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <template v-if="row.status === 0">
                <el-button link type="primary" size="small" @click="handleAlert(row)">处理</el-button>
                <el-button link type="info" size="small" @click="ignoreAlert(row)">忽略</el-button>
              </template>
              <span v-else style="color: #999">{{ row.handlerName }} {{ row.handleTime }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="handleDialogVisible" title="处理预警" width="400">
      <el-form label-width="80px">
        <el-form-item label="处理说明">
          <el-input v-model="handleRemark" type="textarea" :rows="3" placeholder="请输入处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertList, getAlertStats, handleAlert as handleAlertApi, ignoreAlert as ignoreAlertApi, triggerAlertCheck } from '@/api/alert'

const loading = ref(false)
const tableData = ref([])
const statsData = ref({ total: 0, expiry: 0, stockLow: 0, unused: 0 })
const queryParams = reactive({ status: null, type: '' })
const handleDialogVisible = ref(false)
const handleRemark = ref('')
const currentRow = ref(null)

const stats = computed(() => ({
  total: { label: '预警总数', value: statsData.value.total, icon: 'Bell', color: '#409EFF' },
  expiry: { label: '有效期预警', value: statsData.value.expiry, icon: 'Timer', color: '#E6A23C' },
  stockLow: { label: '库存不足', value: statsData.value.stockLow, icon: 'Box', color: '#F56C6C' },
  unused: { label: '长期未用', value: statsData.value.unused, icon: 'Clock', color: '#909399' }
}))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAlertList(queryParams)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getAlertStats()
    statsData.value = res.data || { total: 0, expiry: 0, stockLow: 0, unused: 0 }
  } catch (e) {}
}

const handleAlert = (row) => {
  currentRow.value = row
  handleRemark.value = ''
  handleDialogVisible.value = true
}

const confirmHandle = async () => {
  await handleAlertApi(currentRow.value.id, handleRemark.value)
  ElMessage.success('处理成功')
  handleDialogVisible.value = false
  fetchData()
  fetchStats()
}

const ignoreAlert = async (row) => {
  await ignoreAlertApi(row.id)
  ElMessage.success('已忽略')
  fetchData()
  fetchStats()
}

const handleTriggerCheck = async () => {
  await triggerAlertCheck()
  ElMessage.success('预警检查已触发')
  fetchData()
  fetchStats()
}

const typeTag = (t) => t?.includes('EXPIRY') ? 'warning' : t === 'STOCK_LOW' ? 'danger' : 'info'
const typeText = (t) => ({ EXPIRY_WARNING: '有效期预警', EXPIRY_CRITICAL: '有效期紧急', STOCK_LOW: '库存不足', UNUSED: '长期未使用' }[t] || t)
const levelType = (l) => ({ 1: 'info', 2: 'warning', 3: 'danger' }[l] || 'info')
const levelText = (l) => ({ 1: '普通', 2: '重要', 3: '紧急' }[l] || '普通')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')
const statusText = (s) => ({ 0: '未处理', 1: '已处理', 2: '已忽略' }[s] || '未知')

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}
.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  margin-right: 15px;
}
.stat-info { flex: 1; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { color: #999; font-size: 14px; margin-top: 5px; }
</style>
