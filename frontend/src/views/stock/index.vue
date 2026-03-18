<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="queryParams.keyword" placeholder="名称/编码/批号" clearable />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-select v-model="queryParams.locationId" placeholder="全部" clearable>
            <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="即将过期" :value="2" />
            <el-option label="已过期" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button type="success" @click="handleIn">入库登记</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="internalCode" label="内部编码" width="120" />
        <el-table-column prop="materialName" label="标准物质名称" />
        <el-table-column prop="batchNo" label="批号" width="120" />
        <el-table-column prop="quantity" label="库存数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.quantity <= 0 }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-warning': isWarning(row.expiryDate), 'text-danger': isExpired(row.expiryDate) }">
              {{ row.expiryDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="存放位置" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" size="small" @click="handleOut(row)">出库</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStockList } from '@/api/stock'
import { getAllLocations } from '@/api/location'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const locationList = ref([])

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

const handleIn = () => router.push('/stock-in')
const handleOut = (row) => router.push({ path: '/stock-out/apply', query: { stockId: row.id } })

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
</style>
