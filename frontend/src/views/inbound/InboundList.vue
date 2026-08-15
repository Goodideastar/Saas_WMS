<template>
  <div class="inbound-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="queryForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-permission="'inbound:create'" type="primary" @click="handleCreate">新建入库单</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="supplier" label="供应商" />
        <el-table-column prop="inboundType" label="入库类型" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入库时间">
          <template #default="{ row }">{{ row.inboundTime ? row.inboundTime.replace('T', ' ') : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" v-permission="'inbound:audit'" link type="success" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 'PENDING'" v-permission="'inbound:cancel'" link type="danger" @click="handleCancel(row)">取消</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建入库单" width="800px">
      <el-form :model="orderForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库">
              <el-select v-model="orderForm.warehouseId" placeholder="请选择">
                <el-option label="主仓库" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库类型">
              <el-select v-model="orderForm.inboundType" placeholder="请选择">
                <el-option label="采购入库" value="PURCHASE" />
                <el-option label="退货入库" value="RETURN" />
                <el-option label="盘盈入库" value="INVENTORY" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="供应商">
          <el-input v-model="orderForm.supplier" />
        </el-form-item>
        <el-form-item label="关联订单号">
          <el-input v-model="orderForm.relatedOrderNo" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-divider>入库明细</el-divider>
        <el-table :data="orderForm.items" border>
          <el-table-column label="货品" width="200">
            <template #default="{ row }">
              <el-select v-model="row.productId" filterable placeholder="搜索货品">
                <el-option v-for="p in productList" :key="p.id" :label="p.productCode + ' - ' + p.productName" :value="p.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="应入数量" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.expectedQuantity" :min="1" controls-position="right" style="width: 130px" />
            </template>
          </el-table-column>
          <el-table-column label="实入数量" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.actualQuantity" :min="0" controls-position="right" style="width: 130px" />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" controls-position="right" style="width: 130px" />
            </template>
          </el-table-column>
          <el-table-column label="小计">
            <template #default="{ row }">
              {{ ((row.actualQuantity || 0) * (row.unitPrice || 0)).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button link type="danger" @click="orderForm.items.splice(scope.$index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button style="margin-top: 10px" @click="orderForm.items.push({ productId: null, expectedQuantity: 1, actualQuantity: 0, unitPrice: 0 })">
          添加明细
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="'入库单详情 - ' + (detailData.orderNo || '')" width="820px" :close-on-click-modal="false">
      <el-descriptions :column="2" border v-loading="detailLoading">
        <el-descriptions-item label="订单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">{{ detailData.inboundType }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailData.supplier || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ detailData.relatedOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detailData.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime ? detailData.createTime.replace('T', ' ') : '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库时间" :span="2">{{ detailData.inboundTime ? detailData.inboundTime.replace('T', ' ') : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">入库明细</el-divider>
      <el-table :data="detailData.items || []" border stripe size="small">
        <el-table-column prop="productCode" label="产品编码" width="120" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="expectedQuantity" label="应入数量" width="100" align="right" />
        <el-table-column prop="actualQuantity" label="实入数量" width="100" align="right" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right">
          <template #default="{ row }">¥{{ Number(row.unitPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="subtotal" label="小计" width="110" align="right">
          <template #default="{ row }">¥{{ Number(row.subtotal || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getInboundPage, createInbound, auditInbound, cancelInbound, getInboundDetail } from '@/api/inbound.js'
import { getProductPage } from '@/api/product.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref({})
const productList = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const queryForm = reactive({ orderNo: '', status: '', dateRange: null })
const orderForm = reactive({ warehouseId: null, supplier: '', inboundType: '', relatedOrderNo: '', remark: '', items: [] })

const statusMap = { PENDING: { text: '待审核', type: 'warning' }, COMPLETED: { text: '已完成', type: 'success' }, CANCELLED: { text: '已取消', type: 'info' } }
function getStatusType(s) { return statusMap[s]?.type || '' }
function getStatusText(s) { return statusMap[s]?.text || s }

async function loadProducts() {
  const res = await getProductPage({ pageNum: 1, pageSize: 200, status: 1 })
  productList.value = res.data.records || []
}

async function loadData() {
  loading.value = true
  try {
    const params = { ...queryForm, ...pagination }
    if (queryForm.dateRange) { params.startTime = queryForm.dateRange[0]; params.endTime = queryForm.dateRange[1] }
    const res = await getInboundPage(params)
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { Object.assign(queryForm, { orderNo: '', status: '', dateRange: null }); handleSearch() }
function handleCreate() { Object.assign(orderForm, { warehouseId: 1, supplier: '', inboundType: '', relatedOrderNo: '', remark: '', items: [{ productId: null, expectedQuantity: 1, actualQuantity: 0, unitPrice: 0 }] }); loadProducts(); dialogVisible.value = true }

async function handleSubmit() {
  await createInbound(orderForm)
  ElMessage.success('入库单创建成功')
  dialogVisible.value = false
  loadData()
}

async function handleAudit(row) {
  await ElMessageBox.confirm('确定要审核通过该入库单吗？', '提示', { type: 'warning' })
  await auditInbound(row.id)
  ElMessage.success('审核成功')
  loadData()
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确定要取消该入库单吗？', '提示', { type: 'warning' })
  await cancelInbound(row.id)
  ElMessage.success('取消成功')
  loadData()
}

function handleDetail(row) { loadDetail(row.id) }

async function loadDetail(id) {
  detailLoading.value = true
  try {
    const res = await getInboundDetail(id)
    detailData.value = res.data || {}
    detailVisible.value = true
  } finally { detailLoading.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.inbound-list {
  max-width: 1400px;
}
.search-form { margin-bottom: var(--space-md); }
.toolbar { margin-bottom: var(--space-md); }
.pagination-wrapper { margin-top: var(--space-md); display: flex; justify-content: flex-end; }
</style>
