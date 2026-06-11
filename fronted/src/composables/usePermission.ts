import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 权限判断 composable
 * 提供 isAdmin / isAuditor 计算属性，用于组件中控制按钮显隐
 */
export function usePermission() {
  const authStore = useAuthStore()
  const isAdmin = computed(() => authStore.user?.role === 1)
  const isAuditor = computed(() => authStore.user?.role === 2)
  return { isAdmin, isAuditor }
}
