/** SKU 信息 */
export interface Sku {
  skuId: string
  skuName: string
  originalPrice: number
}

/** 创建 SKU 请求 */
export interface CreateSkuRequest {
  skuName: string
  originalPrice: number
}

/** 更新 SKU 请求 */
export interface UpdateSkuRequest {
  skuName?: string
  originalPrice?: number
}
