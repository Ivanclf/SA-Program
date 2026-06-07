package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.repository.AuditRecordRepository;
import com.sa.promotion.domain.audit.service.AuditDomainService;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.service.EventBusService;
import com.sa.promotion.domain.event.service.EventLogService;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import org.springframework.stereotype.Service;

/**
 * 审核应用服务 - APP-003
 *
 * 职责：
 * - 协调审核域服务和活动域服务，处理审核通过/驳回/不通过/作废等用例
 * - 从仓储加载实体，调用域服务执行审核操作，持久化结果，发布事件
 */
@Service
public class AuditAppService {

    private final AuditDomainService auditDomainService;
    private final PromotionRepository promotionRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final EventBusService eventBusService;
    private final EventLogService eventLogService;

    public AuditAppService(AuditDomainService auditDomainService,
                           PromotionRepository promotionRepository,
                           AuditRecordRepository auditRecordRepository,
                           EventBusService eventBusService,
                           EventLogService eventLogService) {
        this.auditDomainService = auditDomainService;
        this.promotionRepository = promotionRepository;
        this.auditRecordRepository = auditRecordRepository;
        this.eventBusService = eventBusService;
        this.eventLogService = eventLogService;
    }

    /**
     * 审核通过
     */
    public Event pass(String promotionId, String auditorId, String comment) {
        Promotion promotion = getPromotion(promotionId);
        AuditRecord auditRecord = getAuditRecord(promotionId);

        Event event = auditDomainService.pass(promotion, auditRecord, auditorId, comment);

        promotionRepository.update(promotion);
        auditRecordRepository.update(auditRecord);
        publishAndRecord(event);

        return event;
    }

    /**
     * 审核驳回（可重新提交）
     */
    public Event reject(String promotionId, String auditorId, String comment) {
        Promotion promotion = getPromotion(promotionId);
        AuditRecord auditRecord = getAuditRecord(promotionId);

        Event event = auditDomainService.reject(promotion, auditRecord, auditorId, comment);

        promotionRepository.update(promotion);
        auditRecordRepository.update(auditRecord);
        publishAndRecord(event);

        return event;
    }

    /**
     * 审核不通过（终态）
     */
    public Event notPass(String promotionId, String auditorId, String comment) {
        Promotion promotion = getPromotion(promotionId);
        AuditRecord auditRecord = getAuditRecord(promotionId);

        Event event = auditDomainService.notPass(promotion, auditRecord, auditorId, comment);

        promotionRepository.update(promotion);
        auditRecordRepository.update(auditRecord);
        publishAndRecord(event);

        return event;
    }

    /**
     * 审核作废
     */
    public Event cancel(String promotionId, String operatorId, String comment) {
        Promotion promotion = getPromotion(promotionId);
        AuditRecord auditRecord = getAuditRecord(promotionId);

        Event event = auditDomainService.cancel(promotion, auditRecord, operatorId, comment);

        promotionRepository.update(promotion);
        auditRecordRepository.update(auditRecord);
        publishAndRecord(event);

        return event;
    }

    // ========== 内部辅助方法 ==========

    private Promotion getPromotion(String promotionId) {
        return promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + promotionId));
    }

    private AuditRecord getAuditRecord(String promotionId) {
        return auditRecordRepository.findByPromotionId(promotionId)
            .orElseThrow(() -> new ResourceNotFoundException("Audit record not found for promotion: " + promotionId));
    }

    private void publishAndRecord(Event event) {
        eventBusService.publish(event);
        eventLogService.record(event);
    }
}
