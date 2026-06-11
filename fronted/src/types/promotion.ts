import type { PromotionStatus, AuditStatus } from '@/utils/enums'

/** 活动-SKU 关联 */
export interface PromotionSku {
  id: string
  promotionId: string
  skuId: string
  discount: number // 0.01 ~ 1.00
}

/** 活动信息 */
export interface Promotion {
  promotionId: string
  name: string
  stime: string // ISO datetime
  etime: string // ISO datetime
  creator: string // 创建人 userId
  operator: string // 最近操作人 userId
  status: PromotionStatus // 活动状态码
  auditStatus: AuditStatus // 审核状态码
  ctime: string
  utime: string
  promotionSkus: PromotionSku[]
}

/** 创建活动请求 */
export interface CreatePromotionRequest {
  name: string
  stime: string
  etime: string
  creatorId: string
}

/** 更新活动请求 */
export interface UpdatePromotionRequest {
  name?: string
  stime?: string
  etime?: string
  operatorId: string
}

/** 活动列表查询参数 */
export interface PromotionListQuery {
  name?: string
  status?: PromotionStatus
  auditStatus?: AuditStatus
  page?: number
  size?: number
}
