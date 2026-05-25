<template>
  <div class="error-page">
    <div class="error-code">404</div>
    <div class="error-divider"></div>
    <h2>页面不存在</h2>
    <p>您访问的页面不存在或已被移除</p>
    <el-button type="primary" size="large" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.error-code {
  font-family: var(--font-display);
  font-size: 8rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.03em;
}

.error-divider {
  width: 48px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
  margin: 24px 0;
}

h2 {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

p {
  font-size: var(--font-base);
  color: var(--text-secondary);
  margin: 0 0 32px;
}
</style>
