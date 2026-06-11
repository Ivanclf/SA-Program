import { PromotionStatusMap, AuditStatusMap } from '@/utils/enums'

/**
 * 状态码映射 composable
 * 提供活动状态和审核状态的 label / color 查询
 */
export function useStatusMap() {
  /** 获取活动状态标签 */
  function getPromotionLabel(status: number): string {
    return PromotionStatusMap[status]?.label ?? '未知'
  }

  /** 获取活动状态颜色 */
  function getPromotionColor(status: number): string {
    return PromotionStatusMap[status]?.color ?? 'info'
  }

  /** 获取审核状态标签 */
  function getAuditLabel(status: number): string {
    return AuditStatusMap[status]?.label ?? '未知'
  }

  /** 获取审核状态颜色 */
  function getAuditColor(status: number): string {
    return AuditStatusMap[status]?.color ?? 'info'
  }

  return {
    getPromotionLabel,
    getPromotionColor,
    getAuditLabel,
    getAuditColor,
  }
}
