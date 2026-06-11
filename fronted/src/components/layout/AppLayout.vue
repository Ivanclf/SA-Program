<template>
  <el-container class="app-layout">
    <!-- 移动端遮罩 -->
    <div
      v-if="mobileMenuOpen"
      class="mobile-overlay"
      @click="mobileMenuOpen = false"
    />

    <!-- 侧边栏 -->
    <el-aside
      :width="isCollapse ? '64px' : '200px'"
      class="app-aside"
      :class="{ 'aside-mobile-open': mobileMenuOpen }"
    >
      <Sidebar :is-collapse="isCollapse && !mobileMenuOpen" />
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="app-header" height="56px">
        <HeaderBar
          :is-collapse="isCollapse"
          @toggle-collapse="toggleCollapse"
        />
      </el-header>

      <!-- 内容区 -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import HeaderBar from './HeaderBar.vue'

const isCollapse = ref(false)
const mobileMenuOpen = ref(false)

function toggleCollapse() {
  if (window.innerWidth < 768) {
    mobileMenuOpen.value = !mobileMenuOpen.value
  } else {
    isCollapse.value = !isCollapse.value
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  background-color: #304156;
  overflow: hidden;
  transition: width 0.25s ease;
  flex-shrink: 0;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 16px;
}

.app-main {
  background-color: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
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

/* ---- 移动端遮罩 ---- */
.mobile-overlay {
  display: none;
}

/* ---- 响应式 ---- */
@media screen and (max-width: 767px) {
  .app-aside {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1001;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    width: 200px !important;
  }

  .app-aside.aside-mobile-open {
    transform: translateX(0);
  }

  .mobile-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(0, 0, 0, 0.4);
  }

  .app-main {
    padding: 12px;
  }
}

@media screen and (max-width: 480px) {
  .app-main {
    padding: 8px;
  }
}
</style>
