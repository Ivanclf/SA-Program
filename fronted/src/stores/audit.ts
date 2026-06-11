import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  passAuditApi,
  rejectAuditApi,
  notPassAuditApi,
  cancelAuditApi,
  getAuditStatusApi,
} from '@/api/audit'
import { useAuthStore } from '@/stores/auth'
import type { AuditRecord, AuditRequest } from '@/types/audit'
import type { PromotionEvent } from '@/types/event'

export const useAuditStore = defineStore('audit', () => {
  // ---- State ----
  const current = ref<AuditRecord | null>(null)
  const loading = ref(false)
  const actionLoading = ref(false)

  // ---- Actions ----
  type AuditApiFn = (promotionId: string, data: AuditRequest) => Promise<{ data: PromotionEvent }>

  /** 通用审核操作：提取公共 loading + auth + API 调用逻辑 */
  async function auditAction(promotionId: string, comment: string, apiFn: AuditApiFn): Promise<PromotionEvent> {
    const authStore = useAuthStore()
    const data: AuditRequest = { auditorId: authStore.user!.userId, comment }
    actionLoading.value = true
    try {
      const res = await apiFn(promotionId, data)
      return res.data
    } finally {
      actionLoading.value = false
    }
  }

  /** 查询审核状态（按 promotionId） */
  async function fetchStatus(promotionId: string) {
    loading.value = true
    try {
      const res = await getAuditStatusApi(promotionId)
      current.value = res.data
    } finally {
      loading.value = false
    }
  }

  const pass = (id: string, comment: string) => auditAction(id, comment, passAuditApi)
  const reject = (id: string, comment: string) => auditAction(id, comment, rejectAuditApi)
  const notPass = (id: string, comment: string) => auditAction(id, comment, notPassAuditApi)
  const cancel = (id: string, comment: string) => auditAction(id, comment, cancelAuditApi)

  return {
    // state
    current,
    loading,
    actionLoading,
    // actions
    fetchStatus,
    pass,
    reject,
    notPass,
    cancel,
  }
})
