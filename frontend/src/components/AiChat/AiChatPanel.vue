<template>
  <div class="ai-panel" :class="{ open: visible }">
    <div class="ai-panel-header">
      <span>AI 助手</span>
      <button @click="$emit('close')">&times;</button>
    </div>
    <div class="ai-panel-messages" ref="msgContainer">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-content">{{ msg.content }}</div>
        <div v-if="msg.toolCalls && msg.toolCalls.length" class="tool-calls">
          <div v-for="tc in msg.toolCalls" :key="tc.tool" class="tool-chip">
            {{ tc.status === 'running' ? '...' : 'OK' }} {{ tc.tool }}
          </div>
        </div>
        <!-- 图表渲染 -->
        <div v-if="msg.chartData" class="chart-container">
          <div v-if="msg.chartData.trend" ref="trendChartRef" class="chart-body"></div>
          <div v-if="msg.chartData.top_products" ref="topProductsChartRef" class="chart-body"></div>
          <div v-if="msg.chartData.warehouse" ref="warehouseChartRef" class="chart-body"></div>
        </div>
      </div>
    </div>
    <div class="ai-panel-input">
      <input v-model="input" @keyup.enter="send" placeholder="输入问题..." :disabled="loading" />
      <button @click="send" :disabled="loading">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'

defineProps({ visible: Boolean })
defineEmits(['close'])

const userStore = useUserStore()
const input = ref('')
const loading = ref(false)
const messages = ref([])
const msgContainer = ref(null)
const trendChartRef = ref(null)
const topProductsChartRef = ref(null)
const warehouseChartRef = ref(null)

let trendChart, topProductsChart, warehouseChart

const send = async () => {
  if (!input.value.trim() || loading.value) return
  const text = input.value
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  loading.value = true

  const assistantMsg = { role: 'assistant', content: '', toolCalls: [], chartData: null }
  messages.value.push(assistantMsg)

  const resp = await fetch('/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${userStore.token}` },
    body: JSON.stringify({ message: text, session_id: 'default' }),
  })

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = 'message'
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    for (const line of lines) {
      const trimmed = line.trim()
      if (trimmed.startsWith('event:')) {
        currentEvent = trimmed.slice(6).trim()
      } else if (trimmed.startsWith('data:')) {
        const dataStr = trimmed.slice(5).trim()
        if (!dataStr) continue
        try {
          const data = JSON.parse(dataStr)
          if (data.type === 'step_start') {
            assistantMsg.toolCalls.push({ tool: data.tool, status: 'running' })
          } else if (data.type === 'step_end') {
            const tc = assistantMsg.toolCalls.find(t => t.tool === data.tool && t.status === 'running')
            if (tc) tc.status = 'done'
          } else if (data.type === 'text_chunk') {
            assistantMsg.content += data.content
            scrollToBottom()
          } else if (data.type === 'done') {
            if (!assistantMsg.content && data.summary) {
              assistantMsg.content = data.summary
            }
            if (data.chart_data && Object.keys(data.chart_data).length > 0) {
              assistantMsg.chartData = data.chart_data
              nextTick(() => renderCharts(assistantMsg.chartData))
            }
          } else if (data.type === 'error') {
            assistantMsg.content = '[错误] ' + (data.message || '请求失败，请稍后重试')
          }
        } catch (e) {
          console.warn('SSE parse error:', e, dataStr)
        }
      }
    }
  }
  loading.value = false
  scrollToBottom()
}

const renderCharts = (chartData) => {
  if (chartData.trend && trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    const dates = chartData.trend.map(d => d.date?.slice(5) ?? '')
    trendChart.setOption({
      title: { text: '近7天出入库趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      series: [
        { name: '入库', type: 'line', data: chartData.trend.map(d => d.inboundQuantity ?? 0), smooth: true },
        { name: '出库', type: 'line', data: chartData.trend.map(d => d.outboundQuantity ?? 0), smooth: true }
      ]
    })
  }
  if (chartData.top_products && topProductsChartRef.value) {
    if (!topProductsChart) topProductsChart = echarts.init(topProductsChartRef.value)
    const names = chartData.top_products.map(d => d.productName)
    const quantities = chartData.top_products.map(d => d.outboundQuantity ?? 0)
    topProductsChart.setOption({
      title: { text: '货品出库排行', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: names.reverse() },
      series: [{ type: 'bar', data: quantities.reverse(), barWidth: 12 }]
    })
  }
  if (chartData.warehouse && warehouseChartRef.value) {
    if (!warehouseChart) warehouseChart = echarts.init(warehouseChartRef.value)
    const names = chartData.warehouse.map(d => d.warehouseName)
    const quantities = chartData.warehouse.map(d => d.stockQuantity ?? 0)
    warehouseChart.setOption({
      title: { text: '仓库库存分布', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: names },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: quantities, barWidth: 32 }]
    })
  }
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = msgContainer.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.ai-panel {
  position: fixed; right: -400px; top: 0; width: 400px; height: 100vh;
  background: #fff; box-shadow: -2px 0 10px rgba(0,0,0,0.1); z-index: 1000;
  display: flex; flex-direction: column; transition: right 0.3s;
}
.ai-panel.open { right: 0; }
.ai-panel-header {
  padding: 16px; border-bottom: 1px solid #eee; display: flex;
  justify-content: space-between; align-items: center; font-weight: bold;
}
.ai-panel-header button { border: none; background: none; font-size: 20px; cursor: pointer; }
.ai-panel-messages { flex: 1; overflow-y: auto; padding: 16px; }
.ai-panel-input { padding: 12px; border-top: 1px solid #eee; display: flex; gap: 8px; }
.ai-panel-input input { flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; }
.ai-panel-input button { padding: 8px 16px; background: #409eff; color: #fff; border: none; border-radius: 6px; cursor: pointer; }
.msg { margin-bottom: 12px; }
.msg.user { text-align: right; }
.msg.user .msg-content { background: #409eff; color: #fff; display: inline-block; padding: 8px 12px; border-radius: 8px; max-width: 80%; }
.msg.assistant .msg-content { background: #f0f2f5; padding: 8px 12px; border-radius: 8px; }
.tool-calls { margin-top: 4px; }
.tool-chip { display: inline-block; padding: 2px 8px; margin: 2px; background: #e6f7ff; border-radius: 4px; font-size: 12px; }
.chart-container { margin-top: 12px; }
.chart-body { height: 200px; width: 100%; margin-bottom: 12px; }
</style>
