import request from './index'
import type { ApiResponse } from '@/types/api'
import type { Promotion } from '@/types/promotion'
import type { Sku } from '@/types/sku'

/** 客户查询活动详情（含 SKU 折扣） */
export function getCustomerPromotionApi(id: string): Promise<ApiResponse<Promotion>> {
  return request.get(`/api/customer/promotion/${id}`)
}

/** 客户查询 SKU 详情 */
export function getCustomerSkuApi(id: string): Promise<ApiResponse<Sku>> {
  return request.get(`/api/customer/sku/${id}`)
}

/** 客户查询 SKU 列表 */
export function getCustomerSkuListApi(): Promise<ApiResponse<Sku[]>> {
  return request.get('/api/customer/sku')
}
