package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.audit.repository.AuditRecordRepository;
import com.sa.promotion.domain.audit.service.AuditDomainService;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.event.service.EventBusService;
import com.sa.promotion.domain.event.service.EventLogService;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.promotion.service.PromotionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("活动应用服务测试")
class PromotionAppServiceTest {

    private PromotionAppService promotionAppService;

    @Mock private PromotionDomainService promotionDomainService;
    @Mock private AuditDomainService auditDomainService;
    @Mock private PromotionRepository promotionRepository;
    @Mock private AuditRecordRepository auditRecordRepository;
    @Mock private EventBusService eventBusService;
    @Mock private EventLogService eventLogService;

    private Promotion promotion;
    private AuditRecord auditRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promotionAppService = new PromotionAppService(promotionDomainService, auditDomainService,
            promotionRepository, auditRecordRepository, eventBusService, eventLogService);

        promotion = Promotion.builder()
            .promotionId("promo-001")
            .name("测试活动")
            .stime(LocalDateTime.now().plusDays(1))
            .etime(LocalDateTime.now().plusDays(7))
            .creator("u001")
            .operator("u001")
            .status(PromotionStatus.DRAFT)
            .auditStatus(AuditStatus.WAITING)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();

        auditRecord = AuditRecord.builder()
            .auditId("promo-001")
            .promotionId("promo-001")
            .auditStatus(AuditStatus.WAITING)
            .build();
    }

    @Test
    @DisplayName("createPromotion - 成功创建活动草稿")
    void testCreatePromotion() {
        when(promotionDomainService.createDraft(anyString(), any(), any(), anyString())).thenReturn(promotion);
        when(auditDomainService.createAuditRecord(anyString())).thenReturn(auditRecord);

        Promotion result = promotionAppService.createPromotion("测试活动",
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7), "u001");

        assertNotNull(result);
        assertEquals("promo-001", result.getPromotionId());
        verify(promotionRepository).save(any(Promotion.class));
        verify(auditRecordRepository).save(any(AuditRecord.class));
        verify(eventBusService).publish(any(Event.class));
    }

    @Test
    @DisplayName("updatePromotion - 成功更新活动并发布事件")
    void testUpdatePromotion() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

        Promotion result = promotionAppService.updatePromotion("promo-001", "新名称", null, null, "u001");

        assertEquals("新名称", result.getName());
        verify(promotionRepository).update(any(Promotion.class));
        verify(eventBusService).publish(any(Event.class));
        verify(eventLogService).record(any(Event.class));
    }

    @Test
    @DisplayName("deletePromotion - DRAFT状态成功删除并发布事件")
    void testDeletePromotion() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

        assertDoesNotThrow(() -> promotionAppService.deletePromotion("promo-001", "u001"));
        verify(eventBusService).publish(any(Event.class));
        verify(eventLogService).record(any(Event.class));
        verify(promotionRepository).delete("promo-001");
    }

    @Test
    @DisplayName("deletePromotion - 非DRAFT状态抛出异常")
    void testDeletePromotionNotDraft() {
        promotion.setStatus(PromotionStatus.AUDITING);
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

        assertThrows(IllegalStateException.class,
            () -> promotionAppService.deletePromotion("promo-001", "u001"));
    }

    @Test
    @DisplayName("addSkuToPromotion - 成功添加SKU")
    void testAddSkuToPromotion() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        // mock 域服务真正执行 addSku 操作
        doAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            String skuId = inv.getArgument(1);
            BigDecimal discount = inv.getArgument(2);
            p.addSku(PromotionSkuHelper.create(p.getPromotionId(), skuId, discount));
            return null;
        }).when(promotionDomainService).addSkuToPromotion(any(), anyString(), any(), anyString());

        Promotion result = promotionAppService.addSkuToPromotion("promo-001", "sku-001",
            new BigDecimal("0.8"), "u001");

        assertEquals(1, result.getPromotionSkus().size());
        verify(promotionRepository).update(promotion);
    }

    @Test
    @DisplayName("submitAudit - 成功提交审核")
    void testSubmitAudit() {
        promotion.addSku(PromotionSkuHelper.create(promotion.getPromotionId(), "sku-001", new BigDecimal("0.8")));
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        Event event = Event.builder().eventId("evt-001").eventType(EventType.E_SUBMIT_AUDIT)
            .promotionId("promo-001").operator("u001").eventTime(LocalDateTime.now()).build();
        when(promotionDomainService.submitAudit(any(), any(), anyString())).thenReturn(event);

        Event result = promotionAppService.submitAudit("promo-001", "u001");

        assertNotNull(result);
        assertEquals(EventType.E_SUBMIT_AUDIT, result.getEventType());
        verify(promotionRepository).update(promotion);
        verify(auditRecordRepository).update(auditRecord);
    }

    @Test
    @DisplayName("offline - 成功手动下线")
    void testOffline() {
        promotion.setStatus(PromotionStatus.ONLINE);
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

        Event event = Event.builder().eventId("evt-001").eventType(EventType.E_MANUAL_OFFLINE)
            .promotionId("promo-001").operator("u001").eventTime(LocalDateTime.now()).build();
        when(promotionDomainService.goOffline(any(), anyString())).thenReturn(event);

        Event result = promotionAppService.offline("promo-001", "u001");

        assertEquals(EventType.E_MANUAL_OFFLINE, result.getEventType());
        verify(promotionRepository).update(promotion);
    }

    // Helper: create a minimal PromotionSku for testing (avoids cross-package private access)
    private static class PromotionSkuHelper {
        static com.sa.promotion.domain.promotion.entity.PromotionSku create(String promoId, String skuId, BigDecimal discount) {
            return com.sa.promotion.domain.promotion.entity.PromotionSku.builder()
                .id(java.util.UUID.randomUUID().toString())
                .promotionId(promoId)
                .skuId(skuId)
                .discount(discount)
                .build();
        }
    }
}
