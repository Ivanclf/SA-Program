package com.sa.promotion.domain.event.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 事件实体 - 事件总线域核心对象
 * 
 * DDD设计说明：
 * - 作为事件驱动架构的核心，所有业务行为都通过事件来表达
 * - 事件是不可变的，一旦产生就不能修改
 * - 事件包含完整的上下文信息，用于状态机流转和审计追踪
 * 
 * 事件驱动架构：
 * - 事件是唯一的驱动源，所有状态变更都由事件触发
 * - 事件生产者（Domain Service）产生事件
 * - 事件消费者（State Engine）消费事件并执行状态流转
 * - 事件总线负责事件的分发和路由
 * 
 * 双状态机联动：
 * - 事件同时携带活动状态和审核状态的前置信息
 * - 通过事件类型决定哪个状态机需要流转
 * - 某些事件会触发双状态机的联动流转
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    /**
     * 事件唯一标识
     */
    private String eventId;
    
    /**
     * 事件类型
     */
    private EventType eventType;
    
    /**
     * 关联的活动ID
     */
    private String promotionId;
    
    /**
     * 前置活动状态（事件发生前的状态）
     */
    private PromotionStatus prevActivityStatus;
    
    /**
     * 前置审核状态（事件发生前的状态）
     */
    private AuditStatus prevAuditStatus;
    
    /**
     * 操作人用户ID
     */
    private String operator;
    
    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 事件参数（JSON格式存储额外信息）
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
    
    // ========== 业务方法 ==========
    
    /**
     * 添加事件参数
     * 
     * @param key 参数键
     * @param value 参数值
     */
    public void addParam(String key, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
    }
    
    /**
     * 获取事件参数
     * 
     * @param key 参数键
     * @return 参数值
     */
    public Object getParam(String key) {
        return this.params != null ? this.params.get(key) : null;
    }
    
    /**
     * 判断是否为活动生命周期事件
     * 
     * @return true-是，false-否
     */
    public boolean isPromotionLifecycleEvent() {
        return eventType == EventType.E_CREATE_DRAFT 
            || eventType == EventType.E_SUBMIT_AUDIT
            || eventType == EventType.E_ACTIVE_ONLINE
            || eventType == EventType.E_ACTIVE_EXPIRE
            || eventType == EventType.E_MANUAL_OFFLINE;
    }
    
    /**
     * 判断是否为审核事件
     * 
     * @return true-是，false-否
     */
    public boolean isAuditEvent() {
        return eventType == EventType.E_AUDIT_PASS
            || eventType == EventType.E_AUDIT_REJECT
            || eventType == EventType.E_AUDIT_NOTPASS
            || eventType == EventType.E_AUDIT_CANCEL;
    }
    
    /**
     * 判断是否需要联动更新活动状态
     * 某些审核事件会同时触发活动状态流转
     * 
     * @return true-需要联动，false-不需要联动
     */
    public boolean requiresLinkageUpdate() {
        return eventType == EventType.E_AUDIT_PASS      // 审核通过 -> 活动变为INIT
            || eventType == EventType.E_AUDIT_REJECT    // 审核驳回 -> 活动变回DRAFT
            || eventType == EventType.E_AUDIT_NOTPASS   // 审核不通过 -> 活动进入终态
            || eventType == EventType.E_AUDIT_CANCEL;   // 审核作废 -> 活动进入终态
    }
}
