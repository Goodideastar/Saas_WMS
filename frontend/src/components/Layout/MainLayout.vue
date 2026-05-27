<template>
  <el-container class="layout-root">
    <el-aside :width="isCollapse ? '64px' : '240px'" class="layout-aside">
      <div class="aside-header">
        <div class="aside-logo">
          <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" class="logo-icon">
            <rect x="4" y="10" width="28" height="20" rx="2" stroke="currentColor" stroke-width="2"/>
            <path d="M4 16h28" stroke="currentColor" stroke-width="2"/>
            <rect x="7" y="6" width="4" height="4" rx="0.5" stroke="currentColor" stroke-width="1.5"/>
            <rect x="25" y="6" width="4" height="4" rx="0.5" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!isCollapse" class="aside-title">WMS</span>
        </transition>
      </div>

      <div class="aside-nav">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#e2e8f0"
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <template #title>数据看板</template>
          </el-menu-item>
          <el-menu-item index="/product/list">
            <el-icon><Box /></el-icon>
            <template #title>货品管理</template>
          </el-menu-item>
          <el-menu-item index="/inbound/list">
            <el-icon><Download /></el-icon>
            <template #title>入库管理</template>
          </el-menu-item>
          <el-menu-item index="/outbound/list">
            <el-icon><Upload /></el-icon>
            <template #title>出库管理</template>
          </el-menu-item>
          <el-menu-item index="/alert/index">
            <el-icon><Warning /></el-icon>
            <template #title>库存预警</template>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="aside-footer" v-if="!isCollapse">
        <div class="status-dot"></div>
        <span class="status-text">System Online</span>
      </div>
    </el-aside>

    <el-container class="layout-main">
      <el-header class="layout-header">
        <div class="header-left">
          <button class="collapse-trigger" @click="isCollapse = !isCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">
              <el-icon :size="14"><HomeFilled /></el-icon>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              <span class="breadcrumb-current">{{ route.meta.title }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <button class="header-btn" title="全屏">
            <el-icon :size="16"><FullScreen /></el-icon>
          </button>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-badge">
              <el-avatar :size="32" class="user-avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="user-name">{{ userInfo?.username || '管理员' }}</span>
              <el-icon :size="12" class="user-chevron"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
<AiChatTrigger :open="aiPanelOpen" @toggle="aiPanelOpen = !aiPanelOpen" />
<AiChatPanel :visible="aiPanelOpen" @close="aiPanelOpen = false" />
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { HomeFilled, UserFilled, ArrowDown, SwitchButton, FullScreen } from '@element-plus/icons-vue'
import AiChatTrigger from '@/components/AiChat/AiChatTrigger.vue'
import AiChatPanel from '@/components/AiChat/AiChatPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const aiPanelOpen = ref(false)
const activeMenu = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)

async function handleCommand(command) {
  if (command === 'logout') {
    await userStore.logoutAction()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-root {
  height: 100vh;
  background: var(--color-bg);
}

/* === Aside === */

.layout-aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow);
  overflow: hidden;
  position: relative;
}

.layout-aside::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 1px;
  background: rgba(255, 255, 255, 0.04);
}

.aside-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.aside-logo {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: #60a5fa;
}

.aside-title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.aside-nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.aside-nav :deep(.el-menu) {
  background: transparent;
}

.aside-nav :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: var(--radius-sm);
  height: 42px;
  line-height: 42px;
  font-size: var(--font-base);
  font-weight: 500;
  color: var(--sidebar-text);
  transition: all var(--transition-fast);
}

.aside-nav :deep(.el-menu-item:hover) {
  background: var(--sidebar-bg-hover);
  color: #cbd5e1;
}

.aside-nav :deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.2);
  color: #e2e8f0;
  font-weight: 600;
}

.aside-nav :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.aside-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
  flex-shrink: 0;
}

.status-text {
  font-size: var(--font-xs);
  color: var(--text-muted);
  white-space: nowrap;
}

/* === Header === */

.layout-main {
  flex-direction: column;
  min-width: 0;
}

.layout-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-trigger {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.collapse-trigger:hover {
  background: var(--color-bg);
  color: var(--text-primary);
}

.breadcrumb-current {
  font-weight: 500;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.header-btn:hover {
  background: var(--color-bg);
  color: var(--text-primary);
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.user-badge:hover {
  background: var(--color-bg);
}

.user-avatar {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.user-name {
  font-size: var(--font-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.user-chevron {
  color: var(--text-muted);
}

/* === Content === */

.layout-content {
  padding: var(--space-lg);
  background: var(--color-bg);
  overflow-y: auto;
  flex: 1;
}
</style>
