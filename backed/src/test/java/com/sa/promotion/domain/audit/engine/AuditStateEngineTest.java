package com.sa.promotion.domain.audit.engine;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审核状态机引擎单元测试
 * 
 * 测试覆盖：
 * 1. 所有合法的状态转换
 * 2. 所有非法的状态转换
 * 3. 终态保护机制
 * 4. REJECTED状态的重新提交能力
 * 5. 边界条件和异常处理
 */
@DisplayName("审核状态机引擎测试")
class AuditStateEngineTest {
    
    private AuditStateEngine engine;
    private AuditRecord auditRecord;
    
    @BeforeEach
    void setUp() {
        engine = new AuditStateEngine();
        auditRecord = createAuditRecord(AuditStatus.WAITING);
    }
    
    // ========== 合法状态转换测试 ==========
    
    @Test
    @DisplayName("WAITING + E_SUBMIT_AUDIT -> AUDITING")
    void testWaitingToAuditing() {
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);
        assertEquals(AuditStatus.AUDITING, newStatus);
        assertEquals(AuditStatus.AUDITING, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_PASS -> PASSED")
    void testAuditingToPassed() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_AUDIT_PASS);
        assertEquals(AuditStatus.PASSED, newStatus);
        assertEquals(AuditStatus.PASSED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_REJECT -> REJECTED")
    void testAuditingToRejected() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_AUDIT_REJECT);
        assertEquals(AuditStatus.REJECTED, newStatus);
        assertEquals(AuditStatus.REJECTED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_NOTPASS -> NOT_PASSED")
    void testAuditingToNotPassed() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_AUDIT_NOTPASS);
        assertEquals(AuditStatus.NOT_PASSED, newStatus);
        assertEquals(AuditStatus.NOT_PASSED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("WAITING + E_AUDIT_CANCEL -> CANCELLED")
    void testWaitingToCancelled() {
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_AUDIT_CANCEL);
        assertEquals(AuditStatus.CANCELLED, newStatus);
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("REJECTED + E_AUDIT_CANCEL -> CANCELLED")
    void testRejectedToCancelled() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_AUDIT_CANCEL);
        assertEquals(AuditStatus.CANCELLED, newStatus);
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("REJECTED + E_SUBMIT_AUDIT -> AUDITING (重新提交)")
    void testRejectedResubmitToAuditing() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        AuditStatus newStatus = engine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);
        assertEquals(AuditStatus.AUDITING, newStatus);
        assertEquals(AuditStatus.AUDITING, auditRecord.getAuditStatus());
    }
    
    // ========== 非法状态转换测试 ==========
    
    @Test
    @DisplayName("WAITING状态下不允许E_AUDIT_PASS事件")
    void testInvalidTransitionFromWaiting() {
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(auditRecord, EventType.E_AUDIT_PASS);
        });
    }
    
    @Test
    @DisplayName("PASSED终态不允许E_SUBMIT_AUDIT事件")
    void testInvalidTransitionFromPassed() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    @Test
    @DisplayName("NOT_PASSED终态不允许任何事件")
    void testInvalidTransitionFromNotPassed() {
        auditRecord.setAuditStatus(AuditStatus.NOT_PASSED);
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    // ========== 终态保护测试 ==========
    
    @Test
    @DisplayName("PASSED终态不允许任何状态转换")
    void testFinalStatePassedNoTransition() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        
        for (EventType eventType : EventType.values()) {
            assertFalse(engine.validateTransition(AuditStatus.PASSED, eventType),
                "PASSED状态不应该允许事件: " + eventType);
        }
        
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(auditRecord, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    @Test
    @DisplayName("NOT_PASSED终态不允许任何状态转换")
    void testFinalStateNotPassedNoTransition() {
        auditRecord.setAuditStatus(AuditStatus.NOT_PASSED);
        
        for (EventType eventType : EventType.values()) {
            assertFalse(engine.validateTransition(AuditStatus.NOT_PASSED, eventType),
                "NOT_PASSED状态不应该允许事件: " + eventType);
        }
    }
    
    @Test
    @DisplayName("CANCELLED终态不允许任何状态转换")
    void testFinalStateCancelledNoTransition() {
        auditRecord.setAuditStatus(AuditStatus.CANCELLED);
        
        for (EventType eventType : EventType.values()) {
            assertFalse(engine.validateTransition(AuditStatus.CANCELLED, eventType),
                "CANCELLED状态不应该允许事件: " + eventType);
        }
    }
    
    // ========== 验证方法测试 ==========
    
    @Test
    @DisplayName("validateTransition - 合法的转换返回true")
    void testValidateTransitionValid() {
        assertTrue(engine.validateTransition(AuditStatus.WAITING, EventType.E_SUBMIT_AUDIT));
        assertTrue(engine.validateTransition(AuditStatus.AUDITING, EventType.E_AUDIT_PASS));
        assertTrue(engine.validateTransition(AuditStatus.REJECTED, EventType.E_SUBMIT_AUDIT));
        assertTrue(engine.validateTransition(AuditStatus.WAITING, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("validateTransition - 非法的转换返回false")
    void testValidateTransitionInvalid() {
        assertFalse(engine.validateTransition(AuditStatus.WAITING, EventType.E_AUDIT_PASS));
        assertFalse(engine.validateTransition(AuditStatus.PASSED, EventType.E_SUBMIT_AUDIT));
        assertFalse(engine.validateTransition(AuditStatus.NOT_PASSED, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("validateTransition - null参数返回false")
    void testValidateTransitionNullParams() {
        assertFalse(engine.validateTransition(null, EventType.E_SUBMIT_AUDIT));
        assertFalse(engine.validateTransition(AuditStatus.WAITING, null));
        assertFalse(engine.validateTransition(null, null));
    }
    
    // ========== getAllowedEvents测试 ==========
    
    @Test
    @DisplayName("getAllowedEvents - WAITING状态允许的事件")
    void testGetAllowedEventsWaiting() {
        EventType[] events = engine.getAllowedEvents(AuditStatus.WAITING);
        assertEquals(2, events.length);
        assertTrue(containsEvent(events, EventType.E_SUBMIT_AUDIT));
        assertTrue(containsEvent(events, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("getAllowedEvents - AUDITING状态允许的事件")
    void testGetAllowedEventsAuditing() {
        EventType[] events = engine.getAllowedEvents(AuditStatus.AUDITING);
        assertEquals(4, events.length);
        assertTrue(containsEvent(events, EventType.E_AUDIT_PASS));
        assertTrue(containsEvent(events, EventType.E_AUDIT_REJECT));
        assertTrue(containsEvent(events, EventType.E_AUDIT_NOTPASS));
        assertTrue(containsEvent(events, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("getAllowedEvents - REJECTED状态允许的事件")
    void testGetAllowedEventsRejected() {
        EventType[] events = engine.getAllowedEvents(AuditStatus.REJECTED);
        assertEquals(2, events.length);
        assertTrue(containsEvent(events, EventType.E_SUBMIT_AUDIT));
        assertTrue(containsEvent(events, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("getAllowedEvents - 终态不允许任何事件")
    void testGetAllowedEventsFinalState() {
        assertEquals(0, engine.getAllowedEvents(AuditStatus.PASSED).length);
        assertEquals(0, engine.getAllowedEvents(AuditStatus.NOT_PASSED).length);
        assertEquals(0, engine.getAllowedEvents(AuditStatus.CANCELLED).length);
    }
    
    // ========== isFinalState测试 ==========
    
    @Test
    @DisplayName("isFinalState - 终态判断")
    void testIsFinalState() {
        assertTrue(engine.isFinalState(AuditStatus.PASSED));
        assertTrue(engine.isFinalState(AuditStatus.NOT_PASSED));
        assertTrue(engine.isFinalState(AuditStatus.CANCELLED));
        assertFalse(engine.isFinalState(AuditStatus.WAITING));
        assertFalse(engine.isFinalState(AuditStatus.AUDITING));
        assertFalse(engine.isFinalState(AuditStatus.REJECTED));
        assertFalse(engine.isFinalState(null));
    }
    
    // ========== getTransitionDescription测试 ==========
    
    @Test
    @DisplayName("getTransitionDescription - 获取转换描述")
    void testGetTransitionDescription() {
        String desc = engine.getTransitionDescription(AuditStatus.WAITING, EventType.E_SUBMIT_AUDIT);
        assertTrue(desc.contains("等待审核"));
        assertTrue(desc.contains("提交审核"));
        assertTrue(desc.contains("审核中"));
    }
    
    @Test
    @DisplayName("getTransitionDescription - 非法转换返回Invalid")
    void testGetTransitionDescriptionInvalid() {
        String desc = engine.getTransitionDescription(AuditStatus.WAITING, EventType.E_AUDIT_PASS);
        assertEquals("Invalid transition", desc);
    }
    
    // ========== requiresPromotionLinkage测试 ==========
    
    @Test
    @DisplayName("requiresPromotionLinkage - 需要联动的事件")
    void testRequiresPromotionLinkage() {
        assertTrue(engine.requiresPromotionLinkage(EventType.E_AUDIT_PASS));
        assertTrue(engine.requiresPromotionLinkage(EventType.E_AUDIT_REJECT));
        assertTrue(engine.requiresPromotionLinkage(EventType.E_AUDIT_NOTPASS));
        assertTrue(engine.requiresPromotionLinkage(EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("requiresPromotionLinkage - 不需要联动的事件")
    void testNotRequiresPromotionLinkage() {
        assertFalse(engine.requiresPromotionLinkage(EventType.E_CREATE_DRAFT));
        assertFalse(engine.requiresPromotionLinkage(EventType.E_SUBMIT_AUDIT));
        assertFalse(engine.requiresPromotionLinkage(EventType.E_ACTIVE_ONLINE));
        assertFalse(engine.requiresPromotionLinkage(EventType.E_ACTIVE_EXPIRE));
        assertFalse(engine.requiresPromotionLinkage(EventType.E_MANUAL_OFFLINE));
    }
    
    // ========== 异常处理测试 ==========
    
    @Test
    @DisplayName("transition - null审核记录抛出异常")
    void testTransitionNullAuditRecord() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine.transition(null, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    // ========== 辅助方法 ==========
    
    private AuditRecord createAuditRecord(AuditStatus status) {
        return AuditRecord.builder()
            .auditId("test-audit-id")
            .promotionId("test-promotion-id")
            .auditStatus(status)
            .build();
    }
    
    private boolean containsEvent(EventType[] events, EventType target) {
        for (EventType event : events) {
            if (event == target) {
                return true;
            }
        }
        return false;
    }
}
