package com.sa.promotion.domain.promotion.engine;

import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 活动状态机引擎 - 促销活动域
 * 
 * DDD设计说明：
 * - 负责活动状态机的状态流转逻辑
 * - 封装状态转换规则，确保状态流转的合法性
 * - 提供状态校验和转换的核心能力
 * 
 * 事件驱动架构：
 * - 接收EventType事件，根据当前状态决定是否可以流转
 * - 状态流转由事件驱动，每次流转都会产生新的事件
 * - 状态机不直接操作数据库，只负责内存中的状态计算
 * 
 * 状态流转规则（基于设计文档3.1）：
 * - DRAFT + E_SUBMIT_AUDIT -> AUDITING
 * - AUDITING + E_AUDIT_PASS -> INIT
 * - AUDITING + E_AUDIT_REJECT -> DRAFT
 * - AUDITING + E_AUDIT_NOTPASS -> 终态（EXPIRE/OFFLINE）
 * - INIT + E_ACTIVE_ONLINE -> ONLINE
 * - ONLINE + E_ACTIVE_EXPIRE -> EXPIRE
 * - ONLINE + E_MANUAL_OFFLINE -> OFFLINE
 * - DRAFT + E_AUDIT_CANCEL -> 终态
 */
@Component
public class PromotionStateEngine {
    
    /**
     * 状态转换规则表
     * Key: 当前状态, Value: Map<事件类型, 目标状态>
     */
    private static final Map<PromotionStatus, Map<EventType, PromotionStatus>> TRANSITION_RULES = new HashMap<>();
    
    static {
        // 初始化状态转换规则
        
        // DRAFT状态的转换规则
        Map<EventType, PromotionStatus> draftTransitions = new HashMap<>();
        draftTransitions.put(EventType.E_SUBMIT_AUDIT, PromotionStatus.AUDITING);
        draftTransitions.put(EventType.E_AUDIT_CANCEL, PromotionStatus.OFFLINE); // 取消后进入终态
        TRANSITION_RULES.put(PromotionStatus.DRAFT, draftTransitions);
        
        // AUDITING状态的转换规则
        Map<EventType, PromotionStatus> auditingTransitions = new HashMap<>();
        auditingTransitions.put(EventType.E_AUDIT_PASS, PromotionStatus.INIT);
        auditingTransitions.put(EventType.E_AUDIT_REJECT, PromotionStatus.DRAFT);
        auditingTransitions.put(EventType.E_AUDIT_NOTPASS, PromotionStatus.EXPIRE); // 不通过进入终态
        auditingTransitions.put(EventType.E_AUDIT_CANCEL, PromotionStatus.OFFLINE); // 取消后进入终态
        TRANSITION_RULES.put(PromotionStatus.AUDITING, auditingTransitions);
        
        // INIT状态的转换规则
        Map<EventType, PromotionStatus> initTransitions = new HashMap<>();
        initTransitions.put(EventType.E_ACTIVE_ONLINE, PromotionStatus.ONLINE);
        TRANSITION_RULES.put(PromotionStatus.INIT, initTransitions);
        
        // ONLINE状态的转换规则
        Map<EventType, PromotionStatus> onlineTransitions = new HashMap<>();
        onlineTransitions.put(EventType.E_ACTIVE_EXPIRE, PromotionStatus.EXPIRE);
        onlineTransitions.put(EventType.E_MANUAL_OFFLINE, PromotionStatus.OFFLINE);
        TRANSITION_RULES.put(PromotionStatus.ONLINE, onlineTransitions);
        
        // 终态（EXPIRE、OFFLINE）不允许任何转换
        TRANSITION_RULES.put(PromotionStatus.EXPIRE, new HashMap<>());
        TRANSITION_RULES.put(PromotionStatus.OFFLINE, new HashMap<>());
    }
    
    /**
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param eventType 触发的事件类型
     * @return true-合法，false-不合法
     */
    public boolean validateTransition(PromotionStatus currentStatus, EventType eventType) {
        if (currentStatus == null || eventType == null) {
            return false;
        }
        
        // 检查当前状态是否为终态
        if (currentStatus.isFinalState()) {
            return false;
        }
        
        // 查找转换规则
        Map<EventType, PromotionStatus> transitions = TRANSITION_RULES.get(currentStatus);
        if (transitions == null) {
            return false;
        }
        
        // 检查该事件是否允许在当前状态下触发
        return transitions.containsKey(eventType);
    }
    
    /**
     * 执行状态转换
     * 
     * @param promotion 活动实体
     * @param eventType 触发的事件类型
     * @return 转换后的新状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public PromotionStatus transition(Promotion promotion, EventType eventType) {
        if (promotion == null) {
            throw new IllegalArgumentException("Promotion cannot be null");
        }
        
        PromotionStatus currentStatus = promotion.getStatus();
        
        // 验证转换是否合法
        if (!validateTransition(currentStatus, eventType)) {
            throw new IllegalStateException(
                String.format("Invalid state transition: %s + %s", currentStatus, eventType)
            );
        }
        
        // 获取目标状态
        PromotionStatus targetStatus = TRANSITION_RULES.get(currentStatus).get(eventType);
        
        if (targetStatus == null) {
            throw new IllegalStateException(
                String.format("No target state found for: %s + %s", currentStatus, eventType)
            );
        }
        
        // 更新活动状态
        promotion.setStatus(targetStatus);
        
        return targetStatus;
    }
    
    /**
     * 获取指定状态下允许的事件列表
     * 
     * @param status 当前状态
     * @return 允许的事件类型数组
     */
    public EventType[] getAllowedEvents(PromotionStatus status) {
        Map<EventType, PromotionStatus> transitions = TRANSITION_RULES.get(status);
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
    public boolean isFinalState(PromotionStatus status) {
        return status != null && status.isFinalState();
    }
    
    /**
     * 获取状态转换的描述信息
     * 
     * @param currentStatus 当前状态
     * @param eventType 事件类型
     * @return 转换描述字符串
     */
    public String getTransitionDescription(PromotionStatus currentStatus, EventType eventType) {
        if (!validateTransition(currentStatus, eventType)) {
            return "Invalid transition";
        }
        
        PromotionStatus targetStatus = TRANSITION_RULES.get(currentStatus).get(eventType);
        return String.format("%s --[%s]--> %s", 
            currentStatus.getDescription(), 
            eventType.getDescription(), 
            targetStatus.getDescription()
        );
    }
}
