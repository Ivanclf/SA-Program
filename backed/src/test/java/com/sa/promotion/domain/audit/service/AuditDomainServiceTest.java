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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 审核流程域服务单元测试
 *
 * 测试覆盖：
 * 1. createAuditRecord - 创建审核记录
 * 2. pass/reject/notPass/cancel - 审核操作（双状态机联动，通过引擎执行）
 * 3. resubmitAudit - 重新提交审核（双状态机联动）
 */
@DisplayName("审核流程域服务测试")
class AuditDomainServiceTest {

    private AuditDomainService auditDomainService;

    @Mock
    private AuditStateEngine auditStateEngine;

    @Mock
    private PromotionStateEngine promotionStateEngine;

    @Mock
    private StateMachineLinkageValidator linkageValidator;

    private Promotion promotion;
    private AuditRecord auditRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditDomainService = new AuditDomainService(auditStateEngine, promotionStateEngine, linkageValidator);

        promotion = createPromotion(PromotionStatus.AUDITING, AuditStatus.AUDITING);
        auditRecord = createAuditRecord(AuditStatus.AUDITING);

        // 默认mock行为
        when(linkageValidator.validateLinkageTransition(any(), any(), any())).thenReturn(true);

        // Mock: PromotionStateEngine 状态转换
        when(promotionStateEngine.transition(any(), any())).thenAnswer(invocation -> {
            Promotion p = invocation.getArgument(0);
            EventType eventType = invocation.getArgument(1);

            if (eventType == EventType.E_AUDIT_PASS) {
                p.setStatus(PromotionStatus.INIT);
            } else if (eventType == EventType.E_AUDIT_REJECT) {
                p.setStatus(PromotionStatus.DRAFT);
            } else if (eventType == EventType.E_AUDIT_NOTPASS) {
                p.setStatus(PromotionStatus.EXPIRE);
            } else if (eventType == EventType.E_AUDIT_CANCEL) {
                p.setStatus(PromotionStatus.OFFLINE);
            } else if (eventType == EventType.E_SUBMIT_AUDIT) {
                p.setStatus(PromotionStatus.AUDITING);
            }
            return p.getStatus();
        });

        // Mock: AuditStateEngine 状态转换（覆盖所有审核事件类型）
        when(auditStateEngine.transition(any(), any())).thenAnswer(invocation -> {
            AuditRecord record = invocation.getArgument(0);
            EventType eventType = invocation.getArgument(1);

            if (eventType == EventType.E_SUBMIT_AUDIT) {
                record.setAuditStatus(AuditStatus.AUDITING);
            } else if (eventType == EventType.E_AUDIT_PASS) {
                record.setAuditStatus(AuditStatus.PASSED);
            } else if (eventType == EventType.E_AUDIT_REJECT) {
                record.setAuditStatus(AuditStatus.REJECTED);
            } else if (eventType == EventType.E_AUDIT_NOTPASS) {
                record.setAuditStatus(AuditStatus.NOT_PASSED);
            } else if (eventType == EventType.E_AUDIT_CANCEL) {
                record.setAuditStatus(AuditStatus.CANCELLED);
            }
            return record.getAuditStatus();
        });
    }

    // ========== createAuditRecord测试 ==========

    @Test
    @DisplayName("createAuditRecord - 成功创建审核记录")
    void testCreateAuditRecord() {
        AuditRecord record = auditDomainService.createAuditRecord("promotion-001");

        assertNotNull(record);
        assertEquals("promotion-001", record.getAuditId());
        assertEquals("promotion-001", record.getPromotionId());
        assertEquals(AuditStatus.WAITING, record.getAuditStatus());
        assertNotNull(record.getCtime());
    }

    // ========== pass测试 ==========

    @Test
    @DisplayName("pass - 成功审核通过（通过引擎执行状态转换）")
    void testPassValid() {
        Event event = auditDomainService.pass(promotion, auditRecord, "auditor-001", "审核通过");

        assertNotNull(event);
        assertEquals(EventType.E_AUDIT_PASS, event.getEventType());
        assertEquals(AuditStatus.PASSED, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.PASSED, promotion.getAuditStatus());
        assertEquals(PromotionStatus.INIT, promotion.getStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
        assertEquals("审核通过", auditRecord.getComment());
        assertEquals("审核通过", event.getParam("comment"));
        assertNotNull(auditRecord.getCompleteTime());
    }

    @Test
    @DisplayName("pass - 不能审核时抛出异常")
    void testPassCannotAudit() {
        auditRecord.setAuditStatus(AuditStatus.WAITING);

        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.pass(promotion, auditRecord, "auditor-001", "审核通过");
        });
    }

    @Test
    @DisplayName("pass - 联动验证失败抛出异常")
    void testPassLinkageValidationFailed() {
        when(linkageValidator.validateLinkageTransition(any(), any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.pass(promotion, auditRecord, "auditor-001", "审核通过");
        });
    }

    // ========== reject测试 ==========

    @Test
    @DisplayName("reject - 成功审核驳回（通过引擎执行状态转换）")
    void testRejectValid() {
        Event event = auditDomainService.reject(promotion, auditRecord, "auditor-001", "需要修改");

        assertNotNull(event);
        assertEquals(EventType.E_AUDIT_REJECT, event.getEventType());
        assertEquals(AuditStatus.REJECTED, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.REJECTED, promotion.getAuditStatus());
        assertEquals(PromotionStatus.DRAFT, promotion.getStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
    }

    @Test
    @DisplayName("reject - 不能审核时抛出异常")
    void testRejectCannotAudit() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);

        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.reject(promotion, auditRecord, "auditor-001", "需要修改");
        });
    }

    // ========== notPass测试 ==========

    @Test
    @DisplayName("notPass - 成功审核不通过（通过引擎执行状态转换）")
    void testNotPassValid() {
        Event event = auditDomainService.notPass(promotion, auditRecord, "auditor-001", "不符合要求");

        assertNotNull(event);
        assertEquals(EventType.E_AUDIT_NOTPASS, event.getEventType());
        assertEquals(AuditStatus.NOT_PASSED, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.NOT_PASSED, promotion.getAuditStatus());
        assertEquals(PromotionStatus.EXPIRE, promotion.getStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
    }

    @Test
    @DisplayName("notPass - 不能审核时抛出异常")
    void testNotPassCannotAudit() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);

        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.notPass(promotion, auditRecord, "auditor-001", "不符合要求");
        });
    }

    // ========== cancel测试 ==========

    @Test
    @DisplayName("cancel - WAITING状态成功取消（通过引擎执行状态转换）")
    void testCancelFromWaiting() {
        auditRecord.setAuditStatus(AuditStatus.WAITING);
        promotion.setStatus(PromotionStatus.DRAFT);

        Event event = auditDomainService.cancel(promotion, auditRecord, "operator-001", "取消审核");

        assertNotNull(event);
        assertEquals(EventType.E_AUDIT_CANCEL, event.getEventType());
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.CANCELLED, promotion.getAuditStatus());
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
        assertEquals("取消审核", auditRecord.getComment());
    }

    @Test
    @DisplayName("cancel - REJECTED状态成功取消（通过引擎执行状态转换）")
    void testCancelFromRejected() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        promotion.setStatus(PromotionStatus.DRAFT);

        Event event = auditDomainService.cancel(promotion, auditRecord, "operator-001", "取消审核");

        assertNotNull(event);
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.CANCELLED, promotion.getAuditStatus());
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
    }

    @Test
    @DisplayName("cancel - AUDITING状态抛出异常")
    void testCancelInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.cancel(promotion, auditRecord, "operator-001", "取消审核");
        });
    }

    // ========== resubmitAudit测试 ==========

    @Test
    @DisplayName("resubmitAudit - REJECTED状态成功重新提交（双状态机联动）")
    void testResubmitAuditValid() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        promotion.setStatus(PromotionStatus.DRAFT);

        Event event = auditDomainService.resubmitAudit(promotion, auditRecord, "user-001");

        assertNotNull(event);
        assertEquals(EventType.E_SUBMIT_AUDIT, event.getEventType());
        assertEquals(AuditStatus.AUDITING, auditRecord.getAuditStatus());
        assertEquals(AuditStatus.AUDITING, promotion.getAuditStatus());
        assertEquals(PromotionStatus.AUDITING, promotion.getStatus());
        assertNotNull(auditRecord.getSubmitTime());
    }

    @Test
    @DisplayName("resubmitAudit - 非REJECTED状态抛出异常")
    void testResubmitAuditInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.resubmitAudit(promotion, auditRecord, "user-001");
        });
    }

    @Test
    @DisplayName("resubmitAudit - 联动验证失败抛出异常")
    void testResubmitAuditLinkageValidationFailed() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        promotion.setStatus(PromotionStatus.DRAFT);
        when(linkageValidator.validateLinkageTransition(any(), any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> {
            auditDomainService.resubmitAudit(promotion, auditRecord, "user-001");
        });
    }

    // ========== 辅助方法 ==========

    private Promotion createPromotion(PromotionStatus status, AuditStatus auditStatus) {
        return Promotion.builder()
            .promotionId("test-promotion-id")
            .name("Test Promotion")
            .stime(LocalDateTime.now())
            .etime(LocalDateTime.now().plusDays(7))
            .creator("test-user")
            .operator("test-user")
            .status(status)
            .auditStatus(auditStatus)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();
    }

    private AuditRecord createAuditRecord(AuditStatus status) {
        return AuditRecord.builder()
            .auditId("test-audit-id")
            .promotionId("test-promotion-id")
            .auditStatus(status)
            .build();
    }
}
