import { ref, computed, type Ref, type ComputedRef } from 'vue'

/**
 * usePagination —— 通用客户端分页 composable
 *
 * 使用方式:
 *   const filteredList = computed(() => rawList.filter(...))
 *   const { page, pageSize, total, pagedList, resetPage } = usePagination(filteredList)
 *
 * 模板中:
 *   <el-table :data="pagedList" />
 *   <el-pagination
 *     v-model:current-page="page"
 *     v-model:page-size="pageSize"
 *     :total="total"
 *     ... />
 */
export function usePagination<T>(
  source: Ref<T[]> | ComputedRef<T[]>,
  defaultPageSize: number = 10,
) {
  const page = ref(1)
  const pageSize = ref(defaultPageSize)

  const total = computed(() => source.value.length)

  const pagedList = computed<T[]>(() => {
    const start = (page.value - 1) * pageSize.value
    return source.value.slice(start, start + pageSize.value)
  })

  /** 筛选条件变更时调用，重置到第 1 页 */
  function resetPage() {
    page.value = 1
  }

  return { page, pageSize, total, pagedList, resetPage }
}
