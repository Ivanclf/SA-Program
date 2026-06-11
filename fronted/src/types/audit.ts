import type { AuditStatus } from '@/utils/enums'

/** 审核记录 */
export interface AuditRecord {
  auditId: string
  promotionId: string
  auditStatus: AuditStatus
  submitTime: string // ISO datetime
  completeTime: string // ISO datetime
  auditorId: string
  comment: string
  ctime: string
  utime: string
}

/** 审核请求（通过/驳回/不通过/作废通用） */
export interface AuditRequest {
  auditorId: string
  comment: string
}
