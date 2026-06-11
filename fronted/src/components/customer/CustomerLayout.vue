<template>
  <div class="customer-layout">
    <!-- 简洁顶栏 -->
    <header class="customer-header">
      <div class="header-content">
        <h1 class="header-title">促销活动</h1>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="customer-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 -->
    <footer class="customer-footer">
      <span>促销活动管理系统 © 2026</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.customer-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.header-content {
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.header-title {
  font-size: 1.2rem;
  color: #409eff;
  margin: 0;
}

.customer-main {
  flex: 1;
  max-width: 960px;
  margin: 24px auto;
  width: 100%;
  padding: 0 16px;
}

.customer-footer {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 0.85rem;
  border-top: 1px solid #e6e6e6;
}

/* ---- Router 页面切换过渡 ---- */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ---- 响应式 ---- */
@media screen and (max-width: 767px) {
  .customer-header {
    padding: 0 16px;
  }

  .customer-main {
    margin: 16px auto;
    padding: 0 12px;
  }
}

@media screen and (max-width: 480px) {
  .customer-main {
    margin: 12px auto;
    padding: 0 8px;
  }

  .customer-footer {
    font-size: 0.75rem;
    padding: 12px;
  }
}
</style>
