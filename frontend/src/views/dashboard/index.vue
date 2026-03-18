<template>
  <div class="dashboard animate-stagger">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="(card, index) in statCards" :key="index">
        <div class="stat-card-dark" :style="{ '--card-accent': card.gradient }" @click="handleCardClick(card.route)">
          <div class="stat-card-content">
            <div class="stat-icon-dark" :style="{ '--icon-gradient': card.gradient }">
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
              <el-badge :value="todoList.length" class="header-badge" />
            </div>
          </template>
          <el-timeline v-if="todoList.length > 0">
            <el-timeline-item v-for="item in todoList" :key="item.id"
              :type="item.type" :timestamp="item.time" placement="top">
              <div class="todo-item" @click="handleTodoClick(item)">
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
              <el-badge :value="alertList.length" type="danger" class="header-badge" />
            </div>
          </template>
          <div v-if="alertList.length > 0" class="alert-list">
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
  { label: '库存总数', value: 0, icon: 'Box', gradient: 'linear-gradient(135deg, #8b5cf6, #3b82f6)', route: '/stock' },
  { label: '标准物质种类', value: 0, icon: 'Collection', gradient: 'linear-gradient(135deg, #06b6d4, #3b82f6)', route: '/basic/material' },
  { label: '本月入库', value: 0, icon: 'Download', gradient: 'linear-gradient(135deg, #f59e0b, #ec4899)', route: '/stock-in' },
  { label: '本月出库', value: 0, icon: 'Upload', gradient: 'linear-gradient(135deg, #10b981, #06b6d4)', route: '/stock-out' }
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
  const colors = ['#8b5cf6', '#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#ec4899', '#ef4444']
  categoryChartRef.value.innerHTML = `
    <div class="chart-bars">
      ${data.map((item, i) => `
        <div class="chart-bar-item">
          <span class="chart-bar-label">${item.name}</span>
          <div class="chart-bar-track">
            <div class="chart-bar-fill" style="width: ${Math.min(item.count * 5, 100)}%; background: ${colors[i % colors.length]}"></div>
          </div>
          <span class="chart-bar-value">${item.count} 种</span>
        </div>
      `).join('')}
    </div>
  `
}

const renderExpiryChart = (data) => {
  if (!expiryChartRef.value) return
  const colors = {
    '正常': 'linear-gradient(135deg, #10b981, #06b6d4)',
    '即将过期(1个月内)': 'linear-gradient(135deg, #f59e0b, #ec4899)',
    '紧急(7天内)': 'linear-gradient(135deg, #ef4444, #f59e0b)',
    '已过期': 'linear-gradient(135deg, #64748b, #94a3b8)'
  }

  expiryChartRef.value.innerHTML = `
    <div class="expiry-stats">
      ${data.map(item => `
        <div class="expiry-stat-item">
          <div class="expiry-stat-value" style="background: ${colors[item.name] || 'linear-gradient(135deg, #8b5cf6, #3b82f6)'}">${item.value}</div>
          <div class="expiry-stat-label">${item.name}</div>
        </div>
      `).join('')}
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

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card-dark {
  position: relative;
  padding: 24px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  overflow: hidden;
  transition: var(--transition-normal);

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
    transform: translateY(-4px);
    box-shadow: 0 20px 40px rgba(139, 92, 246, 0.15);
    border-color: rgba(139, 92, 246, 0.3);
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
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  color: var(--text-muted);
  font-size: 14px;
  margin-top: 4px;
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
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-badge {
  :deep(.el-badge__content) {
    background: var(--accent-purple);
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
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition-fast);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: rgba(139, 92, 246, 0.05);
    margin: 0 -20px;
    padding: 12px 20px;
    border-radius: var(--radius-sm);
  }
}

.alert-content {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.alert-time {
  color: var(--text-muted);
  font-size: 12px;
}

.chart-container {
  height: 300px;
}

.chart-bars {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 16px;
  gap: 16px;
}

.chart-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart-bar-label {
  width: 100px;
  font-size: 14px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-bar-track {
  flex: 1;
  height: 8px;
  background: rgba(139, 92, 246, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.chart-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}

.chart-bar-value {
  width: 60px;
  text-align: right;
  font-size: 14px;
  color: var(--text-muted);
}

.expiry-stats {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
  padding: 20px;
}

.expiry-stat-item {
  text-align: center;
}

.expiry-stat-value {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 auto 12px;
}

.expiry-stat-label {
  font-size: 14px;
  color: var(--text-muted);
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
  border-radius: var(--radius-md);
  transition: var(--transition-normal);

  &:hover {
    background: rgba(139, 92, 246, 0.08);
    transform: translateY(-2px);

    .quick-entry-icon {
      transform: scale(1.1);
    }
  }
}

.quick-entry-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--entry-gradient);
  margin-bottom: 12px;
  transition: var(--transition-normal);
  background-size: 200% 200%;
  animation: gradient-shift 3s ease infinite;
}

.quick-entry-name {
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
}

// 动画
@keyframes gradient-shift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
</style>
