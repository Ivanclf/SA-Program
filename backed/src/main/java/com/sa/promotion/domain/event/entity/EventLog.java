package com.sa.promotion.domain.event.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 事件日志实体 - 事件持久化对象
 * 
 * DDD设计说明：
 * - 用于事件的持久化存储，支持事件溯源（Event Sourcing）
 * - 记录系统中发生的所有重要事件，形成完整的审计轨迹
 * - 支持事件回放（Replay），用于系统恢复和数据修复
 * 
 * 与Event的区别：
 * - Event是内存中的领域对象，用于事件传递和状态机驱动
 * - EventLog是持久化对象，用于长期存储和查询
 * - EventLog通常由Event转换而来，在事件处理完成后持久化
 * 
 * 事件驱动架构：
 * - 所有事件都必须记录到event_log表
 * - 事件日志是系统的"事实来源"（Source of Truth）
 * - 可以通过事件日志重建任意时刻的系统状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {
    
    /**
     * 事件唯一标识（与Event.eventId相同）
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
     * 事件参数（JSON格式字符串）
     */
    private String params;
    
    // ========== 业务方法 ==========
    
    /**
     * 从Event对象转换为EventLog对象
     * 
     * @param event 事件对象
     * @param paramsJson 参数的JSON字符串表示
     * @return EventLog对象
     */
    public static EventLog fromEvent(Event event, String paramsJson) {
        return EventLog.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .promotionId(event.getPromotionId())
            .prevActivityStatus(event.getPrevActivityStatus())
            .prevAuditStatus(event.getPrevAuditStatus())
            .operator(event.getOperator())
            .eventTime(event.getEventTime())
            .params(paramsJson)
            .build();
    }
    
    /**
     * 判断是否为活动相关事件
     * 
     * @return true-是，false-否
     */
    public boolean isPromotionRelated() {
        return this.promotionId != null && !this.promotionId.isEmpty();
    }
    
    /**
     * 判断是否有状态变更记录
     * 
     * @return true-有，false-无
     */
    public boolean hasStatusChange() {
        return this.prevActivityStatus != null || this.prevAuditStatus != null;
    }
}
