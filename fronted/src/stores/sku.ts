import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createSkuApi,
  updateSkuApi,
  deleteSkuApi,
  getSkuDetailApi,
  getSkuListApi,
} from '@/api/sku'
import type { Sku, CreateSkuRequest, UpdateSkuRequest } from '@/types/sku'

export const useSkuStore = defineStore('sku', () => {
  // ---- State ----
  const list = ref<Sku[]>([])
  const current = ref<Sku | null>(null)
  const loading = ref(false)

  // ---- Actions ----
  /** 获取 SKU 列表 */
  async function fetchList() {
    loading.value = true
    try {
      const res = await getSkuListApi()
      list.value = res.data
    } finally {
      loading.value = false
    }
  }

  /** 获取 SKU 详情 */
  async function fetchDetail(id: string) {
    loading.value = true
    try {
      const res = await getSkuDetailApi(id)
      current.value = res.data
    } finally {
      loading.value = false
    }
  }

  /** 创建 SKU */
  async function create(data: CreateSkuRequest) {
    const res = await createSkuApi(data)
    list.value.unshift(res.data)
    current.value = res.data
    return res.data
  }

  /** 更新 SKU */
  async function update(id: string, data: UpdateSkuRequest) {
    const res = await updateSkuApi(id, data)
    current.value = res.data
    const idx = list.value.findIndex(s => s.skuId === id)
    if (idx !== -1) list.value[idx] = res.data
    return res.data
  }

  /** 删除 SKU */
  async function deleteSku(id: string) {
    await deleteSkuApi(id)
    list.value = list.value.filter(s => s.skuId !== id)
    if (current.value?.skuId === id) current.value = null
  }

  return {
    // state
    list,
    current,
    loading,
    // actions
    fetchList,
    fetchDetail,
    create,
    update,
    deleteSku,
  }
})
