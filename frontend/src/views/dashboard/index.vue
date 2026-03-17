<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>有效期预警</span>
          </template>
          <div class="stat-content">
            <span class="stat-number">0</span>
            <span class="stat-label">种标准物质即将过期</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>库存不足</span>
          </template>
          <div class="stat-content">
            <span class="stat-number">0</span>
            <span class="stat-label">种标准物质库存不足</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>待办事项</span>
          </template>
          <div class="stat-content">
            <span class="stat-number">0</span>
            <span class="stat-label">项待处理</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>快捷入口</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" size="large" style="width: 100%">入库登记</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" size="large" style="width: 100%">出库申请</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" size="large" style="width: 100%">库存查询</el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" size="large" style="width: 100%">盘点任务</el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard { padding: 20px; }
.stat-content { text-align: center; padding: 20px 0; }
.stat-number { font-size: 36px; font-weight: bold; color: #409EFF; display: block; }
.stat-label { color: #999; }
</style>
