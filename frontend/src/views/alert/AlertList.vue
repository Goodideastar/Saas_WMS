<template>
  <div class="alert-list">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>未处理预警总数</template>
          <div class="stat-value text-danger">{{ stats.unhandled || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日新增预警</template>
          <div class="stat-value text-warning">{{ stats.todayNew || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>已处理预警</template>
          <div class="stat-value text-success">{{ stats.handled || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="处理状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 110px">
            <el-option label="未处理" value="UNHANDLED" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="productCode" label="货品编码" />
        <el-table-column prop="productName" label="货品名称" />
        <el-table-column label="预警类型">
          <template #default="{ row }">
            <el-tag :type="row.alertType === 'BELOW_MIN' ? 'danger' : 'warning'">
              {{ row.alertType === 'BELOW_MIN' ? '低于下限' : '高于上限' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertValue" label="预警阈值" />
        <el-table-column prop="actualStock" label="当前库存" />
        <el-table-column label="处理状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UNHANDLED' ? 'danger' : 'success'">
              {{ row.status === 'UNHANDLED' ? '未处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'UNHANDLED'" v-permission="'alert:handle'" link type="primary" @click="handleAlert(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="handleDialogVisible" title="处理预警" width="400px">
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="处理意见">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="4" placeholder="请输入处理意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getAlertPage, handleAlert as handleAlertApi, getAlertStats } from '@/api/alert.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const handleDialogVisible = ref(false)
const stats = reactive({ unhandled: 0, todayNew: 0, handled: 0 })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const queryForm = reactive({ status: '' })
const handleForm = reactive({ id: null, handleRemark: '' })

let pollingTimer = null

async function loadData() {
  loading.value = true
  try {
    const res = await getAlertPage({ ...queryForm, ...pagination })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await getAlertStats()
    Object.assign(stats, res.data || {})
  } catch (error) {
    console.error('Failed to load alert stats', error)
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { Object.assign(queryForm, { status: '' }); handleSearch() }
function handleAlert(row) { handleForm.id = row.id; handleForm.handleRemark = ''; handleDialogVisible.value = true }

async function submitHandle() {
  await handleAlertApi({ id: handleForm.id, handleRemark: handleForm.handleRemark })
  ElMessage.success('处理成功')
  handleDialogVisible.value = false
  loadData()
  loadStats()
}

onMounted(() => {
  loadData()
  loadStats()
  pollingTimer = setInterval(() => {
    loadStats()
  }, 30000)
})

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer)
})
</script>

<style scoped>
.alert-list {
  max-width: 1400px;
}
.mb-20 { margin-bottom: var(--space-lg); }
.stat-value {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: 700;
  text-align: center;
  padding: 10px 0;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.text-danger { color: var(--color-danger); }
.text-warning { color: var(--color-warning); }
.text-success { color: var(--color-success); }
.search-form { margin-bottom: var(--space-md); }
.pagination-wrapper { margin-top: var(--space-md); display: flex; justify-content: flex-end; }
</style>
