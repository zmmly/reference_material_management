<template>
  <div class="page-container">
    <el-card>
      <!-- 操作栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-alert
            title="数据库备份功能说明"
            description="创建系统数据库的完整备份，建议定期备份以保障数据安全。备份文件将保存在服务器上，可以随时下载。"
            type="info"
            :closable="false"
            show-icon
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" :loading="backuping" @click="handleBackup">
            <el-icon><Download /></el-icon>
            创建备份
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe class="data-table">
        <el-table-column prop="filename" label="备份文件名" min-width="220" />
        <el-table-column prop="fileSize" label="文件大小" min-width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="backupTime" label="备份时间" min-width="150" />
        <el-table-column prop="operatorName" label="操作人" min-width="100" />
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleDownload(row)">
                <el-icon><Download /></el-icon>
                下载
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Delete } from '@element-plus/icons-vue'
import { getBackupList, createBackup, downloadBackup, deleteBackup } from '@/api/backup'

const loading = ref(false)
const backuping = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({ current: 1, size: 10 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getBackupList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleBackup = async () => {
  backuping.value = true
  try {
    await createBackup()
    ElMessage.success('备份创建成功')
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    backuping.value = false
  }
}

const handleDownload = async (row) => {
  try {
    const response = await downloadBackup(row.id)
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', row.filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该备份文件？删除后无法恢复', '提示', {
    type: 'warning'
  })
  await deleteBackup(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  margin-left: 20px;
}

.data-table {
  margin-bottom: 16px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
