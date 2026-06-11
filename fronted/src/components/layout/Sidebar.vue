<template>
  <div class="sidebar">
    <!-- Logo 区域 -->
    <div class="sidebar-logo">
      <span v-if="!isCollapse" class="logo-text">促销管理系统</span>
      <span v-else class="logo-text-short">促销</span>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
    >
      <!-- 管理员菜单 -->
      <template v-if="authStore.isAdmin">
        <el-menu-item index="/promotion">
          <el-icon><Present /></el-icon>
          <template #title>活动管理</template>
        </el-menu-item>
        <el-menu-item index="/sku">
          <el-icon><Goods /></el-icon>
          <template #title>SKU管理</template>
        </el-menu-item>
      </template>

      <!-- 审核员菜单 -->
      <template v-if="authStore.isAuditor">
        <el-menu-item index="/audit">
          <el-icon><Checked /></el-icon>
          <template #title>审核任务</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Present, Goods, Checked } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineProps<{
  isCollapse: boolean
}>()

const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/promotion')) return '/promotion'
  if (path.startsWith('/sku')) return '/sku'
  if (path.startsWith('/audit')) return '/audit'
  return path
})
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 1.1rem;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  white-space: nowrap;
  overflow: hidden;
  transition: font-size 0.2s ease;
}

.logo-text-short {
  font-size: 0.85rem;
}

/* Element Plus menu 覆盖 */
:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}
</style>
