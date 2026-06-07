package com.sa.promotion.domain.promotion.service;

import com.sa.promotion.domain.audit.engine.AuditStateEngine;
import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.engine.StateMachineLinkageValidator;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.entity.PromotionSku;
import com.sa.promotion.domain.promotion.engine.PromotionStateEngine;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 促销活动域服务 - 活动生命周期业务逻辑
 * 
 * DDD设计说明：
 * - 协调Promotion实体、状态机引擎和事件总线
 * - 封装活动创建、提交审核、上线、下线等核心业务流程
 * - 确保所有操作都通过事件驱动，产生标准化的事件
 * 
 * 事件驱动架构：
 * - 每个业务操作都会产生Event对象
 * - 通过EventBusService发布事件
 * - 状态机引擎消费事件并执行状态流转
 * 
 * 双状态机联动：
 * - submitAudit()会同时触发活动状态机和审核状态机的联动
 */
@Service
public class PromotionDomainService {
    
    private final PromotionStateEngine promotionStateEngine;
    private final AuditStateEngine auditStateEngine;
    private final StateMachineLinkageValidator linkageValidator;

    public PromotionDomainService(PromotionStateEngine promotionStateEngine,
                                  AuditStateEngine auditStateEngine,
                                  StateMachineLinkageValidator linkageValidator) {
        this.promotionStateEngine = promotionStateEngine;
        this.auditStateEngine = auditStateEngine;
        this.linkageValidator = linkageValidator;
    }
    
    /**
     * 创建活动草稿
     * 
     * @param name 活动名称
     * @param stime 开始时间
     * @param etime 结束时间
     * @param creatorId 创建人ID
     * @return 创建的活动实体
     */
    public Promotion createDraft(String name, LocalDateTime stime, LocalDateTime etime, String creatorId) {
        // 1. 创建Promotion实体
        Promotion promotion = Promotion.builder()
            .promotionId(UUID.randomUUID().toString())
            .name(name)
            .stime(stime)
            .etime(etime)
            .creator(creatorId)
            .operator(creatorId)
            .status(PromotionStatus.DRAFT)
            .auditStatus(AuditStatus.WAITING)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();
        
        // 2. 验证活动时间
        if (!promotion.isTimeValid()) {
            throw new IllegalArgumentException("Invalid time range: stime must be before etime");
        }

        // 3. Event 创建和发布由 Application 层统一处理

        return promotion;
    }
    
    /**
     * 添加SKU到活动
     * 
     * @param promotion 活动实体
     * @param skuId SKU ID
     * @param discount 折扣
     * @param operatorId 操作人ID
     */
    public void addSkuToPromotion(Promotion promotion, String skuId, BigDecimal discount, String operatorId) {
        // 1. 创建PromotionSku实体
        PromotionSku promotionSku = PromotionSku.builder()
            .id(UUID.randomUUID().toString())
            .skuId(skuId)
            .discount(discount)
            .build();
        
        // 2. 通过聚合根添加SKU（内部会验证状态和折扣）
        promotion.addSku(promotionSku);
        
        // 3. 更新操作人和更新时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
    }
    
    /**
     * 从活动中移除SKU
     * 
     * @param promotion 活动实体
     * @param skuId SKU ID
     * @param operatorId 操作人ID
     */
    public void removeSkuFromPromotion(Promotion promotion, String skuId, String operatorId) {
        // 1. 通过聚合根移除SKU
        promotion.removeSku(skuId);
        
        // 2. 更新操作人和更新时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
    }
    
    /**
     * 更新活动SKU折扣
     * 
     * @param promotion 活动实体
     * @param skuId SKU ID
     * @param discount 新折扣
     * @param operatorId 操作人ID
     */
    public void updateSkuDiscount(Promotion promotion, String skuId, BigDecimal discount, String operatorId) {
        // 1. 通过聚合根更新折扣
        promotion.updateSkuDiscount(skuId, discount);
        
        // 2. 更新操作人和更新时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
    }
    
    /**
     * 提交审核
     * 触发双状态机联动：
     * - 活动状态机: DRAFT -> AUDITING
     * - 审核状态机: WAITING -> AUDITING
     * 
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param operatorId 操作人ID
     * @return 产生的事件
     */
    public Event submitAudit(Promotion promotion, AuditRecord auditRecord, String operatorId) {
        // 1. 验证是否可以提交审核
        if (!promotion.canSubmitAudit()) {
            throw new IllegalStateException("Cannot submit audit: " + promotion.getStatus());
        }
        
        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_SUBMIT_AUDIT)) {
            throw new IllegalStateException("Invalid linkage transition for submit audit");
        }
        
        // 3. 保存前置状态（用于事件记录）
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();
        
        // 4. 执行活动状态机转换
        promotionStateEngine.transition(promotion, EventType.E_SUBMIT_AUDIT);

        // 5. 通过审核状态机引擎执行状态转换（统一使用引擎管理状态流转）
        auditStateEngine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);

        // 6. 设置审核元数据
        auditRecord.setSubmitTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 更新操作人和时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
        
        // 8. 产生E_SUBMIT_AUDIT事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_SUBMIT_AUDIT)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();

        // 9. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 活动上线（手动或自动）
     * 状态流转: INIT -> ONLINE
     * 
     * @param promotion 活动实体
     * @param operatorId 操作人ID
     * @return 产生的事件
     */
    public Event goOnline(Promotion promotion, String operatorId) {
        // 1. 验证是否可以上线
        if (!promotion.canGoOnline()) {
            throw new IllegalStateException("Cannot go online: " + promotion.getStatus());
        }
        
        // 2. 保存前置状态
        PromotionStatus prevStatus = promotion.getStatus();
        
        // 3. 执行状态机转换
        promotionStateEngine.transition(promotion, EventType.E_ACTIVE_ONLINE);
        
        // 4. 更新操作人和时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
        
        // 5. 产生E_ACTIVE_ONLINE事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_ACTIVE_ONLINE)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevStatus)
            .prevAuditStatus(promotion.getAuditStatus())
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();
        
        // 6. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 手动下线活动
     * 状态流转: ONLINE -> OFFLINE
     * 
     * @param promotion 活动实体
     * @param operatorId 操作人ID
     * @return 产生的事件
     */
    public Event goOffline(Promotion promotion, String operatorId) {
        // 1. 验证是否可以下线
        if (!promotion.canManualOffline()) {
            throw new IllegalStateException("Cannot go offline: " + promotion.getStatus());
        }
        
        // 2. 保存前置状态
        PromotionStatus prevStatus = promotion.getStatus();
        
        // 3. 执行状态机转换
        promotionStateEngine.transition(promotion, EventType.E_MANUAL_OFFLINE);
        
        // 4. 更新操作人和时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
        
        // 5. 产生E_MANUAL_OFFLINE事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_MANUAL_OFFLINE)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevStatus)
            .prevAuditStatus(promotion.getAuditStatus())
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();
        
        // 6. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 活动过期（定时任务调用）
     * 状态流转: ONLINE -> EXPIRE
     * 
     * @param promotion 活动实体
     * @return 产生的事件
     */
    public Event expire(Promotion promotion) {
        // 1. 验证是否已过期
        if (!promotion.isExpired()) {
            throw new IllegalStateException("Promotion is not expired yet");
        }
        
        // 2. 保存前置状态
        PromotionStatus prevStatus = promotion.getStatus();
        
        // 3. 执行状态机转换
        promotionStateEngine.transition(promotion, EventType.E_ACTIVE_EXPIRE);
        
        // 4. 更新时间
        promotion.setUtime(LocalDateTime.now());
        
        // 5. 产生E_ACTIVE_EXPIRE事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_ACTIVE_EXPIRE)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevStatus)
            .prevAuditStatus(promotion.getAuditStatus())
            .operator("SYSTEM") // 系统自动触发
            .eventTime(LocalDateTime.now())
            .build();
        
        // 6. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 检查并自动激活应该生效的活动（定时任务调用）
     * 
     * @param promotion 活动实体
     * @param operatorId 操作人ID（可以是SYSTEM）
     * @return true-已激活，false-未激活
     */
    public boolean autoActivateIfNeeded(Promotion promotion, String operatorId) {
        if (promotion.shouldAutoActivate()) {
            goOnline(promotion, operatorId);
            return true;
        }
        return false;
    }
}
