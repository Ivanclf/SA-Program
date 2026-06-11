import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  createPromotionApi,
  updatePromotionApi,
  deletePromotionApi,
  submitAuditApi,
  offlinePromotionApi,
  getPromotionListApi,
  getPromotionDetailApi,
} from '@/api/promotion'
import { useAuthStore } from '@/stores/auth'
import { PromotionStatus, AuditStatus } from '@/utils/enums'
import type { Promotion, CreatePromotionRequest, UpdatePromotionRequest } from '@/types/promotion'

export const usePromotionStore = defineStore('promotion', () => {
  // ---- State ----
  const list = ref<Promotion[]>([])
  const current = ref<Promotion | null>(null)
  const loading = ref(false)

  // ---- Getters (基于 current 的操作权限) ----
  /** 是否可编辑：仅草稿状态 */
  const canEdit = computed(() => current.value?.status === PromotionStatus.DRAFT)

  /** 是否可提交审核：草稿 + (等待审核 | 驳回) */
  const canSubmitAudit = computed(() =>
    current.value?.status === PromotionStatus.DRAFT
    && [AuditStatus.WAITING, AuditStatus.REJECTED].includes(current.value.auditStatus)
  )

  /** 是否可手动下线：仅生效中 */
  const canOffline = computed(() => current.value?.status === PromotionStatus.ONLINE)

  /** 是否可删除：仅草稿状态 */
  const canDelete = computed(() => current.value?.status === PromotionStatus.DRAFT)

  // ---- Actions ----
  /** 获取活动列表 */
  async function fetchList() {
    loading.value = true
    try {
      const res = await getPromotionListApi()
      list.value = res.data
    } finally {
      loading.value = false
    }
  }

  /** 获取活动详情 */
  async function fetchDetail(id: string) {
    loading.value = true
    try {
      const res = await getPromotionDetailApi(id)
      current.value = res.data
    } finally {
      loading.value = false
    }
  }

  /** 创建活动草稿 */
  async function create(data: CreatePromotionRequest) {
    const res = await createPromotionApi(data)
    list.value.unshift(res.data)
    current.value = res.data
    return res.data
  }

  /** 更新活动信息 */
  async function update(id: string, data: UpdatePromotionRequest) {
    const res = await updatePromotionApi(id, data)
    current.value = res.data
    const idx = list.value.findIndex(p => p.promotionId === id)
    if (idx !== -1) list.value[idx] = res.data
    return res.data
  }

  /** 删除活动（仅草稿状态） */
  async function deletePromotion(id: string) {
    const authStore = useAuthStore()
    await deletePromotionApi(id, authStore.user!.userId)
    list.value = list.value.filter(p => p.promotionId !== id)
    if (current.value?.promotionId === id) current.value = null
  }

  /** 提交后同步：刷新详情并更新列表中对应项 */
  async function syncAfterAction(id: string) {
    await fetchDetail(id)
    const idx = list.value.findIndex(p => p.promotionId === id)
    if (idx !== -1 && current.value) list.value[idx] = current.value
  }

  /** 提交审核 */
  async function submitAudit(id: string) {
    const authStore = useAuthStore()
    await submitAuditApi(id, authStore.user!.userId)
    await syncAfterAction(id)
  }

  /** 手动下线 */
  async function offline(id: string) {
    const authStore = useAuthStore()
    await offlinePromotionApi(id, authStore.user!.userId)
    await syncAfterAction(id)
  }

  return {
    // state
    list,
    current,
    loading,
    // getters
    canEdit,
    canSubmitAudit,
    canOffline,
    canDelete,
    // actions
    fetchList,
    fetchDetail,
    create,
    update,
    deletePromotion,
    submitAudit,
    offline,
  }
})
