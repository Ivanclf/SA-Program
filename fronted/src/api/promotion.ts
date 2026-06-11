import request from './index'
import type { ApiResponse } from '@/types/api'
import type { Promotion, CreatePromotionRequest, UpdatePromotionRequest } from '@/types/promotion'
import type { PromotionEvent } from '@/types/event'

/** 创建活动草稿 */
export function createPromotionApi(data: CreatePromotionRequest): Promise<ApiResponse<Promotion>> {
  return request.post('/api/promotion/create', data)
}

/** 更新活动信息 */
export function updatePromotionApi(id: string, data: UpdatePromotionRequest): Promise<ApiResponse<Promotion>> {
  return request.put(`/api/promotion/update/${id}`, data)
}

/** 删除活动（仅草稿状态可删除） */
export function deletePromotionApi(id: string, operatorId: string): Promise<ApiResponse<null>> {
  return request.delete(`/api/promotion/delete/${id}`, { params: { operatorId } })
}

/** 提交审核 */
export function submitAuditApi(id: string, operatorId: string): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/promotion/submit-audit/${id}`, null, { params: { operatorId } })
}

/** 手动下线活动 */
export function offlinePromotionApi(id: string, operatorId: string): Promise<ApiResponse<PromotionEvent>> {
  return request.post(`/api/promotion/offline/${id}`, null, { params: { operatorId } })
}

/** 查询活动列表 */
export function getPromotionListApi(): Promise<ApiResponse<Promotion[]>> {
  return request.get('/api/promotion/list')
}

/** 查询活动详情 */
export function getPromotionDetailApi(id: string): Promise<ApiResponse<Promotion>> {
  return request.get(`/api/promotion/${id}`)
}

/** 查询活动事件时间线 */
export function getPromotionEventsApi(id: string): Promise<ApiResponse<PromotionEvent[]>> {
  return request.get(`/api/promotion/${id}/events`)
}

/** 为活动添加 SKU */
export function addSkuToPromotionApi(
  id: string,
  skuId: string,
  discount: number,
  operatorId: string,
): Promise<ApiResponse<Promotion>> {
  return request.post(`/api/promotion/${id}/sku`, null, {
    params: { skuId, discount, operatorId },
  })
}

/** 从活动移除 SKU */
export function removeSkuFromPromotionApi(
  id: string,
  skuId: string,
  operatorId: string,
): Promise<ApiResponse<Promotion>> {
  return request.delete(`/api/promotion/${id}/sku/${skuId}`, {
    params: { operatorId },
  })
}
