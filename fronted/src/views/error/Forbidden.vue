<template>
  <div class="error-page">
    <div class="error-card">
      <h1 class="error-code">403</h1>
      <p class="error-desc">无权限访问</p>
      <p class="error-hint">您当前的账号角色没有访问该页面的权限</p>
      <el-button type="primary" @click="goLogin">返回登录页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goLogin() {
  router.push('/login')
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
  color: #e6a23c;
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
