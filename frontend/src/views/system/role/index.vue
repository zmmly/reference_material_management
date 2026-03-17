<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

const loading = ref(false)
const tableData = ref([])
const queryParams = reactive({ current: 1, size: 10, name: '' })

const fetchData = async () => {
  loading.value = true
  // TODO: 调用API
  loading.value = false
}

onMounted(() => fetchData())
</script>

<style scoped>.page-container { padding: 20px; }</style>
