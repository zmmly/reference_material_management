<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span style="font-size: 16px; font-weight: 600">预警规则配置</span>
      </template>

      <el-table :data="configs" v-loading="loading" border>
        <el-table-column prop="name" label="预警名称" min-width="160" />
        <el-table-column label="阈值" min-width="200">
          <template #default="{ row }">
            <template v-if="editingRow === row.type">
              <el-input-number v-model="editForm.threshold" :min="1" :max="365"
                :suffix="thresholdUnit(row.type)" size="small" style="width: 180px" />
            </template>
            <span v-else>{{ row.threshold }} {{ thresholdUnit(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <template v-if="editingRow === row.type">
              <el-switch v-model="editForm.enabled" :active-value="1" :inactive-value="0" />
            </template>
            <el-tag v-else :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="260">
          <template #default="{ row }">
            {{ description(row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="editingRow === row.type">
              <el-button link type="primary" size="small" @click="handleSave">保存</el-button>
              <el-button link size="small" @click="handleCancel">取消</el-button>
            </template>
            <el-button v-else link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertConfigs, updateAlertConfig } from '@/api/alert'

const loading = ref(false)
const configs = ref([])
const editingRow = ref(null)
const editForm = ref({ threshold: 0, enabled: 1 })

const thresholdUnit = (type) => {
  if (type === 'UNUSED_MONTHS') return '个月'
  if (type === 'STOCK_LOW') return '件'
  return '天'
}

const description = (type) => {
  const map = {
    EXPIRY_WARNING: '标准物质有效期不足该天数时，生成有效期预警',
    EXPIRY_CRITICAL: '标准物质有效期不足该天数时，生成紧急预警',
    STOCK_LOW: '标准物质在库数量不超过该值时，生成库存不足预警',
    UNUSED_MONTHS: '标准物质超过该月数未出库时，生成久未使用预警'
  }
  return map[type] || ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAlertConfigs()
    configs.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  editingRow.value = row.type
  editForm.value = { threshold: row.threshold, enabled: row.enabled }
}

const handleCancel = () => {
  editingRow.value = null
}

const handleSave = async () => {
  try {
    await updateAlertConfig(editingRow.value, editForm.value.threshold, editForm.value.enabled)
    ElMessage.success('保存成功')
    editingRow.value = null
    fetchData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container { padding: 20px; }
</style>
