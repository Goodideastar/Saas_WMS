<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>今日入库单数</template>
          <div class="stat-value">{{ summary.inboundCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>今日出库单数</template>
          <div class="stat-value">{{ summary.outboundCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>今日入库金额</template>
          <div class="stat-value">¥{{ summary.inboundAmount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>今日出库金额</template>
          <div class="stat-value">¥{{ summary.outboundAmount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>总库存量</template>
          <div class="stat-value">{{ summary.totalStock || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <template #header>未处理预警</template>
          <div class="stat-value text-danger">{{ alertStats.unhandled || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>近7天出入库趋势</span>
              <el-button size="small" @click="loadAll">刷新</el-button>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>货品出库排行榜 (Top 10)</template>
          <div ref="topProductsChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>库存预警统计</template>
          <div ref="alertChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>仓库库存分布</template>
          <div ref="warehouseChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getTodaySummary, getLast7DaysTrend, getAlertStats, getTopProducts, getWarehouseDistribution } from '@/api/dashboard.js'
import * as echarts from 'echarts'

const summary = ref({})
const alertStats = ref({})
const trendChartRef = ref(null)
const topProductsChartRef = ref(null)
const alertChartRef = ref(null)
const warehouseChartRef = ref(null)

let trendChart, topProductsChart, alertChart, warehouseChart

async function loadAll() {
  try {
    const [summaryRes, alertRes, trendRes, topRes, warehouseRes] = await Promise.all([
      getTodaySummary(),
      getAlertStats(),
      getLast7DaysTrend(),
      getTopProducts(),
      getWarehouseDistribution()
    ])
    summary.value = summaryRes.data || {}
    alertStats.value = alertRes.data || {}
    renderTrendChart(trendRes.data || [])
    renderTopProductsChart(topRes.data || [])
    renderAlertChart(alertRes.data || {})
    renderWarehouseChart(warehouseRes.data || [])
  } catch (error) {
    console.error('Failed to load dashboard data', error)
  }
}

function renderTrendChart(data) {
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const dates = data.map(d => d.date)
  const inbound = data.map(d => d.inboundQuantity || 0)
  const outbound = data.map(d => d.outboundQuantity || 0)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '入库', type: 'line', data: inbound, smooth: true },
      { name: '出库', type: 'line', data: outbound, smooth: true }
    ]
  })
}

function renderTopProductsChart(data) {
  if (!topProductsChart) topProductsChart = echarts.init(topProductsChartRef.value)
  const names = data.map(d => d.productName)
  const quantities = data.map(d => d.outboundQuantity)
  topProductsChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names },
    series: [{ type: 'bar', data: quantities, itemStyle: { color: '#409EFF' } }]
  })
}

function renderAlertChart(data) {
  if (!alertChart) alertChart = echarts.init(alertChartRef.value)
  alertChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: data.belowMin || 0, name: '低于下限' },
        { value: data.aboveMax || 0, name: '高于上限' }
      ],
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  })
}

function renderWarehouseChart(data) {
  if (!warehouseChart) warehouseChart = echarts.init(warehouseChartRef.value)
  const names = data.map(d => d.warehouseName)
  const quantities = data.map(d => d.stockQuantity)
  warehouseChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: names.map((name, i) => ({ name, value: quantities[i] })),
      label: { formatter: '{b}: {d}%' }
    }]
  })
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', () => {
    trendChart?.resize()
    topProductsChart?.resize()
    alertChart?.resize()
    warehouseChart?.resize()
  })
})

onUnmounted(() => {
  trendChart?.dispose()
  topProductsChart?.dispose()
  alertChart?.dispose()
  warehouseChart?.dispose()
})
</script>

<style scoped>
.dashboard-container { padding: 0; }
.mb-20 { margin-bottom: 20px; }
.stat-value { font-size: 28px; font-weight: bold; text-align: center; padding: 10px 0; }
.text-danger { color: #f56c6c; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
