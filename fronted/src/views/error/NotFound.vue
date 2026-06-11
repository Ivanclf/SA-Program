<template>
  <div class="error-page">
    <div class="error-card">
      <h1 class="error-code">404</h1>
      <p class="error-desc">页面不存在</p>
      <p class="error-hint">您访问的页面可能已被移除或地址输入有误</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.error-card {
  text-align: center;
  padding: 48px;
}

.error-code {
  font-size: 6rem;
  font-weight: bold;
  color: #409eff;
  margin: 0 0 8px;
  line-height: 1;
}

.error-desc {
  font-size: 1.5rem;
  color: #333;
  margin: 0 0 8px;
}

.error-hint {
  color: #999;
  margin: 0 0 24px;
}
</style>
