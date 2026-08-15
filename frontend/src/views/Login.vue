<template>
  <div class="login-page">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <div class="scan-line"></div>
    <div class="bg-grid"></div>

    <div class="login-wrapper">
      <!-- Left: Brand + Tech Visual -->
      <div class="login-brand">
        <div class="brand-bg-icon">
          <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="10" y="30" width="100" height="70" rx="2" stroke="rgba(6,182,212,0.12)" stroke-width="1"/>
            <line x1="10" y1="50" x2="110" y2="50" stroke="rgba(6,182,212,0.08)" stroke-width="1"/>
            <line x1="10" y1="70" x2="110" y2="70" stroke="rgba(6,182,212,0.08)" stroke-width="1"/>
            <line x1="40" y1="30" x2="40" y2="100" stroke="rgba(6,182,212,0.08)" stroke-width="1"/>
            <line x1="70" y1="30" x2="70" y2="100" stroke="rgba(6,182,212,0.08)" stroke-width="1"/>
            <rect x="18" y="36" width="14" height="10" rx="1" stroke="rgba(6,182,212,0.15)" stroke-width="1"/>
            <rect x="48" y="56" width="14" height="10" rx="1" stroke="rgba(6,182,212,0.15)" stroke-width="1"/>
            <rect x="78" y="76" width="14" height="10" rx="1" stroke="rgba(6,182,212,0.15)" stroke-width="1"/>
          </svg>
        </div>

        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="4" y="12" width="40" height="28" rx="3" stroke="currentColor" stroke-width="2.5"/>
            <path d="M4 20h40" stroke="currentColor" stroke-width="2.5"/>
            <rect x="8" y="6" width="6" height="6" rx="1" stroke="currentColor" stroke-width="2"/>
            <rect x="34" y="6" width="6" height="6" rx="1" stroke="currentColor" stroke-width="2"/>
            <rect x="17" y="14" width="14" height="12" rx="1" fill="currentColor" opacity="0.15"/>
          </svg>
        </div>
        <h1 class="brand-title">WMS</h1>
        <p class="brand-sub">智能仓储管理系统</p>

        <div class="brand-status">
          <span class="status-dot"></span>
          <span>SYS ONLINE</span>
        </div>
      </div>

      <!-- Right: Login Form -->
      <div class="login-card">
        <div class="card-top-accent"></div>
        <div class="card-header">
          <h2>登录</h2>
          <p>输入您的账号以继续</p>
        </div>

        <div v-if="isExpired" class="expired-banner">
          <el-icon :size="16"><WarningFilled /></el-icon>
          <span>登录已过期，请重新登录</span>
        </div>

        <el-form ref="formRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
              class="tech-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              class="tech-input"
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>
          <el-form-item>
            <button type="submit" class="login-btn" @click.prevent="handleLogin" :disabled="loading">
              <span v-if="!loading" class="btn-text">登 录</span>
              <span v-else class="btn-loading">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </span>
            </button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <p class="login-footer">WMS v1.0 · Secure Connection</p>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const canvasRef = ref(null)

const isExpired = computed(() => route.query.expired === '1')

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginAction({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// --- Particle Canvas ---
let animId = null
const particles = []
const PARTICLE_COUNT = 30

function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const brand = canvas.parentElement
  canvas.width = brand.offsetWidth
  canvas.height = brand.offsetHeight

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      r: Math.random() * 1.5 + 0.8,
      alpha: Math.random() * 0.4 + 0.15
    })
  }
}

function drawParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    if (p.x < 0) p.x = canvas.width
    if (p.x > canvas.width) p.x = 0
    if (p.y < 0) p.y = canvas.height
    if (p.y > canvas.height) p.y = 0

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(6, 182, 212, ${p.alpha})`
    ctx.fill()
  }

  // Draw faint connection lines between close particles
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 80) {
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(6, 182, 212, ${0.06 * (1 - dist / 80)})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }

  animId = requestAnimationFrame(drawParticles)
}

onMounted(() => {
  initCanvas()
  drawParticles()
})

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #0a0f1e;
  position: relative;
  overflow: hidden;
}

/* Particle canvas behind everything in the left panel area */
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

/* Subtle grid overlay */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(6, 182, 212, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(6, 182, 212, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  pointer-events: none;
  z-index: 0;
}

/* Horizontal scan line */
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.25), transparent);
  z-index: 1;
  animation: scanDown 4s linear infinite;
  pointer-events: none;
}

@keyframes scanDown {
  0% { top: 0; opacity: 0; }
  5% { opacity: 1; }
  95% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.login-wrapper {
  display: flex;
  align-items: stretch;
  gap: 0;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(6, 182, 212, 0.12),
    0 0 60px rgba(6, 182, 212, 0.06);
  position: relative;
  z-index: 1;
}

/* ── Left Brand Panel ── */
.login-brand {
  width: 260px;
  background: linear-gradient(160deg, rgba(6, 182, 212, 0.06) 0%, rgba(10, 15, 30, 0.95) 100%);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  border-right: 1px solid rgba(6, 182, 212, 0.12);
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.brand-bg-icon {
  position: absolute;
  bottom: 20px;
  right: -10px;
  width: 140px;
  height: 140px;
  opacity: 0.5;
  color: #06b6d4;
  pointer-events: none;
}

.brand-icon {
  width: 64px;
  height: 64px;
  color: #06b6d4;
  margin-bottom: 8px;
  filter: drop-shadow(0 0 12px rgba(6, 182, 212, 0.5));
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% { filter: drop-shadow(0 0 12px rgba(6, 182, 212, 0.4)); }
  50% { filter: drop-shadow(0 0 20px rgba(6, 182, 212, 0.8)); }
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 0.06em;
  margin: 0;
}

.brand-sub {
  font-size: var(--font-sm);
  color: rgba(148, 163, 184, 0.7);
  letter-spacing: 0.1em;
  margin: 0;
}

.brand-status {
  position: absolute;
  bottom: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: rgba(6, 182, 212, 0.6);
  letter-spacing: 0.12em;
  font-family: monospace;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #06b6d4;
  box-shadow: 0 0 6px rgba(6, 182, 212, 0.8);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ── Right Login Card ── */
.login-card {
  width: 400px;
  padding: 48px 40px 40px;
  background: #111827;
  position: relative;
}

.card-top-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #06b6d4, #3b82f6);
}

.expired-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
  border-radius: var(--radius-sm);
  color: #fbbf24;
  font-size: var(--font-sm);
  font-weight: 500;
}

.card-header {
  margin-bottom: 32px;
}

.card-header h2 {
  font-family: var(--font-display);
  font-size: 1.375rem;
  font-weight: 600;
  color: #e2e8f0;
  margin: 0 0 6px;
  letter-spacing: 0.01em;
}

.card-header p {
  font-size: var(--font-sm);
  color: rgba(148, 163, 184, 0.7);
  margin: 0;
}

/* Tech-styled input wrapper */
.tech-input :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(6, 182, 212, 0.15);
  box-shadow: none !important;
  border-radius: 6px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.tech-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(6, 182, 212, 0.6);
  box-shadow: 0 0 0 1px rgba(6, 182, 212, 0.15) !important;
}

.tech-input :deep(.el-input__prefix) {
  color: rgba(6, 182, 212, 0.6);
}

.tech-input :deep(.el-input__inner) {
  color: #e2e8f0;
}

.tech-input :deep(.el-input__inner::placeholder) {
  color: rgba(148, 163, 184, 0.4);
}

/* Login button */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: var(--font-base);
  font-weight: 600;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  border: none;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform 0.1s, box-shadow 0.2s;
}

.login-btn:hover:not(:disabled) {
  box-shadow: 0 4px 20px rgba(6, 182, 212, 0.35);
  transform: translateY(-1px);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

/* Button shine sweep */
.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s;
}

.login-btn:hover::after {
  left: 120%;
}

/* Loading dots */
.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.btn-loading .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
  animation: dotBounce 1.2s ease-in-out infinite;
}

.btn-loading .dot:nth-child(2) { animation-delay: 0.2s; }
.btn-loading .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* Remember checkbox */
:deep(.el-checkbox__label) {
  color: rgba(148, 163, 184, 0.7);
  font-size: var(--font-sm);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #06b6d4;
  border-color: #06b6d4;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  border-color: #fff;
}

.login-footer {
  position: absolute;
  bottom: 20px;
  font-size: var(--font-xs);
  color: rgba(100, 116, 139, 0.4);
  letter-spacing: 0.04em;
  z-index: 1;
}
</style>
