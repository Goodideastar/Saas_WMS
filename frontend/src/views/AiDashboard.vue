<template>
  <div class="ai-dashboard">
    <h2>AI 深度分析</h2>

    <div class="analysis-input">
      <input v-model="query" @keyup.enter="analyze" placeholder="输入分析问题，如: 分析本月出库趋势并预测下周补货需求" />
      <button @click="analyze" :disabled="loading">分析</button>
    </div>

    <div v-if="loading" class="loading">AI分析中...</div>

    <div v-if="result" class="analysis-result">
      <div class="insight-card">
        <h3>分析洞察</h3>
        <p>{{ result.insight }}</p>
      </div>
    </div>

    <div class="preset-section">
      <h3>快捷分析</h3>
      <div class="preset-grid">
        <div class="preset-card" v-for="p in presets" :key="p.label" @click="query = p.question; analyze()">
          <div class="preset-icon">{{ p.icon }}</div>
          <div class="preset-label">{{ p.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const query = ref('')
const loading = ref(false)
const result = ref(null)

const presets = [
  { icon: '📦', label: '库存健康度分析', question: '分析所有商品的库存健康度，识别低库存和积压商品' },
  { icon: '📈', label: '出库趋势预测', question: '分析近7天出库趋势，预测下周需要补货的商品' },
  { icon: '⚠️', label: '异常告警分析', question: '分析当前所有未处理的库存告警，给出处理建议' },
  { icon: '🏭', label: '仓库效率分析', question: '分析各仓库的出入库效率，找出瓶颈' },
]

const analyze = async () => {
  if (!query.value.trim() || loading.value) return
  loading.value = true
  result.value = null
  const resp = await fetch('http://localhost:8090/ai/analysis', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${userStore.token}` },
    body: JSON.stringify({ query: query.value }),
  })
  result.value = await resp.json()
  loading.value = false
}
</script>

<style scoped>
.ai-dashboard { padding: 24px; max-width: 1200px; margin: 0 auto; }
h2 { margin-bottom: 24px; }
.analysis-input { display: flex; gap: 12px; margin-bottom: 24px; }
.analysis-input input { flex: 1; padding: 12px 16px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; }
.analysis-input button { padding: 12px 24px; background: #409eff; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
.loading { text-align: center; padding: 40px; color: #999; }
.insight-card { background: #f0f7ff; padding: 20px; border-radius: 8px; margin-bottom: 24px; line-height: 1.8; }
.preset-section { margin-top: 32px; }
.preset-section h3 { margin-bottom: 16px; }
.preset-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.preset-card { text-align: center; padding: 24px; background: #fff; border: 1px solid #eee; border-radius: 8px; cursor: pointer; transition: box-shadow 0.2s; }
.preset-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.preset-icon { font-size: 32px; margin-bottom: 8px; }
.preset-label { font-size: 14px; color: #666; }
</style>
