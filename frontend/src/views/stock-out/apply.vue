<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>出库申请</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
        <el-form-item label="标准物质">
          <el-input :value="stockInfo.materialName" disabled />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="内部编码">
              <el-input :value="stockInfo.internalCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批号">
              <el-input :value="stockInfo.batchNo" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="有效期">
              <el-input :value="stockInfo.expiryDate" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放位置">
              <el-input :value="stockInfo.locationName" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider />
        <el-alert
          title="出库数量固定为1（单件物品）"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-form-item label="出库原因" prop="reason">
          <el-select v-model="form.reason" placeholder="请选择出库原因" style="width: 100%">
            <el-option label="实验使用" value="EXPERIMENT" />
            <el-option label="过期销毁" value="EXPIRED" />
            <el-option label="报废" value="SCRAP" />
            <el-option label="调拨出" value="TRANSFER_OUT" />
            <el-option label="赠送" value="DONATE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途说明" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请输入用途说明" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">提交申请</el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStock, applyStockOut } from '@/api/stock'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const formRef = ref()

const stockInfo = ref({
  materialName: '',
  internalCode: '',
  batchNo: '',
  expiryDate: '',
  locationName: ''
})

const form = reactive({
  stockId: null,
  reason: '',
  purpose: ''
})

const rules = {
  reason: [{ required: true, message: '请选择出库原因', trigger: 'change' }],
  purpose: [{ required: true, message: '请输入用途说明', trigger: 'blur' }]
}

const fetchStockInfo = async () => {
  const stockId = route.query.stockId
  if (!stockId) {
    ElMessage.error('缺少库存ID参数')
    router.push('/stock')
    return
  }

  loading.value = true
  try {
    const res = await getStock(stockId)
    stockInfo.value = res.data || {}
    form.stockId = parseInt(stockId)
  } catch (error) {
    ElMessage.error('获取库存信息失败')
    router.push('/stock')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await applyStockOut(form)
    ElMessage.success('申请提交成功')
    router.push('/stock-out')
  } catch (error) {
    ElMessage.error('申请提交失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/stock')
}

onMounted(() => fetchStockInfo())
</script>

<style scoped>
.page-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
