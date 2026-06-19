package com.sa.promotion.domain.audit.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.engine.AuditStateEngine;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.engine.StateMachineLinkageValidator;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.engine.PromotionStateEngine;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审核流程域服务 - 审核业务逻辑
 *
 * DDD设计说明：
 * - 协调AuditRecord实体、审核状态机引擎和事件总线
 * - 封装审核通过、驳回、不通过、作废等核心业务流程
 * - 确保所有审核操作都通过事件驱动
 *
 * 事件驱动架构：
 * - 每个审核操作都会产生Event对象
 * - 通过EventBusService发布事件
 * - 双状态机联动：审核事件会触发活动状态的联动更新
 *
 * 双状态机联动：
 * - E_AUDIT_PASS: 审核通过 -> 活动从AUDITING变为INIT
 * - E_AUDIT_REJECT: 审核驳回 -> 活动从AUDITING变回DRAFT
 * - E_AUDIT_NOTPASS: 审核不通过 -> 活动进入终态EXPIRE
 * - E_AUDIT_CANCEL: 审核作废 -> 活动进入终态OFFLINE
 *
 * 状态机一致性：
 * - 审核状态变更统一通过AuditStateEngine.transition()执行
 * - 不再直接调用实体方法修改状态，确保状态机规则集中管理
 */
@Service
public class AuditDomainService {

    private final AuditStateEngine auditStateEngine;
    private final PromotionStateEngine promotionStateEngine;
    private final StateMachineLinkageValidator linkageValidator;

    public AuditDomainService(AuditStateEngine auditStateEngine,
                              PromotionStateEngine promotionStateEngine,
                              StateMachineLinkageValidator linkageValidator) {
        this.auditStateEngine = auditStateEngine;
        this.promotionStateEngine = promotionStateEngine;
        this.linkageValidator = linkageValidator;
    }

    /**
     * 创建审核记录
     *
     * @param promotionId 活动ID
     * @return 创建的审核记录实体
     */
    public AuditRecord createAuditRecord(String promotionId) {
        return AuditRecord.builder()
            .auditId(promotionId) // 审核记录ID与活动ID相同
            .promotionId(promotionId)
            .auditStatus(AuditStatus.WAITING)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();
    }

    /**
     * 审核通过
     * 双状态机联动：
     * - 审核状态机: AUDITING -> PASSED (终态) [通过AuditStateEngine]
     * - 活动状态机: AUDITING -> INIT [通过PromotionStateEngine]
     *
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param auditorId 审核员ID
     * @param comment 审核意见
     * @return 产生的事件
     */
    public Event pass(Promotion promotion, AuditRecord auditRecord, String auditorId, String comment) {
        // 1. 验证审核记录是否可以执行通过操作
        if (!auditRecord.canAudit()) {
            throw new IllegalStateException("Cannot pass audit: " + auditRecord.getAuditStatus());
        }

        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_PASS)) {
            throw new IllegalStateException("Invalid linkage transition for audit pass");
        }

        // 3. 保存前置状态（用于事件记录）
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();

        // 4. 通过审核状态机引擎执行状态转换（统一使用引擎管理状态流转）
        auditStateEngine.transition(auditRecord, EventType.E_AUDIT_PASS);

        // 5. 同步 promotion.auditStatus ← auditRecord.auditStatus
        promotion.setAuditStatus(auditRecord.getAuditStatus());

        // 6. 设置审核元数据
        auditRecord.setAuditorId(auditorId);
        auditRecord.setComment(comment);
        auditRecord.setCompleteTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 执行活动状态机联动转换
        promotionStateEngine.transition(promotion, EventType.E_AUDIT_PASS);

        // 8. 更新时间
        promotion.setOperator(auditorId);
        promotion.setUtime(LocalDateTime.now());

        // 9. 产生E_AUDIT_PASS事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_AUDIT_PASS)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(auditorId)
            .eventTime(LocalDateTime.now())
            .build();

        event.addParam("comment", comment);

        // 9. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 审核驳回（可重新提交）
     * 双状态机联动：
     * - 审核状态机: AUDITING -> REJECTED [通过AuditStateEngine]
     * - 活动状态机: AUDITING -> DRAFT [通过PromotionStateEngine]
     *
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param auditorId 审核员ID
     * @param comment 驳回原因
     * @return 产生的事件
     */
    public Event reject(Promotion promotion, AuditRecord auditRecord, String auditorId, String comment) {
        // 1. 验证审核记录是否可以执行驳回操作
        if (!auditRecord.canAudit()) {
            throw new IllegalStateException("Cannot reject audit: " + auditRecord.getAuditStatus());
        }

        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_REJECT)) {
            throw new IllegalStateException("Invalid linkage transition for audit reject");
        }

        // 3. 保存前置状态
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();

        // 4. 通过审核状态机引擎执行状态转换
        auditStateEngine.transition(auditRecord, EventType.E_AUDIT_REJECT);

        // 5. 同步 promotion.auditStatus ← auditRecord.auditStatus
        promotion.setAuditStatus(auditRecord.getAuditStatus());

        // 6. 设置审核元数据
        auditRecord.setAuditorId(auditorId);
        auditRecord.setComment(comment);
        auditRecord.setCompleteTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 执行活动状态机联动转换
        promotionStateEngine.transition(promotion, EventType.E_AUDIT_REJECT);

        // 8. 更新时间
        promotion.setOperator(auditorId);
        promotion.setUtime(LocalDateTime.now());

        // 8. 产生E_AUDIT_REJECT事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_AUDIT_REJECT)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(auditorId)
            .eventTime(LocalDateTime.now())
            .build();

        event.addParam("comment", comment);

        // 9. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 审核不通过（终态，不可重新提交）
     * 双状态机联动：
     * - 审核状态机: AUDITING -> NOT_PASSED (终态) [通过AuditStateEngine]
     * - 活动状态机: AUDITING -> EXPIRE (终态) [通过PromotionStateEngine]
     *
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param auditorId 审核员ID
     * @param comment 不通过原因
     * @return 产生的事件
     */
    public Event notPass(Promotion promotion, AuditRecord auditRecord, String auditorId, String comment) {
        // 1. 验证审核记录是否可以执行不通过操作
        if (!auditRecord.canAudit()) {
            throw new IllegalStateException("Cannot notPass audit: " + auditRecord.getAuditStatus());
        }

        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_NOTPASS)) {
            throw new IllegalStateException("Invalid linkage transition for audit notPass");
        }

        // 3. 保存前置状态
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();

        // 4. 通过审核状态机引擎执行状态转换
        auditStateEngine.transition(auditRecord, EventType.E_AUDIT_NOTPASS);

        // 5. 同步 promotion.auditStatus ← auditRecord.auditStatus
        promotion.setAuditStatus(auditRecord.getAuditStatus());

        // 6. 设置审核元数据
        auditRecord.setAuditorId(auditorId);
        auditRecord.setComment(comment);
        auditRecord.setCompleteTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 执行活动状态机联动转换
        promotionStateEngine.transition(promotion, EventType.E_AUDIT_NOTPASS);

        // 8. 更新时间
        promotion.setOperator(auditorId);
        promotion.setUtime(LocalDateTime.now());

        // 8. 产生E_AUDIT_NOTPASS事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_AUDIT_NOTPASS)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(auditorId)
            .eventTime(LocalDateTime.now())
            .build();

        event.addParam("comment", comment);

        // 9. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 审核作废（取消审核）
     * 双状态机联动：
     * - 审核状态机: WAITING/REJECTED -> CANCELLED (终态) [通过AuditStateEngine]
     * - 活动状态机: DRAFT/AUDITING -> OFFLINE (终态) [通过PromotionStateEngine]
     *
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param operatorId 操作人ID
     * @param comment 取消原因
     * @return 产生的事件
     */
    public Event cancel(Promotion promotion, AuditRecord auditRecord, String operatorId, String comment) {
        // 1. 验证审核记录是否可以执行取消操作（只有WAITING和REJECTED可以取消）
        if (auditRecord.getAuditStatus() != AuditStatus.WAITING
            && auditRecord.getAuditStatus() != AuditStatus.REJECTED) {
            throw new IllegalStateException("Cannot cancel audit: " + auditRecord.getAuditStatus());
        }

        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_CANCEL)) {
            throw new IllegalStateException("Invalid linkage transition for audit cancel");
        }

        // 3. 保存前置状态
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();

        // 4. 通过审核状态机引擎执行状态转换
        auditStateEngine.transition(auditRecord, EventType.E_AUDIT_CANCEL);

        // 5. 同步 promotion.auditStatus ← auditRecord.auditStatus
        promotion.setAuditStatus(auditRecord.getAuditStatus());

        // 6. 设置审核元数据
        auditRecord.setComment(comment);
        auditRecord.setCompleteTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 执行活动状态机联动转换
        promotionStateEngine.transition(promotion, EventType.E_AUDIT_CANCEL);

        // 8. 更新时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());

        // 8. 产生E_AUDIT_CANCEL事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_AUDIT_CANCEL)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();

        event.addParam("comment", comment);

        // 9. 事件发布由 Application 层统一处理

        return event;
    }

    /**
     * 重新提交审核（审核驳回后重新提交）
     * 双状态机联动：
     * - 审核状态机: REJECTED -> AUDITING [通过AuditStateEngine]
     * - 活动状态机: DRAFT -> AUDITING [通过PromotionStateEngine]
     *
     * @param promotion 活动实体
     * @param auditRecord 审核记录实体
     * @param operatorId 操作人ID
     * @return 产生的事件
     */
    public Event resubmitAudit(Promotion promotion, AuditRecord auditRecord, String operatorId) {
        // 1. 验证是否可以重新提交
        if (auditRecord.getAuditStatus() != AuditStatus.REJECTED) {
            throw new IllegalStateException("Can only resubmit when status is REJECTED");
        }

        // 2. 验证双状态机联动合法性
        if (!linkageValidator.validateLinkageTransition(promotion, auditRecord, EventType.E_SUBMIT_AUDIT)) {
            throw new IllegalStateException("Invalid linkage transition for resubmit audit");
        }

        // 3. 保存前置状态
        PromotionStatus prevPromotionStatus = promotion.getStatus();
        AuditStatus prevAuditStatus = auditRecord.getAuditStatus();

        // 4. 通过审核状态机引擎执行状态转换 (REJECTED -> AUDITING)
        auditStateEngine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);

        // 5. 同步 promotion.auditStatus ← auditRecord.auditStatus
        promotion.setAuditStatus(auditRecord.getAuditStatus());

        // 6. 设置审核元数据
        auditRecord.setSubmitTime(LocalDateTime.now());
        auditRecord.setUtime(LocalDateTime.now());

        // 7. 执行活动状态机联动转换 (DRAFT -> AUDITING)
        promotionStateEngine.transition(promotion, EventType.E_SUBMIT_AUDIT);

        // 8. 更新活动操作人和时间
        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());

        // 8. 产生E_SUBMIT_AUDIT事件
        Event event = Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.E_SUBMIT_AUDIT)
            .promotionId(auditRecord.getPromotionId())
            .prevActivityStatus(prevPromotionStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();

        // 9. 事件发布由 Application 层统一处理

        return event;
    }
}
