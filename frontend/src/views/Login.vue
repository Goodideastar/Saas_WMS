<template>
  <div class="login-page">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <div class="scan-line"></div>
    <div class="bg-grid"></div>

    <div class="login-wrapper">
      <!-- HUD 四角框 -->
      <span class="corner corner-tl"></span>
      <span class="corner corner-tr"></span>
      <span class="corner corner-bl"></span>
      <span class="corner corner-br"></span>

      <!-- Left: Brand + Tech Visual -->
      <div class="login-brand">
        <div class="brand-bg-rack">
          <!-- 立体货架线稿 -->
          <svg viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
            <g stroke="rgba(6,182,212,0.14)" stroke-width="1.2">
              <!-- 立柱 -->
              <path d="M40 60 L40 170 M160 60 L160 170 M70 45 L70 155 M130 45 L130 155"/>
              <!-- 横梁（三层） -->
              <path d="M40 85 L160 85 M40 125 L160 125 M40 165 L160 165"/>
              <path d="M70 75 L130 75 M70 115 L130 115 M70 155 L130 155"/>
              <!-- 斜撑 -->
              <path d="M40 85 L70 75 M70 75 L40 125 M40 125 L70 115 M160 85 L130 75 M130 75 L160 125 M160 125 L130 115"/>
            </g>
            <g stroke="rgba(6,182,212,0.30)" stroke-width="1.2">
              <!-- 托盘货箱 -->
              <rect x="48" y="92" width="18" height="12" rx="1"/>
              <rect x="86" y="52" width="18" height="12" rx="1"/>
              <rect x="86" y="92" width="18" height="12" rx="1"/>
              <rect x="124" y="92" width="18" height="12" rx="1"/>
              <rect x="86" y="132" width="18" height="12" rx="1"/>
              <rect x="48" y="132" width="18" height="12" rx="1"/>
            </g>
            <!-- 扫描光带 -->
            <rect x="36" y="0" width="128" height="3" fill="rgba(6,182,212,0.35)">
              <animate attributeName="y" values="55;160;55" dur="6s" repeatCount="indefinite"/>
            </rect>
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

        <div class="brand-metrics">
          <div class="metric">
            <span class="metric-label">ZONE</span>
            <span class="metric-value">A-07</span>
          </div>
          <div class="metric-sep"></div>
          <div class="metric">
            <span class="metric-label">LINK</span>
            <span class="metric-value ok">STABLE</span>
          </div>
        </div>

        <div class="brand-status">
          <span class="status-dot"></span>
          <span>SYS ONLINE</span>
        </div>
      </div>

      <!-- Right: Login Form -->
      <div class="login-card">
        <div class="card-top-accent"></div>
        <div class="card-header">
          <h2>身份验证</h2>
          <p>ACCESS CONTROL · 输入凭据接入仓储网络</p>
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
              autocomplete="username"
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
              autocomplete="current-password"
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="验证码"
                :prefix-icon="Key"
                size="large"
                maxlength="4"
                class="captcha-input tech-input"
                autocomplete="off"
              />
              <div
                class="captcha-img"
                :class="{ 'is-loading': captchaLoading }"
                title="点击刷新验证码"
                @click="loadCaptcha"
              >
                <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="验证码" />
                <el-icon v-else class="captcha-refresh-icon"><RefreshRight /></el-icon>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>
          <el-form-item>
            <button type="submit" class="login-btn" @click.prevent="handleLogin" :disabled="loading">
              <span v-if="!loading" class="btn-text">接 入 系 统</span>
              <span v-else class="btn-loading">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </span>
            </button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <p class="login-footer">WMS v1.0 · SECURE CHANNEL · TLS ENCRYPTED</p>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, RefreshRight, WarningFilled } from '@element-plus/icons-vue'

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
  remember: false,
  captchaKey: '',
  captchaCode: ''
})
const captchaImage = ref('')
const captchaLoading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    const data = res.data
    loginForm.captchaKey = data.key
    loginForm.captchaCode = ''
    captchaImage.value = 'data:image/png;base64,' + data.image
  } catch {
    ElMessage.error('验证码加载失败，点击图片重试')
  } finally {
    captchaLoading.value = false
  }
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginAction({
      username: loginForm.username,
      password: loginForm.password,
      captchaKey: loginForm.captchaKey,
      captchaCode: loginForm.captchaCode
    })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    // 验证码单次有效：任何登录失败后都刷新，避免下次提交撞 4001
    loadCaptcha()
    ElMessage.error(error?.msg || '登录失败，请检查用户名、密码和验证码')
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
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

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

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 90) {
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(6, 182, 212, ${0.06 * (1 - dist / 90)})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }

  animId = requestAnimationFrame(drawParticles)
}

const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

onMounted(() => {
  initCanvas()
  if (!prefersReducedMotion) drawParticles()
  loadCaptcha()
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
  background:
    radial-gradient(ellipse 80% 60% at 50% 0%, rgba(6, 182, 212, 0.06), transparent),
    #0a0f1e;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(6, 182, 212, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(6, 182, 212, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 70% 70% at 50% 50%, #000 30%, transparent 100%);
  pointer-events: none;
  z-index: 0;
}

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
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(6, 182, 212, 0.12),
    0 0 60px rgba(6, 182, 212, 0.06);
  position: relative;
  z-index: 1;
}

/* ── HUD 四角框 ── */
.corner {
  position: absolute;
  width: 14px;
  height: 14px;
  z-index: 2;
  pointer-events: none;
}
.corner-tl { top: -1px; left: -1px; border-top: 2px solid rgba(6, 182, 212, 0.7); border-left: 2px solid rgba(6, 182, 212, 0.7); }
.corner-tr { top: -1px; right: -1px; border-top: 2px solid rgba(6, 182, 212, 0.7); border-right: 2px solid rgba(6, 182, 212, 0.7); }
.corner-bl { bottom: -1px; left: -1px; border-bottom: 2px solid rgba(6, 182, 212, 0.7); border-left: 2px solid rgba(6, 182, 212, 0.7); }
.corner-br { bottom: -1px; right: -1px; border-bottom: 2px solid rgba(6, 182, 212, 0.7); border-right: 2px solid rgba(6, 182, 212, 0.7); }

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

.brand-bg-rack {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  opacity: 0.6;
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

/* ── 状态读数 ── */
.brand-metrics {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 8px 14px;
  border: 1px solid rgba(6, 182, 212, 0.15);
  border-radius: 4px;
  background: rgba(6, 182, 212, 0.04);
  font-family: monospace;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.metric-label {
  font-size: 9px;
  color: rgba(148, 163, 184, 0.5);
  letter-spacing: 0.15em;
}

.metric-value {
  font-size: 11px;
  color: rgba(6, 182, 212, 0.85);
  letter-spacing: 0.08em;
}

.metric-value.ok {
  color: #34d399;
  text-shadow: 0 0 6px rgba(52, 211, 153, 0.4);
}

.metric-sep {
  width: 1px;
  height: 22px;
  background: rgba(6, 182, 212, 0.2);
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
  font-size: var(--font-xs);
  color: rgba(148, 163, 184, 0.55);
  margin: 0;
  letter-spacing: 0.06em;
  font-family: monospace;
}

/* Tech-styled input */
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

/* ── Captcha row ── */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 110px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(6, 182, 212, 0.2);
  background: rgba(15, 23, 42, 0.8);
  flex-shrink: 0;
  transition: border-color 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-img:hover {
  border-color: rgba(6, 182, 212, 0.5);
  box-shadow: 0 0 8px rgba(6, 182, 212, 0.2);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-refresh-icon {
  color: rgba(6, 182, 212, 0.5);
  font-size: 20px;
  animation: spin 1s linear infinite;
}

.captcha-img.is-loading .captcha-refresh-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Login footer */
.login-footer {
  position: absolute;
  bottom: 20px;
  font-size: var(--font-xs);
  color: rgba(100, 116, 139, 0.4);
  letter-spacing: 0.04em;
  z-index: 1;
  font-family: monospace;
}

/* ── 响应式 ── */
@media (max-width: 720px) {
  .login-wrapper {
    flex-direction: column;
    width: calc(100vw - 32px);
    max-width: 400px;
  }

  .login-brand {
    width: 100%;
    padding: 32px 24px 44px;
    border-right: none;
    border-bottom: 1px solid rgba(6, 182, 212, 0.12);
  }

  .brand-bg-rack {
    display: none;
  }

  .login-card {
    width: 100%;
    padding: 32px 24px 32px;
  }

  .brand-status {
    bottom: 12px;
  }
}

/* ── 减少动态 ── */
@media (prefers-reduced-motion: reduce) {
  .scan-line,
  .brand-icon,
  .status-dot,
  .captcha-refresh-icon {
    animation: none !important;
  }
}
</style>
