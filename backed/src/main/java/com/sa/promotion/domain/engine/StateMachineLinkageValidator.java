package com.sa.promotion.domain.engine;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.springframework.stereotype.Component;

/**
 * 双状态机联动校验器 - 跨域协调服务
 * 
 * DDD设计说明：
 * - 负责活动状态机和审核状态机的联动校验
 * - 确保双状态机在事件驱动下的一致性
 * - 提供跨域的状态转换验证能力
 * 
 * 事件驱动架构：
 * - 监听事件类型，判断是否需要联动更新
 * - 某些事件会同时触发两个状态机的流转
 * - 保证联动操作的原子性和一致性
 * 
 * 双状态机联动规则：
 * 1. E_SUBMIT_AUDIT: 
 *    - 审核状态机: WAITING -> AUDITING
 *    - 活动状态机: DRAFT -> AUDITING
 * 
 * 2. E_AUDIT_PASS:
 *    - 审核状态机: AUDITING -> PASSED (终态)
 *    - 活动状态机: AUDITING -> INIT
 * 
 * 3. E_AUDIT_REJECT:
 *    - 审核状态机: AUDITING -> REJECTED
 *    - 活动状态机: AUDITING -> DRAFT
 * 
 * 4. E_AUDIT_NOTPASS:
 *    - 审核状态机: AUDITING -> NOT_PASSED (终态)
 *    - 活动状态机: AUDITING -> EXPIRE (终态)
 * 
 * 5. E_AUDIT_CANCEL:
 *    - 审核状态机: WAITING/REJECTED -> CANCELLED (终态)
 *    - 活动状态机: DRAFT/AUDITING -> OFFLINE (终态)
 */
@Component
public class StateMachineLinkageValidator {
    
    /**
     * 验证双状态机联动转换是否合法
     * 
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param eventType 触发的事件类型
     * @return true-合法，false-不合法
     */
    public boolean validateLinkageTransition(Promotion promotion, AuditRecord auditRecord, EventType eventType) {
        if (promotion == null || auditRecord == null || eventType == null) {
            return false;
        }
        
        // 检查事件是否需要联动
        if (!requiresLinkage(eventType)) {
            return true; // 不需要联动的事件，单独验证即可
        }
        
        PromotionStatus promotionStatus = promotion.getStatus();
        AuditStatus auditStatus = auditRecord.getAuditStatus();
        
        // 根据事件类型验证联动规则
        switch (eventType) {
            case E_SUBMIT_AUDIT:
                return validateSubmitAudit(promotionStatus, auditStatus);
            
            case E_AUDIT_PASS:
                return validateAuditPass(promotionStatus, auditStatus);
            
            case E_AUDIT_REJECT:
                return validateAuditReject(promotionStatus, auditStatus);
            
            case E_AUDIT_NOTPASS:
                return validateAuditNotPass(promotionStatus, auditStatus);
            
            case E_AUDIT_CANCEL:
                return validateAuditCancel(promotionStatus, auditStatus);
            
            default:
                return false;
        }
    }
    
    /**
     * 验证提交审核的联动合法性
     */
    private boolean validateSubmitAudit(PromotionStatus promotionStatus, AuditStatus auditStatus) {
        // 活动必须是DRAFT状态，审核必须是WAITING状态
        return promotionStatus == PromotionStatus.DRAFT 
            && auditStatus == AuditStatus.WAITING;
    }
    
    /**
     * 验证审核通过的联动合法性
     */
    private boolean validateAuditPass(PromotionStatus promotionStatus, AuditStatus auditStatus) {
        // 活动和审核都必须是AUDITING状态
        return promotionStatus == PromotionStatus.AUDITING 
            && auditStatus == AuditStatus.AUDITING;
    }
    
    /**
     * 验证审核驳回的联动合法性
     */
    private boolean validateAuditReject(PromotionStatus promotionStatus, AuditStatus auditStatus) {
        // 活动和审核都必须是AUDITING状态
        return promotionStatus == PromotionStatus.AUDITING 
            && auditStatus == AuditStatus.AUDITING;
    }
    
    /**
     * 验证审核不通过的联动合法性
     */
    private boolean validateAuditNotPass(PromotionStatus promotionStatus, AuditStatus auditStatus) {
        // 活动和审核都必须是AUDITING状态
        return promotionStatus == PromotionStatus.AUDITING 
            && auditStatus == AuditStatus.AUDITING;
    }
    
    /**
     * 验证审核取消的联动合法性
     */
    private boolean validateAuditCancel(PromotionStatus promotionStatus, AuditStatus auditStatus) {
        // 活动可以是DRAFT或AUDITING，审核可以是WAITING或REJECTED
        boolean promotionValid = promotionStatus == PromotionStatus.DRAFT 
            || promotionStatus == PromotionStatus.AUDITING;
        boolean auditValid = auditStatus == AuditStatus.WAITING 
            || auditStatus == AuditStatus.REJECTED;
        return promotionValid && auditValid;
    }
    
    /**
     * 判断事件是否需要双状态机联动
     * 
     * @param eventType 事件类型
     * @return true-需要联动，false-不需要联动
     */
    public boolean requiresLinkage(EventType eventType) {
        return eventType == EventType.E_SUBMIT_AUDIT
            || eventType == EventType.E_AUDIT_PASS
            || eventType == EventType.E_AUDIT_REJECT
            || eventType == EventType.E_AUDIT_NOTPASS
            || eventType == EventType.E_AUDIT_CANCEL;
    }
    
    /**
     * 获取联动转换的描述信息
     * 
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param eventType 事件类型
     * @return 联动转换描述字符串
     */
    public String getLinkageDescription(Promotion promotion, AuditRecord auditRecord, EventType eventType) {
        if (!validateLinkageTransition(promotion, auditRecord, eventType)) {
            return "Invalid linkage transition";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Linkage Transition: ").append(eventType.getDescription()).append("\n");
        sb.append("  Promotion: ").append(promotion.getStatus().getDescription());
        sb.append(" -> ").append(getTargetPromotionStatus(promotion.getStatus(), eventType).getDescription()).append("\n");
        sb.append("  Audit: ").append(auditRecord.getAuditStatus().getDescription());
        sb.append(" -> ").append(getTargetAuditStatus(auditRecord.getAuditStatus(), eventType).getDescription());
        
        return sb.toString();
    }
    
    /**
     * 获取活动状态机的目标状态
     */
    private PromotionStatus getTargetPromotionStatus(PromotionStatus currentStatus, EventType eventType) {
        switch (eventType) {
            case E_SUBMIT_AUDIT:
                return PromotionStatus.AUDITING;
            case E_AUDIT_PASS:
                return PromotionStatus.INIT;
            case E_AUDIT_REJECT:
                return PromotionStatus.DRAFT;
            case E_AUDIT_NOTPASS:
                return PromotionStatus.EXPIRE;
            case E_AUDIT_CANCEL:
                return PromotionStatus.OFFLINE;
            default:
                return currentStatus;
        }
    }
    
    /**
     * 获取审核状态机的目标状态
     */
    private AuditStatus getTargetAuditStatus(AuditStatus currentStatus, EventType eventType) {
        switch (eventType) {
            case E_SUBMIT_AUDIT:
                return AuditStatus.AUDITING;
            case E_AUDIT_PASS:
                return AuditStatus.PASSED;
            case E_AUDIT_REJECT:
                return AuditStatus.REJECTED;
            case E_AUDIT_NOTPASS:
                return AuditStatus.NOT_PASSED;
            case E_AUDIT_CANCEL:
                return AuditStatus.CANCELLED;
            default:
                return currentStatus;
        }
    }
    
    /**
     * 检查是否存在状态冲突
     * 
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @return true-存在冲突，false-无冲突
     */
    public boolean hasStateConflict(Promotion promotion, AuditRecord auditRecord) {
        if (promotion == null || auditRecord == null) {
            return true;
        }
        
        PromotionStatus promotionStatus = promotion.getStatus();
        AuditStatus auditStatus = auditRecord.getAuditStatus();
        
        // 定义合法的状态组合
        // DRAFT <-> WAITING/REJECTED
        // AUDITING <-> AUDITING
        // INIT <-> PASSED
        // ONLINE/EXPIRE/OFFLINE <-> PASSED/NOT_PASSED/CANCELLED
        
        if (promotionStatus == PromotionStatus.DRAFT) {
            return auditStatus != AuditStatus.WAITING && auditStatus != AuditStatus.REJECTED;
        } else if (promotionStatus == PromotionStatus.AUDITING) {
            return auditStatus != AuditStatus.AUDITING;
        } else if (promotionStatus == PromotionStatus.INIT) {
            return auditStatus != AuditStatus.PASSED;
        } else if (promotionStatus == PromotionStatus.ONLINE 
                || promotionStatus == PromotionStatus.EXPIRE 
                || promotionStatus == PromotionStatus.OFFLINE) {
            // 终态可以对应多种审核终态
            return !auditStatus.isFinalState();
        }
        
        return false;
    }
}
