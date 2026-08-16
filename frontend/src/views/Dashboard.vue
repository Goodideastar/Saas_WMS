<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>数据看板</h1>
      <div class="header-actions">
        <span class="update-time">上次更新: {{ updateTime }}</span>
        <el-button size="small" :icon="Refresh" @click="loadAll" :loading="refreshing">刷新</el-button>
      </div>
    </div>

    <div class="stat-grid stagger-list">
      <div class="stat-card" v-for="item in statCards" :key="item.key">
        <div class="stat-card-icon" :style="{ background: item.color + '15', color: item.color }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-card-body">
          <span class="stat-card-label">{{ item.label }}</span>
          <span class="stat-card-value" :class="{ 'text-danger': item.key === 'alertCount' }">
            <template v-if="item.key === 'inboundAmount' || item.key === 'outboundAmount'">¥</template>{{ item.value }}
          </span>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card chart-wide">
        <div class="chart-card-header">
          <h3>近7天出入库趋势</h3>
          <span class="chart-subtitle">单位: 件</span>
        </div>
        <div ref="trendChartRef" class="chart-body"></div>
      </div>

      <div class="chart-card">
        <div class="chart-card-header">
          <h3>货品出库排行</h3>
          <span class="chart-subtitle">Top 10</span>
        </div>
        <div ref="topProductsChartRef" class="chart-body"></div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-card-header">
          <h3>库存预警分布</h3>
        </div>
        <div ref="alertChartRef" class="chart-body"></div>
      </div>

      <div class="chart-card chart-wide">
        <div class="chart-card-header">
          <h3>仓库库存分布</h3>
        </div>
        <div ref="warehouseChartRef" class="chart-body"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getTodaySummary, getLast7DaysTrend, getAlertStats, getTopProducts, getWarehouseDistribution } from '@/api/dashboard.js'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const summary = ref({})
const alertStats = ref({})
const refreshing = ref(false)

const updateTime = ref(dayjs().format('HH:mm:ss'))

const statCards = computed(() => [
  { key: 'inboundCount', label: '今日入库单数', value: summary.value.inboundCount ?? 0, icon: 'Download', color: '#2563eb' },
  { key: 'outboundCount', label: '今日出库单数', value: summary.value.outboundCount ?? 0, icon: 'Upload', color: '#7c3aed' },
  { key: 'inboundAmount', label: '今日入库金额', value: (summary.value.inboundAmount ?? 0).toLocaleString(), icon: 'Coin', color: '#059669' },
  { key: 'outboundAmount', label: '今日出库金额', value: (summary.value.outboundAmount ?? 0).toLocaleString(), icon: 'Money', color: '#d97706' },
  { key: 'totalStock', label: '总库存量', value: (summary.value.totalStock ?? 0).toLocaleString(), icon: 'Box', color: '#0284c7' },
  { key: 'alertCount', label: '未处理预警', value: alertStats.value.unhandled ?? 0, icon: 'Warning', color: '#dc2626' }
])

const trendChartRef = ref(null)
const topProductsChartRef = ref(null)
const alertChartRef = ref(null)
const warehouseChartRef = ref(null)

let trendChart, topProductsChart, alertChart, warehouseChart

const CHART_COLORS = {
  primary: '#2563eb',
  purple: '#7c3aed',
  green: '#059669',
  amber: '#d97706',
  red: '#dc2626',
  blue: '#0284c7',
  gray: '#94a3b8'
}

async function loadAll() {
  refreshing.value = true
  try {
    const [summaryRes, alertRes, trendRes, topRes, warehouseRes] = await Promise.all([
      getTodaySummary(),
      getAlertStats(),
      getLast7DaysTrend(),
      getTopProducts(),
      getWarehouseDistribution()
    ])
    summary.value = summaryRes.data || {}
    alertStats.value = {
      unhandled: Number(alertRes.data?.unhandled ?? 0),
      belowMin: Number(alertRes.data?.belowMin ?? 0),
      aboveMax: Number(alertRes.data?.aboveMax ?? 0)
    }
    renderTrendChart((trendRes.data || []).map(d => ({ date: d.date, inboundQuantity: Number(d.inboundQuantity ?? 0), outboundQuantity: Number(d.outboundQuantity ?? 0) })))
    renderTopProductsChart((topRes.data || []).map(d => ({ productName: d.productName, outboundQuantity: Number(d.outboundQuantity ?? 0) })))
    renderAlertChart(alertStats.value)
    renderWarehouseChart((warehouseRes.data || []).map(d => ({ warehouseName: d.warehouseName, stockQuantity: Number(d.stockQuantity ?? 0) })))
    updateTime.value = dayjs().format('HH:mm:ss')
  } finally {
    refreshing.value = false
  }
}

function makeChart(domRef) {
  const instance = echarts.init(domRef.value)
  instance.setOption({ color: Object.values(CHART_COLORS) })
  return instance
}

function renderTrendChart(data) {
  if (!trendChart) trendChart = makeChart(trendChartRef)
  const dates = data.map(d => d.date?.slice(5) ?? '')
  trendChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    legend: { bottom: 0, textStyle: { color: '#64748b' }, itemGap: 24 },
    grid: { top: 16, left: 8, right: 16, bottom: 36 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    series: [
      { name: '入库', type: 'line', data: data.map(d => d.inboundQuantity ?? 0), smooth: true, symbolSize: 4, lineStyle: { width: 2 }, areaStyle: { opacity: 0.06 } },
      { name: '出库', type: 'line', data: data.map(d => d.outboundQuantity ?? 0), smooth: true, symbolSize: 4, lineStyle: { width: 2 }, areaStyle: { opacity: 0.06 } }
    ]
  })
}

function renderTopProductsChart(data) {
  if (!topProductsChart) topProductsChart = makeChart(topProductsChartRef)
  const names = data.map(d => d.productName)
  const quantities = data.map(d => d.outboundQuantity ?? 0)
  topProductsChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    grid: { top: 8, left: 4, right: 16, bottom: 8 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    yAxis: { type: 'category', data: names.reverse(), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#64748b', fontSize: 11 } },
    series: [{
      type: 'bar', data: quantities.reverse(),
      itemStyle: { borderRadius: [0, 4, 4, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#2563eb' }, { offset: 1, color: '#60a5fa' }]) },
      barWidth: 12,
      label: { show: true, position: 'right', color: '#64748b', fontSize: 11 }
    }]
  })
}

function renderAlertChart(data) {
  if (!alertChart) alertChart = makeChart(alertChartRef)
  alertChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    legend: { bottom: 0, textStyle: { color: '#64748b' } },
    series: [{
      type: 'pie',
      radius: ['56%', '78%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scaleSize: 8 },
      data: [
        { value: data.belowMin ?? 0, name: '低于下限' },
        { value: data.aboveMax ?? 0, name: '高于上限' }
      ]
    }]
  })
}

function renderWarehouseChart(data) {
  if (!warehouseChart) warehouseChart = makeChart(warehouseChartRef)
  const names = data.map(d => d.warehouseName)
  const quantities = data.map(d => d.stockQuantity ?? 0)
  warehouseChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    grid: { top: 16, left: 8, right: 16, bottom: 16 },
    xAxis: { type: 'category', data: names, axisTick: { show: false }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    series: [{
      type: 'bar', data: quantities,
      barWidth: 32,
      itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#7c3aed' }, { offset: 1, color: '#a78bfa' }]) },
      label: { show: true, position: 'top', color: '#64748b', fontSize: 11 }
    }]
  })
}

function handleResize() {
  trendChart?.resize()
  topProductsChart?.resize()
  alertChart?.resize()
  warehouseChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  topProductsChart?.dispose()
  alertChart?.dispose()
  warehouseChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
}

.page-header h1 {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.update-time {
  font-size: var(--font-xs);
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

/* Stat Grid */

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: var(--space-lg);
}

.stat-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition-base);
  cursor: default;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-card-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.stat-card-label {
  font-size: var(--font-xs);
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-card-value {
  font-family: var(--font-display);
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-card-value.text-danger {
  color: var(--color-danger);
}

/* Chart Grid */

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
  transition: box-shadow var(--transition-base);
}

.chart-card:hover {
  box-shadow: var(--shadow-md);
}

.chart-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.chart-card-header h3 {
  font-family: var(--font-display);
  font-size: var(--font-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.01em;
}

.chart-subtitle {
  font-size: var(--font-xs);
  color: var(--text-muted);
}

.chart-body {
  height: 300px;
  width: 100%;
}

.chart-wide {
  grid-column: span 1;
}

@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
