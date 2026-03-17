<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="(card, index) in statCards" :key="index">
        <el-card shadow="hover" class="stat-card" @click="handleCardClick(card.route)">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项和预警 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>待办事项</span>
              <el-badge :value="todoList.length" type="warning" />
            </div>
          </template>
          <el-timeline v-if="todoList.length > 0">
            <el-timeline-item v-for="item in todoList" :key="item.id"
              :type="item.type" :timestamp="item.time" placement="top">
              <div style="cursor: pointer" @click="handleTodoClick(item)">
                {{ item.title }}
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>预警信息</span>
              <el-badge :value="alertList.length" type="danger" />
            </div>
          </template>
          <div v-if="alertList.length > 0">
            <div v-for="alert in alertList" :key="alert.id" class="alert-item" @click="goToAlert">
              <el-tag :type="levelType(alert.level)" size="small">{{ levelText(alert.level) }}</el-tag>
              <span class="alert-content">{{ alert.content }}</span>
              <span class="alert-time">{{ formatTime(alert.createTime) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无预警" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>库存分类统计</span></template>
          <div ref="categoryChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>有效期分布</span></template>
          <div ref="expiryChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card style="margin-top: 20px">
      <template #header><span>快捷入口</span></template>
      <el-row :gutter="20">
        <el-col :span="4" v-for="(entry, index) in quickEntries" :key="index">
          <div class="quick-entry" @click="router.push(entry.route)">
            <el-icon :size="32" :style="{ color: entry.color }"><component :is="entry.icon" /></el-icon>
            <span>{{ entry.name }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardSummary, getCategoryStats, getExpiryStats } from '@/api/report'
import { getAlertList, getAlertStats } from '@/api/alert'

const router = useRouter()
const categoryChartRef = ref(null)
const expiryChartRef = ref(null)

const summary = ref({ totalStock: 0, totalMaterials: 0, monthIn: 0, monthOut: 0 })
const alertStats = ref({ total: 0, expiry: 0, stockLow: 0, unused: 0 })
const alertList = ref([])
const todoList = ref([])

const statCards = ref([
  { label: '库存总数', value: 0, icon: 'Box', color: '#409EFF', route: '/stock' },
  { label: '标准物质种类', value: 0, icon: 'Collection', color: '#67C23A', route: '/basic/material' },
  { label: '本月入库', value: 0, icon: 'Download', color: '#E6A23C', route: '/stock-in' },
  { label: '本月出库', value: 0, icon: 'Upload', color: '#F56C6C', route: '/stock-out' }
])

const quickEntries = [
  { name: '入库登记', icon: 'Download', color: '#409EFF', route: '/stock-in' },
  { name: '出库申请', icon: 'Upload', color: '#67C23A', route: '/stock-out' },
  { name: '库存查询', icon: 'Search', color: '#E6A23C', route: '/stock' },
  { name: '采购申请', icon: 'ShoppingCart', color: '#909399', route: '/purchase' },
  { name: '盘点任务', icon: 'DocumentChecked', color: '#F56C6C', route: '/stock-check' },
  { name: '预警中心', icon: 'Bell', color: '#FF6B6B', route: '/alert' }
]

const fetchData = async () => {
  try {
    const res = await getDashboardSummary()
    summary.value = res.data || {}

    statCards.value[0].value = summary.value.totalStock || 0
    statCards.value[1].value = summary.value.totalMaterials || 0
    statCards.value[2].value = summary.value.monthIn || 0
    statCards.value[3].value = summary.value.monthOut || 0
  } catch (e) {}
}

const fetchAlerts = async () => {
  try {
    const res = await getAlertList({ status: 0 })
    alertList.value = (res.data || []).slice(0, 5)
  } catch (e) {}
}

const fetchCategoryStats = async () => {
  try {
    const res = await getCategoryStats()
    renderCategoryChart(res.data || [])
  } catch (e) {}
}

const fetchExpiryStats = async () => {
  try {
    const res = await getExpiryStats()
    renderExpiryChart(res.data || [])
  } catch (e) {}
}

const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  // 简化版本，实际应使用 ECharts
  categoryChartRef.value.innerHTML = `
    <div style="height: 300px; display: flex; flex-direction: column; justify-content: center; padding: 20px">
      ${data.map(item => `
        <div style="display: flex; align-items: center; margin-bottom: 15px">
          <span style="width: 100px">${item.name}</span>
          <div style="flex: 1; background: #f0f0f0; height: 20px; border-radius: 10px; overflow: hidden">
            <div style="background: #409EFF; height: 100%; width: ${Math.min(item.count * 5, 100)}%"></div>
          </div>
          <span style="width: 60px; text-align: right">${item.count} 种</span>
        </div>
      `).join('')}
    </div>
  `
}

const renderExpiryChart = (data) => {
  if (!expiryChartRef.value) return
  const colors = { '正常': '#67C23A', '即将过期(1个月内)': '#E6A23C', '紧急(7天内)': '#F56C6C', '已过期': '#909399' }
  const total = data.reduce((sum, item) => sum + item.value, 0)

  expiryChartRef.value.innerHTML = `
    <div style="height: 300px; display: flex; align-items: center; justify-content: center">
      <div style="display: flex; flex-wrap: wrap; gap: 20px; justify-content: center">
        ${data.map(item => `
          <div style="text-align: center">
            <div style="font-size: 36px; font-weight: bold; color: ${colors[item.name] || '#409EFF'}">${item.value}</div>
            <div style="font-size: 14px; color: #999">${item.name}</div>
          </div>
        `).join('')}
      </div>
    </div>
  `
}

const handleCardClick = (route) => router.push(route)
const handleTodoClick = (item) => item.route && router.push(item.route)
const goToAlert = () => router.push('/alert')

const levelType = (l) => ({ 1: 'info', 2: 'warning', 3: 'danger' }[l] || 'info')
const levelText = (l) => ({ 1: '普通', 2: '重要', 3: '紧急' }[l] || '普通')
const formatTime = (t) => t ? t.substring(0, 10) : ''

onMounted(() => {
  fetchData()
  fetchAlerts()
  nextTick(() => {
    fetchCategoryStats()
    fetchExpiryStats()
  })
})
</script>

<style scoped>
.dashboard { padding: 20px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-3px); }
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
.alert-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.alert-item:hover { background: #f5f5f5; }
.alert-content { flex: 1; margin-left: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alert-time { color: #999; font-size: 12px; }
.quick-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.quick-entry:hover { background: #f5f5f5; }
.quick-entry span { margin-top: 10px; color: #666; }
</style>
