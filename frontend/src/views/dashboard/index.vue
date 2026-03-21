<template>
  <div class="dashboard animate-stagger">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="(card, index) in statCards" :key="index">
        <div class="stat-card-dark" :class="card.cardClass" :style="{ '--card-accent': card.borderColor }" @click="handleCardClick(card.route)">
          <div class="stat-card-content">
            <div class="stat-icon-dark" :style="{ '--icon-bg': card.iconBg }">
              <el-icon><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card class="glass-card quick-entry-card content-row">
      <template #header>
        <span class="card-title">⚡ 快捷入口</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="4" v-for="(entry, index) in quickEntries" :key="index">
          <div class="quick-entry" @click="router.push(entry.route)">
            <div class="quick-entry-icon" :style="{ '--entry-gradient': entry.gradient }">
              <el-icon :size="24"><component :is="entry.icon" /></el-icon>
            </div>
            <span class="quick-entry-name">{{ entry.name }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 待办事项和预警 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">📋 待办事项</span>
              <el-badge :value="totalTodoCount" class="header-badge" />
            </div>
          </template>
          <el-timeline v-if="todoList.length > 0">
            <el-timeline-item v-for="item in todoList" :key="item.id"
              :type="item.type" :timestamp="item.time" placement="top">
              <div class="todo-item" @click="item.route && router.push(item.route)">
                {{ item.title }}
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">⚠️ 预警信息</span>
              <el-badge :value="alertTotal" type="danger" class="header-badge" />
            </div>
          </template>
          <div v-if="alertList.length > 0" class="alert-list">
            <div v-for="alert in alertList" :key="alert.id" class="alert-item" :class="alertClass(alert)" @click="router.push('/alert')">
              <div class="alert-icon" :style="{ background: alertIconBg(alert.level) }">
                <el-icon><component :is="alertIcon(alert.level)" /></el-icon>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.content }}</div>
                <div class="alert-time">{{ formatTime(alert.createTime) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无预警" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header>
            <span class="card-title">📊 库存分类统计</span>
          </template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header>
            <span class="card-title">📅 有效期分布</span>
          </template>
          <div ref="expiryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardSummary, getCategoryStats, getExpiryStats, getDashboardTodoItems } from '@/api/report'
import { getAlertList } from '@/api/alert'
import * as echarts from 'echarts'

const router = useRouter()
const categoryChartRef = ref(null)
const expiryChartRef = ref(null)
let categoryChart = null
let expiryChart = null

const summary = ref({ totalStock: 0, totalMaterials: 0, monthIn: 0, monthOut: 0 })
const alertStats = ref({ total: 0, expiry: 0, stockLow: 0, unused: 0 })
const alertList = ref([])
const alertTotal = ref(0)
const todoList = ref([])
const todoData = ref({ pendingPurchaseCount: 0, approvedPurchaseCount: 0, alertCount: 0 })

const totalTodoCount = computed(() => {
  return (todoData.value.pendingPurchaseCount + todoData.value.approvedPurchaseCount + todoData.value.alertCount)
})

const statCards = ref([
  {
    label: '库存总数',
    value: 0,
    icon: 'Box',
    cardClass: 'primary',
    borderColor: '#667eea',
    iconBg: 'rgba(102, 126, 234, 0.1)',
    route: '/stock'
  },
  {
    label: '标准物质种类',
    value: 0,
    icon: 'Collection',
    cardClass: 'info',
    borderColor: '#4ecdc4',
    iconBg: 'rgba(78, 205, 196, 0.1)',
    route: '/basic/material'
  },
  {
    label: '本月入库',
    value: 0,
    icon: 'Download',
    cardClass: 'success',
    borderColor: '#51cf66',
    iconBg: 'rgba(81, 207, 102, 0.1)',
    route: '/stock-in'
  },
  {
    label: '本月出库',
    value: 0,
    icon: 'Upload',
    cardClass: 'warning',
    borderColor: '#ff9f43',
    iconBg: 'rgba(255, 159, 67, 0.1)',
    route: '/stock-out'
  }
])

const quickEntries = [
  { name: '入库登记', icon: 'Download', gradient: 'linear-gradient(135deg, #8b5cf6, #3b82f6)', route: '/stock-in' },
  { name: '出库申请', icon: 'Upload', gradient: 'linear-gradient(135deg, #06b6d4, #3b82f6)', route: '/stock-out' },
  { name: '库存查询', icon: 'Search', gradient: 'linear-gradient(135deg, #f59e0b, #ec4899)', route: '/stock' },
  { name: '采购申请', icon: 'ShoppingCart', gradient: 'linear-gradient(135deg, #10b981, #06b6d4)', route: '/purchase' },
  { name: '盘点任务', icon: 'DocumentChecked', gradient: 'linear-gradient(135deg, #ef4444, #f59e0b)', route: '/stock-check' },
  { name: '预警中心', icon: 'Bell', gradient: 'linear-gradient(135deg, #ec4899, #8b5cf6)', route: '/alert' }
]

const fetchData = async () => {
  try {
    const res = await getDashboardSummary()
    summary.value = res.data || {}

    statCards.value[0].value = summary.value.totalStock || 0
    statCards.value[1].value = summary.value.totalMaterials || 0
    statCards.value[2].value = summary.value.monthIn || 0
    statCards.value[3].value = summary.value.monthOut || 0
  } catch (e) {
    console.error('获取统计信息失败:', e)
  }
}

const fetchTodoItems = async () => {
  try {
    console.log('开始获取待办事项数据...')
    const res = await getDashboardTodoItems()
    console.log('待办事项API响应:', res)

    if (res.code === 200 && res.data) {
      todoData.value = res.data
      console.log('待办事项数据:', todoData.value)

      todoList.value = [
        {
          id: 1,
          title: `待审批采购申请：${todoData.value.pendingPurchaseCount}项`,
          type: 'warning',
          time: null,
          route: '/purchase'
        },
        {
          id: 2,
          title: `待确认到货：${todoData.value.approvedPurchaseCount}项`,
          type: 'primary',
          time: null,
          route: '/purchase'
        },
        {
          id: 3,
          title: `待处理预警：${todoData.value.alertCount}项`,
          type: 'danger',
          time: alertList.value.length > 0 ? alertList.value[0]?.createTime : null,
          route: '/alert'
        }
      ]
      console.log('待办事项列表:', todoList.value)
    }
  } catch (e) {
    console.error('获取待办事项失败:', e)
  }
}

const fetchAlerts = async () => {
  try {
    console.log('开始获取预警数据...')
    const res = await getAlertList({ status: 0 })
    console.log('预警数据响应:', res)
    console.log('响应码:', res?.code)
    console.log('响应数据:', res?.data)

    const alerts = res.data || []
    alertList.value = alerts.slice(0, 5)
    alertTotal.value = alerts.length
    console.log('预警总数:', alertTotal.value)
    console.log('预警列表长度:', alertList.value.length)
    console.log('预警列表:', alertList.value)
  } catch (e) {
    console.error('获取预警数据失败:', e)
  }
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

  if (categoryChart) {
    categoryChart.dispose()
  }

  categoryChart = echarts.init(categoryChartRef.value)

  const colors = ['#667eea', '#4ecdc4', '#51cf66', '#ff9f43', '#764ba2', '#ff6b6b', '#2196f3']

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 种 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#666',
        fontSize: 13
      }
    },
    series: [
      {
        name: '库存分类',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        labelLine: {
          show: false
        },
        data: data.map((item, i) => ({
          value: item.count,
          name: item.name,
          itemStyle: { color: colors[i % colors.length] }
        }))
      }
    ]
  }

  categoryChart.setOption(option)
}

const renderExpiryChart = (data) => {
  if (!expiryChartRef.value) return

  if (expiryChart) {
    expiryChart.dispose()
  }

  expiryChart = echarts.init(expiryChartRef.value)

  const colorMap = {
    '正常': '#51cf66',
    '即将过期(1个月内)': '#ff9f43',
    '紧急(7天内)': '#ff6b6b',
    '已过期': '#999999'
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 件 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#666',
        fontSize: 13
      }
    },
    series: [
      {
        name: '有效期分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        labelLine: {
          show: false
        },
        data: data.map(item => ({
          value: item.value,
          name: item.name,
          itemStyle: { color: colorMap[item.name] || '#8b5cf6' }
        }))
      }
    ]
  }

  expiryChart.setOption(option)
}

const handleResize = () => {
  categoryChart?.resize()
  expiryChart?.resize()
}

const handleCardClick = (route) => router.push(route)
const handleTodoClick = (item) => item.route && router.push(item.route)
const goToAlert = () => router.push('/alert')

const levelType = (l) => ({ 1: 'info', 2: 'warning', 3: 'danger' }[l] || 'info')
const levelText = (l) => ({ 1: '普通', 2: '重要', 3: '紧急' }[l] || '普通')
const formatTime = (t) => t ? t.substring(0, 10) : ''

// 预警相关函数
const alertClass = (alert) => {
  if (alert.level === 3) return 'alert-overdue'
  if (alert.level === 2) return 'alert-expiring'
  return 'alert-low-stock'
}

const alertIcon = (level) => {
  if (level === 3) return 'Clock'
  if (level === 2) return 'WarningFilled'
  return 'Bell'
}

const alertIconBg = (level) => {
  if (level === 3) return 'rgba(255, 107, 107, 0.2)'
  if (level === 2) return 'rgba(255, 159, 67, 0.2)'
  return 'rgba(33, 150, 243, 0.2)'
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  nextTick(async () => {
    await fetchData()
    await fetchAlerts()
    await fetchTodoItems()
    await fetchCategoryStats()
    await fetchExpiryStats()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  categoryChart?.dispose()
  expiryChart?.dispose()
})
</script>

<style lang="scss" scoped>
@use '@/styles/design-system' as *;

.dashboard {
  padding: 0;
  background: #f5f7fa;
  min-height: 100vh;
}

.stat-row {
  margin-bottom: var(--spacing-xl);
}

.content-row {
  margin-bottom: var(--spacing-xl);
}

.stat-card-dark {
  position: relative;
  padding: var(--spacing-xl);
  background: #ffffff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  overflow: hidden;
  transition: transform var(--transition-slow), box-shadow var(--transition-slow);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: var(--card-accent);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  &:active {
    transform: translateY(-1px);
    box-shadow: var(--shadow-sm);
  }
}

.stat-card-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: #1a1a2e;
  line-height: var(--line-height-tight);
  letter-spacing: -0.5px;
}

.stat-label {
  color: #6b7280;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  margin-top: var(--spacing-sm);
  letter-spacing: 0.25px;
}

.stat-icon-dark {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--icon-bg);
  color: var(--card-accent);
  font-size: 24px;
  transition: all var(--transition-slow);
}

.content-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: #1a1a2e;
  letter-spacing: 0.5px;
}

.header-badge {
  :deep(.el-badge__content) {
    background: #ff6b6b;
    border-color: #ff6b6b;
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-semibold);
  }
}

.todo-item {
  cursor: pointer;
  color: var(--text-primary);
  transition: var(--transition-fast);

  &:hover {
    color: var(--accent-purple);
  }
}

.alert-list {
  max-height: 320px;
  overflow-y: auto;

  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;

    &:hover {
      background: #94a3b8;
    }
  }
}

.alert-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  background: #ffffff;
  border-left: 3px solid;

  &:last-child {
    margin-bottom: 0;
  }

  &:hover {
    transform: translateX(4px);
    box-shadow: var(--shadow-sm);
  }

  &.alert-expiring {
    background: linear-gradient(135deg, #fff7ed 0%, #fff3e0 100%);
    border-left-color: #ff9f43;
  }

  &.alert-low-stock {
    background: linear-gradient(135deg, #eff6ff 0%, #e3f2fd 100%);
    border-left-color: #2196f3;
  }

  &.alert-overdue {
    background: linear-gradient(135deg, #fef2f2 0%, #ffebee 100%);
    border-left-color: #ff6b6b;
  }
}

.alert-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-weight: var(--font-weight-semibold);
  color: #1a1a2e;
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-xs);
  line-height: var(--line-height-tight);
}

.alert-time {
  color: #6b7280;
  font-size: var(--font-size-xs);
}

.chart-container {
  height: 280px;
  position: relative;
}

.quick-entry-card {
  :deep(.el-card__body) {
    padding: 24px;
  }
}

.quick-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  cursor: pointer;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  background: #ffffff;

  &:hover {
    background: rgba(102, 126, 234, 0.05);
    transform: translateY(-3px);
    box-shadow: var(--shadow-md);

    .quick-entry-icon {
      transform: scale(1.1);
    }
  }

  &:active {
    transform: translateY(-1px);
  }
}

.quick-entry-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin-bottom: var(--spacing-md);
  transition: all var(--transition-slow);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
}

.quick-entry-name {
  font-size: var(--font-size-sm);
  color: #374151;
  text-align: center;
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.25px;
}
</style>
