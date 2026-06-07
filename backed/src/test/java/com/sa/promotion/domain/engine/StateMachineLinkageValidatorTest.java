package com.sa.promotion.domain.engine;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双状态机联动校验器单元测试
 * 
 * 测试覆盖：
 * 1. 所有合法的联动转换
 * 2. 所有非法的联动转换
 * 3. requiresLinkage方法测试
 * 4. hasStateConflict状态冲突检测
 * 5. getLinkageDescription描述信息
 */
@DisplayName("双状态机联动校验器测试")
class StateMachineLinkageValidatorTest {
    
    private StateMachineLinkageValidator validator;
    private Promotion promotion;
    private AuditRecord auditRecord;
    
    @BeforeEach
    void setUp() {
        validator = new StateMachineLinkageValidator();
        promotion = createPromotion(PromotionStatus.DRAFT);
        auditRecord = createAuditRecord(AuditStatus.WAITING);
    }
    
    // ========== E_SUBMIT_AUDIT联动测试 ==========
    
    @Test
    @DisplayName("E_SUBMIT_AUDIT - DRAFT+WAITING合法")
    void testSubmitAuditValid() {
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_SUBMIT_AUDIT));
    }
    
    @Test
    @DisplayName("E_SUBMIT_AUDIT - AUDITING+AUDITING不合法")
    void testSubmitAuditInvalid() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_SUBMIT_AUDIT));
    }
    
    // ========== E_AUDIT_PASS联动测试 ==========
    
    @Test
    @DisplayName("E_AUDIT_PASS - AUDITING+AUDITING合法")
    void testAuditPassValid() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_PASS));
    }
    
    @Test
    @DisplayName("E_AUDIT_PASS - DRAFT+WAITING不合法")
    void testAuditPassInvalid() {
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_PASS));
    }
    
    // ========== E_AUDIT_REJECT联动测试 ==========
    
    @Test
    @DisplayName("E_AUDIT_REJECT - AUDITING+AUDITING合法")
    void testAuditRejectValid() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_REJECT));
    }
    
    @Test
    @DisplayName("E_AUDIT_REJECT - INIT+PASSED不合法")
    void testAuditRejectInvalid() {
        promotion.setStatus(PromotionStatus.INIT);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_REJECT));
    }
    
    // ========== E_AUDIT_NOTPASS联动测试 ==========
    
    @Test
    @DisplayName("E_AUDIT_NOTPASS - AUDITING+AUDITING合法")
    void testAuditNotPassValid() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_NOTPASS));
    }
    
    @Test
    @DisplayName("E_AUDIT_NOTPASS - ONLINE+PASSED不合法")
    void testAuditNotPassInvalid() {
        promotion.setStatus(PromotionStatus.ONLINE);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_NOTPASS));
    }
    
    // ========== E_AUDIT_CANCEL联动测试 ==========
    
    @Test
    @DisplayName("E_AUDIT_CANCEL - DRAFT+WAITING合法")
    void testAuditCancelDraftWaitingValid() {
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("E_AUDIT_CANCEL - AUDITING+REJECTED合法")
    void testAuditCancelAuditingRejectedValid() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("E_AUDIT_CANCEL - DRAFT+REJECTED合法")
    void testAuditCancelDraftRejectedValid() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("E_AUDIT_CANCEL - INIT+PASSED不合法")
    void testAuditCancelInvalid() {
        promotion.setStatus(PromotionStatus.INIT);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_AUDIT_CANCEL));
    }
    
    // ========== 不需要联动的事件测试 ==========
    
    @Test
    @DisplayName("E_CREATE_DRAFT - 不需要联动，返回true")
    void testCreateDraftNoLinkage() {
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_CREATE_DRAFT));
    }
    
    @Test
    @DisplayName("E_ACTIVE_ONLINE - 不需要联动，返回true")
    void testActiveOnlineNoLinkage() {
        promotion.setStatus(PromotionStatus.INIT);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertTrue(validator.validateLinkageTransition(promotion, auditRecord, EventType.E_ACTIVE_ONLINE));
    }
    
    // ========== null参数测试 ==========
    
    @Test
    @DisplayName("validateLinkageTransition - null参数返回false")
    void testValidateLinkageTransitionNullParams() {
        assertFalse(validator.validateLinkageTransition(null, auditRecord, EventType.E_SUBMIT_AUDIT));
        assertFalse(validator.validateLinkageTransition(promotion, null, EventType.E_SUBMIT_AUDIT));
        assertFalse(validator.validateLinkageTransition(promotion, auditRecord, null));
        assertFalse(validator.validateLinkageTransition(null, null, null));
    }
    
    // ========== requiresLinkage测试 ==========
    
    @Test
    @DisplayName("requiresLinkage - 需要联动的事件")
    void testRequiresLinkage() {
        assertTrue(validator.requiresLinkage(EventType.E_SUBMIT_AUDIT));
        assertTrue(validator.requiresLinkage(EventType.E_AUDIT_PASS));
        assertTrue(validator.requiresLinkage(EventType.E_AUDIT_REJECT));
        assertTrue(validator.requiresLinkage(EventType.E_AUDIT_NOTPASS));
        assertTrue(validator.requiresLinkage(EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("requiresLinkage - 不需要联动的事件")
    void testNotRequiresLinkage() {
        assertFalse(validator.requiresLinkage(EventType.E_CREATE_DRAFT));
        assertFalse(validator.requiresLinkage(EventType.E_ACTIVE_ONLINE));
        assertFalse(validator.requiresLinkage(EventType.E_ACTIVE_EXPIRE));
        assertFalse(validator.requiresLinkage(EventType.E_MANUAL_OFFLINE));
    }
    
    // ========== hasStateConflict状态冲突检测测试 ==========
    
    @Test
    @DisplayName("hasStateConflict - DRAFT+WAITING无冲突")
    void testHasStateConflictDraftWaiting() {
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - DRAFT+REJECTED无冲突")
    void testHasStateConflictDraftRejected() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - DRAFT+AUDITING有冲突")
    void testHasStateConflictDraftAuditing() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - AUDITING+AUDITING无冲突")
    void testHasStateConflictAuditingAuditing() {
        promotion.setStatus(PromotionStatus.AUDITING);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - INIT+PASSED无冲突")
    void testHasStateConflictInitPassed() {
        promotion.setStatus(PromotionStatus.INIT);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - ONLINE+PASSED无冲突")
    void testHasStateConflictOnlinePassed() {
        promotion.setStatus(PromotionStatus.ONLINE);
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - EXPIRE+NOT_PASSED无冲突")
    void testHasStateConflictExpireNotPassed() {
        promotion.setStatus(PromotionStatus.EXPIRE);
        auditRecord.setAuditStatus(AuditStatus.NOT_PASSED);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - OFFLINE+CANCELLED无冲突")
    void testHasStateConflictOfflineCancelled() {
        promotion.setStatus(PromotionStatus.OFFLINE);
        auditRecord.setAuditStatus(AuditStatus.CANCELLED);
        assertFalse(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - ONLINE+AUDITING有冲突")
    void testHasStateConflictOnlineAuditing() {
        promotion.setStatus(PromotionStatus.ONLINE);
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(validator.hasStateConflict(promotion, auditRecord));
    }
    
    @Test
    @DisplayName("hasStateConflict - null参数返回true")
    void testHasStateConflictNullParams() {
        assertTrue(validator.hasStateConflict(null, auditRecord));
        assertTrue(validator.hasStateConflict(promotion, null));
        assertTrue(validator.hasStateConflict(null, null));
    }
    
    // ========== getLinkageDescription测试 ==========
    
    @Test
    @DisplayName("getLinkageDescription - 获取合法联动描述")
    void testGetLinkageDescriptionValid() {
        String desc = validator.getLinkageDescription(promotion, auditRecord, EventType.E_SUBMIT_AUDIT);
        assertTrue(desc.contains("提交审核"));
        assertTrue(desc.contains("草稿"));
        assertTrue(desc.contains("等待审核"));
    }
    
    @Test
    @DisplayName("getLinkageDescription - 非法联动返回Invalid")
    void testGetLinkageDescriptionInvalid() {
        String desc = validator.getLinkageDescription(promotion, auditRecord, EventType.E_AUDIT_PASS);
        assertEquals("Invalid linkage transition", desc);
    }
    
    // ========== 辅助方法 ==========
    
    private Promotion createPromotion(PromotionStatus status) {
        return Promotion.builder()
            .promotionId("test-promotion-id")
            .name("Test Promotion")
            .stime(LocalDateTime.now())
            .etime(LocalDateTime.now().plusDays(7))
            .creator("test-user")
            .operator("test-user")
            .status(status)
            .auditStatus(AuditStatus.WAITING)
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
