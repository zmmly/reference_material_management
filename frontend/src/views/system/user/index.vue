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
          <el-select v-model="queryParams.status" placeholder="状态" clearable class="search-select">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe class="data-table">
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="roleName" label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag v-if="row.roleName" class="role-tag">{{ row.roleName }}</el-tag>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button type="warning" size="small" @click="handleResetPwd(row)">
                <el-icon><Key /></el-icon>
                重置密码
              </el-button>
              <el-button
                :type="row.status === 1 ? 'danger' : 'success'"
                size="small"
                @click="handleStatus(row)"
              >
                <el-icon>
                  <CircleClose v-if="row.status === 1" />
                  <CircleCheck v-else />
                </el-icon>
                {{ row.status === 1 ? '禁用' : '启用' }}
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
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '新增用户'" width="500" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editId" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Key, CircleClose, CircleCheck } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, updateUserStatus, resetPassword } from '@/api/user'
import { getRoleList } from '@/api/role'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const roleList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref()

const queryParams = reactive({ current: 1, size: 10, username: '', status: null })
const form = reactive({ username: '', realName: '', phone: '', email: '', roleId: null })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const res = await getRoleList()
    roleList.value = res.data || []
  } catch (e) {}
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { username: '', realName: '', phone: '', email: '', roleId: null })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (editId.value) {
    await updateUser(editId.value, form)
  } else {
    await createUser(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchData()
}

const handleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确定${newStatus === 1 ? '启用' : '禁用'}该用户？`, '提示', {
    type: 'warning'
  })
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  fetchData()
}

const handleResetPwd = async (row) => {
  await ElMessageBox.confirm('确定重置该用户密码为123456？', '提示', {
    type: 'warning'
  })
  await resetPassword(row.id)
  ElMessage.success('密码已重置为123456')
}

onMounted(() => {
  fetchData()
  fetchRoles()
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
  width: 140px;
}

.search-actions {
  display: flex;
  gap: 10px;
}

/* 表格样式 */
.data-table {
  margin-bottom: 16px;
}

/* 角色标签样式 - 蓝色主题 */
.role-tag {
  background-color: #ecf5ff !important;
  color: #409eff !important;
  border-color: #d9ecff !important;
  font-weight: 500;
  padding: 0 10px;
  height: 24px;
  line-height: 22px;
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 500;
}

.action-buttons .el-button .el-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }

  .action-buttons .el-button {
    width: 100%;
    justify-content: center;
  }
}
</style>
