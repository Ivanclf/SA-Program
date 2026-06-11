<template>
  <div class="header-bar">
    <!-- 左侧：折叠按钮 -->
    <div class="header-left">
      <el-button
        text
        @click="$emit('toggleCollapse')"
      >
        <el-icon :size="20">
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>
      </el-button>
    </div>

    <!-- 右侧：用户信息 + 登出 -->
    <div class="header-right">
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-icon><UserFilled /></el-icon>
          <span class="username">{{ authStore.user?.username || '未登录' }}</span>
          <el-icon class="arrow"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ authStore.isAdmin ? '管理员' : '审核员' }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Fold, Expand, UserFilled, ArrowDown,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineProps<{
  isCollapse: boolean
}>()

defineEmits<{
  toggleCollapse: []
}>()

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.header-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
  font-size: 0.95rem;
}

.user-info .username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-info .arrow {
  font-size: 0.75rem;
  color: #999;
}
</style>
