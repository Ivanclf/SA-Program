package com.sa.promotion.domain.audit.engine;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 审核状态机引擎 - 审核流程域
 * 
 * DDD设计说明：
 * - 负责审核状态机的状态流转逻辑
 * - 封装审核流程的状态转换规则
 * - 独立于活动状态机运行，但通过事件联动
 * 
 * 事件驱动架构：
 * - 接收审核相关事件（E_AUDIT_PASS、E_AUDIT_REJECT等）
 * - 状态流转由事件驱动，每次流转产生审核事件
 * - 某些审核事件会触发活动状态机的联动更新
 * 
 * 状态流转规则（基于设计文档3.2）：
 * - WAITING + E_SUBMIT_AUDIT -> AUDITING
 * - AUDITING + E_AUDIT_PASS -> PASSED (终态)
 * - AUDITING + E_AUDIT_REJECT -> REJECTED
 * - AUDITING + E_AUDIT_NOTPASS -> NOT_PASSED (终态)
 * - WAITING + E_AUDIT_CANCEL -> CANCELLED (终态)
 * - REJECTED + E_AUDIT_CANCEL -> CANCELLED (终态)
 */
@Component
public class AuditStateEngine {
    
    /**
     * 状态转换规则表
     * Key: 当前状态, Value: Map<事件类型, 目标状态>
     */
    private static final Map<AuditStatus, Map<EventType, AuditStatus>> TRANSITION_RULES = new HashMap<>();
    
    static {
        // 初始化状态转换规则
        
        // WAITING状态的转换规则
        Map<EventType, AuditStatus> waitingTransitions = new HashMap<>();
        waitingTransitions.put(EventType.E_SUBMIT_AUDIT, AuditStatus.AUDITING);
        waitingTransitions.put(EventType.E_AUDIT_CANCEL, AuditStatus.CANCELLED);
        TRANSITION_RULES.put(AuditStatus.WAITING, waitingTransitions);
        
        // AUDITING状态的转换规则
        Map<EventType, AuditStatus> auditingTransitions = new HashMap<>();
        auditingTransitions.put(EventType.E_AUDIT_PASS, AuditStatus.PASSED);
        auditingTransitions.put(EventType.E_AUDIT_REJECT, AuditStatus.REJECTED);
        auditingTransitions.put(EventType.E_AUDIT_NOTPASS, AuditStatus.NOT_PASSED);
        auditingTransitions.put(EventType.E_AUDIT_CANCEL, AuditStatus.CANCELLED);
        TRANSITION_RULES.put(AuditStatus.AUDITING, auditingTransitions);
        
        // REJECTED状态的转换规则（可以重新提交或取消）
        Map<EventType, AuditStatus> rejectedTransitions = new HashMap<>();
        rejectedTransitions.put(EventType.E_SUBMIT_AUDIT, AuditStatus.AUDITING); // 重新提交
        rejectedTransitions.put(EventType.E_AUDIT_CANCEL, AuditStatus.CANCELLED);
        TRANSITION_RULES.put(AuditStatus.REJECTED, rejectedTransitions);
        
        // 终态（PASSED、NOT_PASSED、CANCELLED）不允许任何转换
        TRANSITION_RULES.put(AuditStatus.PASSED, new HashMap<>());
        TRANSITION_RULES.put(AuditStatus.NOT_PASSED, new HashMap<>());
        TRANSITION_RULES.put(AuditStatus.CANCELLED, new HashMap<>());
    }
    
    /**
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param eventType 触发的事件类型
     * @return true-合法，false-不合法
     */
    public boolean validateTransition(AuditStatus currentStatus, EventType eventType) {
        if (currentStatus == null || eventType == null) {
            return false;
        }
        
        // 检查当前状态是否为终态
        if (currentStatus.isFinalState()) {
            return false;
        }
        
        // 查找转换规则
        Map<EventType, AuditStatus> transitions = TRANSITION_RULES.get(currentStatus);
        if (transitions == null) {
            return false;
        }
        
        // 检查该事件是否允许在当前状态下触发
        return transitions.containsKey(eventType);
    }
    
    /**
     * 执行状态转换
     * 
     * @param auditRecord 审核记录实体
     * @param eventType 触发的事件类型
     * @return 转换后的新状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public AuditStatus transition(AuditRecord auditRecord, EventType eventType) {
        if (auditRecord == null) {
            throw new IllegalArgumentException("AuditRecord cannot be null");
        }
        
        AuditStatus currentStatus = auditRecord.getAuditStatus();
        
        // 验证转换是否合法
        if (!validateTransition(currentStatus, eventType)) {
            throw new IllegalStateException(
                String.format("Invalid audit state transition: %s + %s", currentStatus, eventType)
            );
        }
        
        // 获取目标状态
        AuditStatus targetStatus = TRANSITION_RULES.get(currentStatus).get(eventType);
        
        if (targetStatus == null) {
            throw new IllegalStateException(
                String.format("No target state found for: %s + %s", currentStatus, eventType)
            );
        }
        
        // 更新审核记录状态
        auditRecord.setAuditStatus(targetStatus);
        
        return targetStatus;
    }
    
    /**
     * 获取指定状态下允许的事件列表
     * 
     * @param status 当前状态
     * @return 允许的事件类型数组
     */
    public EventType[] getAllowedEvents(AuditStatus status) {
        Map<EventType, AuditStatus> transitions = TRANSITION_RULES.get(status);
        if (transitions == null || transitions.isEmpty()) {
            return new EventType[0];
        }
        return transitions.keySet().toArray(new EventType[0]);
    }
    
    /**
     * 判断当前状态是否为终态
     * 
     * @param status 状态
     * @return true-终态，false-非终态
     */
    public boolean isFinalState(AuditStatus status) {
        return status != null && status.isFinalState();
    }
    
    /**
     * 获取状态转换的描述信息
     * 
     * @param currentStatus 当前状态
     * @param eventType 事件类型
     * @return 转换描述字符串
     */
    public String getTransitionDescription(AuditStatus currentStatus, EventType eventType) {
        if (!validateTransition(currentStatus, eventType)) {
            return "Invalid transition";
        }
        
        AuditStatus targetStatus = TRANSITION_RULES.get(currentStatus).get(eventType);
        return String.format("%s --[%s]--> %s", 
            currentStatus.getDescription(), 
            eventType.getDescription(), 
            targetStatus.getDescription()
        );
    }
    
    /**
     * 判断审核事件是否需要联动更新活动状态
     * 
     * @param eventType 事件类型
     * @return true-需要联动，false-不需要联动
     */
    public boolean requiresPromotionLinkage(EventType eventType) {
        return eventType == EventType.E_AUDIT_PASS      // 审核通过 -> 活动变为INIT
            || eventType == EventType.E_AUDIT_REJECT    // 审核驳回 -> 活动变回DRAFT
            || eventType == EventType.E_AUDIT_NOTPASS   // 审核不通过 -> 活动进入终态
            || eventType == EventType.E_AUDIT_CANCEL;   // 审核作废 -> 活动进入终态
    }
}
