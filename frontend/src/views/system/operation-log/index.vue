<template>
  <div class="page-container">
    <el-card>
      <!-- 搜索表单 -->
      <div class="search-bar">
        <div class="search-fields">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            class="search-input"
            @keyup.enter="fetchData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="queryParams.module" placeholder="请选择模块" clearable class="search-select">
            <el-option label="用户管理" value="user" />
            <el-option label="角色管理" value="role" />
            <el-option label="库存管理" value="stock" />
            <el-option label="标准物质" value="material" />
            <el-option label="分类管理" value="category" />
            <el-option label="位置管理" value="location" />
            <el-option label="供应商管理" value="supplier" />
            <el-option label="采购管理" value="purchase" />
            <el-option label="盘点管理" value="check" />
            <el-option label="预警管理" value="alert" />
            <el-option label="系统备份" value="backup" />
          </el-select>
          <el-select v-model="queryParams.action" placeholder="请选择操作类型" clearable class="search-select">
            <el-option label="新增" value="新增" />
            <el-option label="编辑" value="编辑" />
            <el-option label="删除" value="删除" />
            <el-option label="启用" value="启用" />
            <el-option label="禁用" value="禁用" />
            <el-option label="重置密码" value="重置密码" />
            <el-option label="入库" value="入库" />
            <el-option label="出库" value="出库" />
            <el-option label="审核" value="审核" />
            <el-option label="盘点" value="盘点" />
            <el-option label="备份" value="备份" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe class="data-table">
        <el-table-column prop="username" label="操作人" min-width="100" />
        <el-table-column prop="module" label="操作模块" min-width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ getModuleLabel(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="操作对象" min-width="150" show-overflow-tooltip />
        <el-table-column prop="detail" label="操作详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" min-width="120" />
        <el-table-column prop="createTime" label="操作时间" min-width="160" />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getOperationLogList } from '@/api/operationLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  current: 1,
  size: 10,
  username: '',
  module: '',
  action: ''
})

const moduleLabels = {
  user: '用户管理',
  role: '角色管理',
  stock: '库存管理',
  material: '标准物质',
  category: '分类管理',
  location: '位置管理',
  supplier: '供应商管理',
  purchase: '采购管理',
  check: '盘点管理',
  alert: '预警管理',
  backup: '系统备份'
}

const getModuleLabel = (module) => {
  return moduleLabels[module] || module
}

const getActionTagType = (action) => {
  const typeMap = {
    '新增': 'success',
    '编辑': 'primary',
    '删除': 'danger',
    '启用': 'success',
    '禁用': 'warning',
    '重置密码': 'warning',
    '入库': 'success',
    '出库': 'danger',
    '审核': 'primary',
    '盘点': 'info',
    '备份': 'info'
  }
  return typeMap[action] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOperationLogList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(queryParams, {
    current: 1,
    size: 10,
    username: '',
    module: '',
    action: ''
  })
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

/* 搜索栏样式 */
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.search-fields {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search-input {
  width: 220px;
}

.search-select {
  width: 160px;
}

.search-actions {
  display: flex;
  gap: 10px;
}

/* 表格样式 */
.data-table {
  margin-bottom: 16px;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
    gap: 12px;
  }

  .search-fields {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-input,
  .search-select {
    width: 100%;
  }

  .search-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
