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
.dashboard {
  padding: 0;
  background: #f5f7fa;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card-dark {
  position: relative;
  padding: 20px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: var(--card-accent);
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  }
}

.stat-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  color: #999;
  font-size: 14px;
  margin-top: 8px;
}

.stat-icon-dark {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--icon-bg);
  color: var(--card-accent);
  font-size: 20px;
  transition: all 0.3s;
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
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.header-badge {
  :deep(.el-badge__content) {
    background: #ff6b6b;
    border-color: #ff6b6b;
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
  max-height: 280px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
  background: #ffffff;
  border-left: 4px solid;

  &:last-child {
    margin-bottom: 0;
  }

  &:hover {
    transform: translateX(5px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }

  &.alert-expiring {
    background: #fff3e0;
    border-left-color: #ff9f43;
  }

  &.alert-low-stock {
    background: #e3f2fd;
    border-left-color: #2196f3;
  }

  &.alert-overdue {
    background: #ffebee;
    border-left-color: #ff6b6b;
  }
}

.alert-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.alert-time {
  color: #666;
  font-size: 13px;
}

.chart-container {
  height: 300px;
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
  padding: 20px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
  background: transparent;

  &:hover {
    background: rgba(102, 126, 234, 0.05);
    transform: translateY(-2px);

    .quick-entry-icon {
      transform: scale(1.1);
    }
  }
}

.quick-entry-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin-bottom: 12px;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.quick-entry-name {
  font-size: 14px;
  color: #666;
  text-align: center;
  font-weight: 500;
}
</style>
