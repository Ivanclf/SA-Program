/** 活动事件 */
export interface PromotionEvent {
  eventId: string
  eventType: string
  promotionId: string
  prevActivityStatus: number
  prevAuditStatus: number
  operator: string
  eventTime: string
  params: Record<string, any>
}
