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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("审核应用服务测试")
class AuditAppServiceTest {

    private AuditAppService auditAppService;

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
        auditAppService = new AuditAppService(auditDomainService, promotionRepository,
            auditRecordRepository, eventBusService, eventLogService);

        promotion = Promotion.builder()
            .promotionId("promo-001")
            .name("测试活动")
            .status(PromotionStatus.AUDITING)
            .auditStatus(AuditStatus.AUDITING)
            .creator("u001")
            .operator("u001")
            .stime(LocalDateTime.now().plusDays(1))
            .etime(LocalDateTime.now().plusDays(7))
            .build();

        auditRecord = AuditRecord.builder()
            .auditId("promo-001")
            .promotionId("promo-001")
            .auditStatus(AuditStatus.AUDITING)
            .build();
    }

    @Test
    @DisplayName("pass - 审核通过")
    void testPass() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        Event event = Event.builder().eventId("evt-001").eventType(EventType.E_AUDIT_PASS).build();
        when(auditDomainService.pass(any(), any(), anyString(), anyString())).thenReturn(event);

        Event result = auditAppService.pass("promo-001", "u002", "通过");

        assertNotNull(result);
        assertEquals(EventType.E_AUDIT_PASS, result.getEventType());
        verify(promotionRepository).update(promotion);
        verify(auditRecordRepository).update(auditRecord);
        verify(eventBusService).publish(any(Event.class));
    }

    @Test
    @DisplayName("reject - 审核驳回")
    void testReject() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        Event event = Event.builder().eventId("evt-002").eventType(EventType.E_AUDIT_REJECT).build();
        when(auditDomainService.reject(any(), any(), anyString(), anyString())).thenReturn(event);

        Event result = auditAppService.reject("promo-001", "u002", "需要修改");

        assertEquals(EventType.E_AUDIT_REJECT, result.getEventType());
        verify(promotionRepository).update(promotion);
    }

    @Test
    @DisplayName("notPass - 审核不通过")
    void testNotPass() {
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        Event event = Event.builder().eventId("evt-003").eventType(EventType.E_AUDIT_NOTPASS).build();
        when(auditDomainService.notPass(any(), any(), anyString(), anyString())).thenReturn(event);

        Event result = auditAppService.notPass("promo-001", "u002", "不符合要求");

        assertEquals(EventType.E_AUDIT_NOTPASS, result.getEventType());
    }

    @Test
    @DisplayName("cancel - 审核作废")
    void testCancel() {
        auditRecord.setAuditStatus(AuditStatus.WAITING);
        promotion.setStatus(PromotionStatus.DRAFT);
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        Event event = Event.builder().eventId("evt-004").eventType(EventType.E_AUDIT_CANCEL).build();
        when(auditDomainService.cancel(any(), any(), anyString(), anyString())).thenReturn(event);

        Event result = auditAppService.cancel("promo-001", "u001", "取消原因");

        assertEquals(EventType.E_AUDIT_CANCEL, result.getEventType());
    }

    @Test
    @DisplayName("pass - 活动不存在抛出异常")
    void testPassPromotionNotFound() {
        when(promotionRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(com.sa.promotion.domain.exception.ResourceNotFoundException.class,
            () -> auditAppService.pass("non-existent", "u002", "通过"));
    }
}
