<template>
  <div class="login-page">
    <div class="login-bg-pattern"></div>

    <div class="login-wrapper">
      <div class="login-brand">
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
        <p class="brand-sub">仓储管理系统</p>
      </div>

      <div class="login-card">
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
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <p class="login-footer">WMS v1.0 · Secure Connection</p>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

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
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  position: relative;
  overflow: hidden;
}

.login-bg-pattern {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(37, 99, 235, 0.12), transparent),
    radial-gradient(ellipse 40% 60% at 80% 80%, rgba(37, 99, 235, 0.06), transparent);
  pointer-events: none;
}

.login-bg-pattern::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
}

.login-wrapper {
  display: flex;
  align-items: stretch;
  gap: 0;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
}

.login-brand {
  width: 260px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  gap: 12px;
}

.brand-icon {
  width: 64px;
  height: 64px;
  color: #60a5fa;
  margin-bottom: 8px;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.06em;
  margin: 0;
}

.brand-sub {
  font-size: var(--font-sm);
  color: var(--text-muted);
  letter-spacing: 0.1em;
  margin: 0;
}

.login-card {
  width: 400px;
  padding: 48px 40px 40px;
  background: #1e293b;
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
  color: #f1f5f9;
  margin: 0 0 6px;
  letter-spacing: 0.01em;
}

.card-header p {
  font-size: var(--font-sm);
  color: var(--text-muted);
  margin: 0;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: var(--font-base);
  font-weight: 600;
  letter-spacing: 0.06em;
  background: #2563eb;
  border-color: #2563eb;
}

.login-btn:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.login-footer {
  position: absolute;
  bottom: 20px;
  font-size: var(--font-xs);
  color: rgba(148, 163, 184, 0.4);
  letter-spacing: 0.04em;
  z-index: 1;
}
</style>
