/** 活动状态 */
export enum PromotionStatus {
  DRAFT = 0,     // 草稿
  AUDITING = 1,  // 审核中
  INIT = 2,      // 待生效
  ONLINE = 3,    // 生效中
  EXPIRE = 4,    // 过时
  OFFLINE = 5,   // 下线
}

export const PromotionStatusMap: Record<number, { label: string; color: string }> = {
  [PromotionStatus.DRAFT]:    { label: '草稿',   color: 'info' },
  [PromotionStatus.AUDITING]: { label: '审核中', color: 'warning' },
  [PromotionStatus.INIT]:     { label: '待生效', color: 'primary' },
  [PromotionStatus.ONLINE]:   { label: '生效中', color: 'success' },
  [PromotionStatus.EXPIRE]:   { label: '已过期', color: 'default' },
  [PromotionStatus.OFFLINE]:  { label: '已下线', color: 'danger' },
}

/** 审核状态 */
export enum AuditStatus {
  WAITING = 0,    // 等待审核
  AUDITING = 1,   // 审核中
  PASSED = 2,     // 审核通过
  REJECTED = 3,   // 审核驳回
  NOT_PASSED = 4, // 审核不通过
  CANCELLED = 5,  // 审核拟作废
}

export const AuditStatusMap: Record<number, { label: string; color: string }> = {
  [AuditStatus.WAITING]:    { label: '等待审核',  color: 'info' },
  [AuditStatus.AUDITING]:   { label: '审核中',    color: 'warning' },
  [AuditStatus.PASSED]:     { label: '审核通过',  color: 'success' },
  [AuditStatus.REJECTED]:   { label: '审核驳回',  color: 'danger' },
  [AuditStatus.NOT_PASSED]: { label: '审核不通过', color: 'danger' },
  [AuditStatus.CANCELLED]:  { label: '已作废',    color: 'default' },
}

/** 事件类型 */
export enum EventType {
  E_CREATE_DRAFT = 'E_CREATE_DRAFT',
  E_SUBMIT_AUDIT = 'E_SUBMIT_AUDIT',
  E_AUDIT_PASS = 'E_AUDIT_PASS',
  E_AUDIT_REJECT = 'E_AUDIT_REJECT',
  E_AUDIT_NOTPASS = 'E_AUDIT_NOTPASS',
  E_AUDIT_CANCEL = 'E_AUDIT_CANCEL',
  E_ACTIVE_ONLINE = 'E_ACTIVE_ONLINE',
  E_ACTIVE_EXPIRE = 'E_ACTIVE_EXPIRE',
  E_MANUAL_OFFLINE = 'E_MANUAL_OFFLINE',
  E_UPDATE_ACTIVITY = 'E_UPDATE_ACTIVITY',
  E_DELETE_ACTIVITY = 'E_DELETE_ACTIVITY',
}

export const EventTypeMap: Record<string, string> = {
  [EventType.E_CREATE_DRAFT]: '创建草稿',
  [EventType.E_SUBMIT_AUDIT]: '提交审核',
  [EventType.E_AUDIT_PASS]: '审核通过',
  [EventType.E_AUDIT_REJECT]: '审核驳回',
  [EventType.E_AUDIT_NOTPASS]: '审核不通过',
  [EventType.E_AUDIT_CANCEL]: '审核作废',
  [EventType.E_ACTIVE_ONLINE]: '活动自动生效',
  [EventType.E_ACTIVE_EXPIRE]: '活动过期',
  [EventType.E_MANUAL_OFFLINE]: '手动下线',
  [EventType.E_UPDATE_ACTIVITY]: '更新活动',
  [EventType.E_DELETE_ACTIVITY]: '删除活动',
}
