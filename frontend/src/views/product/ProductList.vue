<template>
  <div class="product-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="货品编码">
          <el-input v-model="queryForm.productCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="货品名称">
          <el-input v-model="queryForm.productName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="queryForm.category" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button v-permission="'product:add'" type="primary" @click="handleAdd">新增货品</el-button>
        <el-button v-permission="'product:adjust'" @click="handleBatchAdjust">批量调整库存</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%" @selection-change="val => selectedRows.value = val">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="productCode" label="货品编码" />
        <el-table-column prop="productName" label="货品名称" />
        <el-table-column prop="specification" label="规格" />
        <el-table-column prop="category" label="类别" />
        <el-table-column prop="currentStock" label="当前库存" />
        <el-table-column label="预警范围">
          <template #default="{ row }">{{ row.alertMin }} ~ {{ row.alertMax }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-permission="'product:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'product:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-permission="'product:adjust'" link type="warning" @click="handleAdjustStock(row)">调整库存</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑货品' : '新增货品'" width="600px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="货品编码" prop="productCode">
          <el-input v-model="formData.productCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="货品名称" prop="productName">
          <el-input v-model="formData.productName" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="formData.specification" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="formData.unit" />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="formData.category" />
        </el-form-item>
        <el-form-item label="参考进价">
          <el-input-number v-model="formData.referenceCost" :precision="2" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="参考售价">
          <el-input-number v-model="formData.referencePrice" :precision="2" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="预警下限">
          <el-input-number v-model="formData.alertMin" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="预警上限">
          <el-input-number v-model="formData.alertMax" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockDialogVisible" title="调整库存" width="400px">
      <el-form :model="stockForm" label-width="80px">
        <el-form-item label="货品名称">
          <span>{{ stockForm.productName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ stockForm.currentStock }}</span>
        </el-form-item>
        <el-form-item label="调整数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="-99999" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjustStock">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量调整库存" width="700px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="调整类型">
          <el-radio-group v-model="batchForm.adjustType">
            <el-radio value="IN">入库（增加）</el-radio>
            <el-radio value="OUT">出库（减少）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">调整明细（{{ batchForm.items.length }} 项）</el-divider>
        <el-table :data="batchForm.items" border size="small">
          <el-table-column label="货品名称" width="160">
            <template #default="{ row }">{{ row.productName }}</template>
          </el-table-column>
          <el-table-column label="当前库存" width="90" align="right">
            <template #default="{ row }">{{ row.currentStock }}</template>
          </el-table-column>
          <el-table-column label="调整数量" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" controls-position="right" style="width: 130px" />
            </template>
          </el-table-column>
        </el-table>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProductPage, addProduct, updateProduct, deleteProduct, adjustStock } from '@/api/product.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const stockDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const queryForm = reactive({ productCode: '', productName: '', category: '', status: null })
const formData = reactive({ id: null, productCode: '', productName: '', specification: '', unit: '', category: '', referenceCost: 0, referencePrice: 0, currentStock: 0, alertMin: 0, alertMax: 99999, status: 1, remark: '' })
const stockForm = reactive({ productId: null, productName: '', currentStock: 0, quantity: 0, remark: '' })

const rules = {
  productCode: [{ required: true, message: '请输入货品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入货品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProductPage({ ...queryForm, ...pagination })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { Object.assign(queryForm, { productCode: '', productName: '', category: '', status: null }); handleSearch() }

function handleAdd() {
  isEdit.value = false
  Object.assign(formData, { id: null, productCode: '', productName: '', specification: '', unit: '', category: '', referenceCost: 0, referencePrice: 0, currentStock: 0, alertMin: 0, alertMax: 99999, status: 1, remark: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) await updateProduct(formData)
    else await addProduct(formData)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } catch (error) {
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定要删除该货品吗？', '提示', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function handleAdjustStock(row) {
  stockForm.productId = row.id
  stockForm.productName = row.productName
  stockForm.currentStock = row.currentStock
  stockForm.quantity = 0
  stockForm.remark = ''
  stockDialogVisible.value = true
}

async function submitAdjustStock() {
  const adjustType = stockForm.quantity >= 0 ? 'IN' : 'OUT'
  await adjustStock({ productId: stockForm.productId, quantity: Math.abs(stockForm.quantity), adjustType: adjustType, remark: stockForm.remark })
  ElMessage.success('库存调整成功')
  stockDialogVisible.value = false
  loadData()
}

function handleBatchAdjust() {
  const selected = tableData.value.filter(r => selectedRows.value.includes(r))
  if (selected.length === 0) { ElMessage.warning('请先勾选要调整的货品'); return }
  batchForm.items = selected.map(r => ({ productId: r.id, productName: r.productName, currentStock: r.currentStock, quantity: 0 }))
  batchForm.adjustType = 'IN'
  batchForm.remark = ''
  batchDialogVisible.value = true
}

async function submitBatchAdjust() {
  const invalid = batchForm.items.some(i => i.quantity === 0)
  if (invalid) { ElMessage.warning('请填写所有货品的调整数量'); return }
  try {
    await batchAdjustStock({ items: batchForm.items, adjustType: batchForm.adjustType, remark: batchForm.remark })
    ElMessage.success('批量调整成功')
    batchDialogVisible.value = false
    loadData()
  } catch (error) {}
}

onMounted(() => loadData())
</script>

<style scoped>
.product-list {
  max-width: 1400px;
}
.search-form { margin-bottom: var(--space-md); }
.toolbar { margin-bottom: var(--space-md); }
.pagination-wrapper { margin-top: var(--space-md); display: flex; justify-content: flex-end; }
</style>
