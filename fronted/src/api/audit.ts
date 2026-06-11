import request from './index'
import type { ApiResponse } from '@/types/api'
import type { AuditRecord, AuditRequest } from '@/types/audit'
import type { PromotionEvent } from '@/types/event'

/** 审核通过 */
export function passAuditApi(promotionId: string, data: AuditRequest): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/audit/pass/${promotionId}`, data)
}

/** 审核驳回（可重新提交） */
export function rejectAuditApi(promotionId: string, data: AuditRequest): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/audit/reject/${promotionId}`, data)
}

/** 审核不通过（终态） */
export function notPassAuditApi(promotionId: string, data: AuditRequest): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/audit/notpass/${promotionId}`, data)
}

/** 审核作废 */
export function cancelAuditApi(promotionId: string, data: AuditRequest): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/audit/cancel/${promotionId}`, data)
}

/** 查询审核状态 */
export function getAuditStatusApi(promotionId: string): Promise<ApiResponse<AuditRecord>> {
  return request.get(`/api/audit/status/${promotionId}`)
}
