 <template>
  <div class="page-container">
    <!-- 盘点任务列表 -->
    <el-card class="task-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>盘点任务</span>
          <el-button type="primary" size="small" @click="handleCreate">新建盘点</el-button>
        </div>
      </template>
      <el-table
        :data="checkList"
        v-loading="loading"
        border
        size="small"
        highlight-current-row
        @current-change="handleSelectCheck"
      >
        <el-table-column prop="checkNo" label="盘点单号" width="150" />
        <el-table-column label="盘点范围" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ scopeText(row.scope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="范围详情" min-width="150">
          <template #default="{ row }">
            <span v-if="row.scope === 'ALL'">全部库存</span>
            <span v-else-if="row.scope === 'CATEGORY'">{{ getCategoryName(row.scopeValue) }}</span>
            <span v-else-if="row.scope === 'LOCATION'">{{ getLocationName(row.scopeValue) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="checkDate" label="盘点日期" width="120" />
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-progress
                :percentage="row.totalCount > 0 ? Math.round((row.checkedCount || 0) / row.totalCount * 100) : 0"
                :status="row.checkedCount >= row.totalCount ? 'success' : ''"
                :stroke-width="8"
                style="flex: 1; min-width: 80px;"
              />
              <span style="font-size: 12px; color: #666; white-space: nowrap">
                {{ row.checkedCount || 0 }}/{{ row.totalCount || 0 }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="remarks" label="备注" min-width="150">
          <template #default="{ row }">
            <span class="text-ellipsis">{{ row.remarks || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 盘点明细 -->
    <el-card v-if="selectedCheck" class="detail-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div>
            <span style="font-weight: 600; font-size: 16px">{{ selectedCheck.checkNo }}</span>
            <el-tag :type="statusType(selectedCheck.status)" style="margin-left: 10px">{{ statusText(selectedCheck.status) }}</el-tag>
            <span style="margin-left: 20px; color: #999">
              进度: {{ selectedCheck.checkedCount }}/{{ selectedCheck.totalCount }}
              <span v-if="selectedCheck.differenceCount > 0" style="color: #f56c6c"> (差异: {{ selectedCheck.differenceCount }})</span>
          </div>
          <el-button v-if="selectedCheck.status === 0" type="success" @click="handleComplete">完成盘点</el-button>
        </div>
      </template>
      <el-table :data="checkItems" v-loading="itemsLoading" border>
        <el-table-column prop="internalCode" label="内部编码" width="120" />
        <el-table-column prop="materialName" label="标准物质" />
        <el-table-column prop="batchNo" label="批号" width="100" />
        <el-table-column prop="locationName" label="位置" width="100" />
        <el-table-column prop="systemQuantity" label="系统数量" width="90">
          <template #default="{ row }">{{ row.systemQuantity }}{{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="actualQuantity" label="实盘数量" width="120">
          <template #default="{ row }">
            <span v-if="row.status > 0" :class="{ 'text-danger': row.difference !== 0 }">{{ row.actualQuantity }}</span>
            <el-input-number v-else v-model="row.inputQuantity" :min="0" size="small" style="width: 100px" />
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异" width="80">
          <template #default="{ row }">
            <span v-if="row.status > 0" :class="{ 'text-danger': row.difference > 0, 'text-warning': row.difference < 0 }">
              {{ row.difference > 0 ? '+' : '' }}{{ row.difference }}
            </span>
            <span v-else :class="{ 'text-danger': getDifference(row) > 0, 'text-warning': getDifference(row) < 0 }">
              {{ getDifference(row) > 0 ? '+' : '' }}{{ getDifference(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异说明" min-width="150">
          <template #default="{ row }">
            <span v-if="row.status > 0">{{ row.differenceReason || '-' }}</span>
            <template v-else>
              <el-input
                v-model="row.inputDifferenceReason"
                placeholder="请输入差异说明"
                :disabled="getDifference(row) === 0"
                size="small"
              />
            </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="primary" size="small" @click="handleCheckItem(row)">确认盘点</el-button>
            </template>
            <template v-if="row.status === 2">
              <el-button link type="warning" size="small" @click="handleAdjust(row)">调整库存</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card v-else>
      <el-empty description="请点击上方盘点任务查看详情" />
    </el-card>

    <!-- 新建盘点对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建盘点任务" width="500">
      <el-form ref="formRef" :model="createForm" :rules="rules" label-width="100px">
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="createForm.checkDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="盘点范围" prop="scope">
          <el-select v-model="createForm.scope" placeholder="请选择" style="width: 100%">
            <el-option label="全部库存" value="ALL" />
            <el-option label="按分类" value="CATEGORY" />
            <el-option label="按位置" value="LOCATION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.scope === 'CATEGORY'" label="选择分类" prop="scopeValue">
          <el-select v-model="createForm.scopeValue" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in categoryList" :key="item.id" :label="item.label" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.scope === 'LOCATION'" label="选择位置" prop="scopeValue">
          <el-select v-model="createForm.scopeValue" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in locationList" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remarks" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调整库存对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="调整库存" width="400">
      <el-form label-width="80px">
        <el-form-item label="调整原因">
          <el-input v-model="adjustReason" type="textarea" :rows="3" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">确定调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStockCheckList, getStockCheckItems,
          createStockCheck,
          checkStockCheckItem
          completeStockCheck
          adjustStockCheckItem
        } from '@/api/stockCheck'
import { getCategoryTree } from '@/api/category'
import { getAllLocations } from '@/api/location'

import }
onMounted(() => {
  fetchCheckList()
    fetchCategories()
    fetchLocations()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-ellipsis {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-card {
  margin-bottom: 20px;
}
.detail-card {
  margin-top: 20px;
}
</style>
