<template>
  <div class="error-page">
    <h1>403</h1>
    <p>抱歉，您没有权限访问该页面</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
h1 { font-size: 72px; color: #409EFF; margin: 0; }
p { color: #666; margin: 20px 0; }
</style>
