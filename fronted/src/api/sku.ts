import request from './index'
import type { ApiResponse } from '@/types/api'
import type { Sku, CreateSkuRequest, UpdateSkuRequest } from '@/types/sku'

/** 创建 SKU */
export function createSkuApi(data: CreateSkuRequest): Promise<ApiResponse<Sku>> {
  return request.post('/api/sku/create', data)
}

/** 更新 SKU */
export function updateSkuApi(id: string, data: UpdateSkuRequest): Promise<ApiResponse<Sku>> {
  return request.put(`/api/sku/update/${id}`, data)
}

/** 删除 SKU */
export function deleteSkuApi(id: string): Promise<ApiResponse<null>> {
  return request.delete(`/api/sku/delete/${id}`)
}

/** 查询 SKU 详情 */
export function getSkuDetailApi(id: string): Promise<ApiResponse<Sku>> {
  return request.get(`/api/sku/${id}`)
}

/** 查询 SKU 列表 */
export function getSkuListApi(): Promise<ApiResponse<Sku[]>> {
  return request.get('/api/sku/list')
}
