package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.repository.AuditRecordRepository;
import com.sa.promotion.domain.audit.service.AuditDomainService;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.service.EventBusService;
import com.sa.promotion.domain.event.service.EventLogService;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.entity.PromotionSku;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.promotion.service.PromotionDomainService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动应用服务 - APP-002
 *
 * 职责：
 * - 协调活动域服务、审核域服务、仓储和事件总线
 * - 封装"创建活动→添加SKU→提交审核"的完整业务流程
 * - 管理事务边界（当前为内存实现，后续接入数据库后添加@Transactional）
 */
@Service
public class PromotionAppService {

    private final PromotionDomainService promotionDomainService;
    private final AuditDomainService auditDomainService;
    private final PromotionRepository promotionRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final EventBusService eventBusService;
    private final EventLogService eventLogService;

    public PromotionAppService(PromotionDomainService promotionDomainService,
                               AuditDomainService auditDomainService,
                               PromotionRepository promotionRepository,
                               AuditRecordRepository auditRecordRepository,
                               EventBusService eventBusService,
                               EventLogService eventLogService) {
        this.promotionDomainService = promotionDomainService;
        this.auditDomainService = auditDomainService;
        this.promotionRepository = promotionRepository;
        this.auditRecordRepository = auditRecordRepository;
        this.eventBusService = eventBusService;
        this.eventLogService = eventLogService;
    }

    /**
     * 创建活动草稿
     */
    public Promotion createPromotion(String name, LocalDateTime stime, LocalDateTime etime, String creatorId) {
        // 1. 通过域服务创建活动草稿
        Promotion promotion = promotionDomainService.createDraft(name, stime, etime, creatorId);

        // 2. 创建关联的审核记录
        AuditRecord auditRecord = auditDomainService.createAuditRecord(promotion.getPromotionId());

        // 3. 持久化
        promotionRepository.save(promotion);
        auditRecordRepository.save(auditRecord);

        // 4. 发布事件
        Event event = buildEvent(promotion, null, null, creatorId,
            com.sa.promotion.domain.event.enums.EventType.E_CREATE_DRAFT);
        publishAndRecord(event);

        return promotion;
    }

    /**
     * 更新活动信息（仅草稿状态可更新）
     */
    public Promotion updatePromotion(String promotionId, String name, LocalDateTime stime,
                                      LocalDateTime etime, String operatorId) {
        Promotion promotion = getPromotion(promotionId);

        com.sa.promotion.domain.promotion.enums.PromotionStatus prevStatus = promotion.getStatus();
        com.sa.promotion.domain.audit.enums.AuditStatus prevAuditStatus = promotion.getAuditStatus();

        if (name != null && !name.trim().isEmpty()) {
            promotion.setName(name.trim());
        }
        if (stime != null) {
            promotion.setStime(stime);
        }
        if (etime != null) {
            promotion.setEtime(etime);
        }
        if (!promotion.isTimeValid()) {
            throw new IllegalArgumentException("Invalid time range: stime must be before etime");
        }

        promotion.setOperator(operatorId);
        promotion.setUtime(LocalDateTime.now());
        promotionRepository.update(promotion);

        Event event = buildEvent(promotion, prevStatus, prevAuditStatus, operatorId,
            com.sa.promotion.domain.event.enums.EventType.E_UPDATE_ACTIVITY);
        publishAndRecord(event);

        return promotion;
    }

    /**
     * 删除活动（仅草稿状态可删除）
     */
    public void deletePromotion(String promotionId, String operatorId) {
        Promotion promotion = getPromotion(promotionId);

        if (promotion.getStatus() != com.sa.promotion.domain.promotion.enums.PromotionStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT promotions can be deleted");
        }

        Event event = buildEvent(promotion, promotion.getStatus(), promotion.getAuditStatus(), operatorId,
            com.sa.promotion.domain.event.enums.EventType.E_DELETE_ACTIVITY);
        publishAndRecord(event);

        promotionRepository.delete(promotionId);
    }

    /**
     * 添加SKU到活动
     */
    public Promotion addSkuToPromotion(String promotionId, String skuId, BigDecimal discount, String operatorId) {
        Promotion promotion = getPromotion(promotionId);

        promotionDomainService.addSkuToPromotion(promotion, skuId, discount, operatorId);
        promotionRepository.update(promotion);
        syncPromotionSkus(promotion);

        return promotion;
    }

    /**
     * 从活动中移除SKU
     */
    public Promotion removeSkuFromPromotion(String promotionId, String skuId, String operatorId) {
        Promotion promotion = getPromotion(promotionId);

        promotionDomainService.removeSkuFromPromotion(promotion, skuId, operatorId);
        promotionRepository.update(promotion);
        syncPromotionSkus(promotion);

        return promotion;
    }

    /**
     * 提交审核（触发双状态机联动）
     */
    public Event submitAudit(String promotionId, String operatorId) {
        Promotion promotion = getPromotion(promotionId);
        AuditRecord auditRecord = getAuditRecord(promotionId);

        Event event = promotionDomainService.submitAudit(promotion, auditRecord, operatorId);

        promotionRepository.update(promotion);
        auditRecordRepository.update(auditRecord);
        publishAndRecord(event);

        return event;
    }

    /**
     * 手动下线活动
     */
    public Event offline(String promotionId, String operatorId) {
        Promotion promotion = getPromotion(promotionId);

        Event event = promotionDomainService.goOffline(promotion, operatorId);

        promotionRepository.update(promotion);
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

    private Event buildEvent(Promotion promotion,
                             com.sa.promotion.domain.promotion.enums.PromotionStatus prevPromoStatus,
                             com.sa.promotion.domain.audit.enums.AuditStatus prevAuditStatus,
                             String operatorId,
                             com.sa.promotion.domain.event.enums.EventType eventType) {
        return Event.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType(eventType)
            .promotionId(promotion.getPromotionId())
            .prevActivityStatus(prevPromoStatus)
            .prevAuditStatus(prevAuditStatus)
            .operator(operatorId)
            .eventTime(LocalDateTime.now())
            .build();
    }

    private void publishAndRecord(Event event) {
        eventBusService.publish(event);
        eventLogService.record(event);
    }

    /**
     * 同步活动的SKU关联记录到 promotion_sku 表
     * 策略：先删后插，保证数据库与内存实体一致
     */
    private void syncPromotionSkus(Promotion promotion) {
        promotionRepository.deletePromotionSkus(promotion.getPromotionId());
        if (promotion.getPromotionSkus() != null) {
            for (PromotionSku sku : promotion.getPromotionSkus()) {
                promotionRepository.insertPromotionSku(sku);
            }
        }
    }
}
