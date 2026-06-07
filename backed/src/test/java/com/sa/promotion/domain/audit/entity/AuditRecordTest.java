package com.sa.promotion.domain.audit.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审核记录实体单元测试
 * 
 * 测试覆盖：
 * 1. 所有状态流转方法（submit、pass、reject、notPass、cancel）
 * 2. 业务规则验证（canSubmit、canAudit、isFinalState）
 * 3. 边界条件和异常处理
 */
@DisplayName("审核记录实体测试")
class AuditRecordTest {
    
    private AuditRecord auditRecord;
    
    @BeforeEach
    void setUp() {
        auditRecord = createAuditRecord(AuditStatus.WAITING);
    }
    
    // ========== canSubmit测试 ==========
    
    @Test
    @DisplayName("canSubmit - WAITING状态返回true")
    void testCanSubmitWaiting() {
        assertTrue(auditRecord.canSubmit());
    }
    
    @Test
    @DisplayName("canSubmit - 非WAITING状态返回false")
    void testCanSubmitNotWaiting() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertFalse(auditRecord.canSubmit());
    }
    
    // ========== canAudit测试 ==========
    
    @Test
    @DisplayName("canAudit - AUDITING状态返回true")
    void testCanAuditAuditing() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertTrue(auditRecord.canAudit());
    }
    
    @Test
    @DisplayName("canAudit - 非AUDITING状态返回false")
    void testCanAuditNotAuditing() {
        assertFalse(auditRecord.canAudit());
    }
    
    // ========== isFinalState测试 ==========
    
    @Test
    @DisplayName("isFinalState - PASSED是终态")
    void testIsFinalStatePassed() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        assertTrue(auditRecord.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - NOT_PASSED是终态")
    void testIsFinalStateNotPassed() {
        auditRecord.setAuditStatus(AuditStatus.NOT_PASSED);
        assertTrue(auditRecord.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - CANCELLED是终态")
    void testIsFinalStateCancelled() {
        auditRecord.setAuditStatus(AuditStatus.CANCELLED);
        assertTrue(auditRecord.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - WAITING不是终态")
    void testIsFinalStateWaiting() {
        assertFalse(auditRecord.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - AUDITING不是终态")
    void testIsFinalStateAuditing() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        assertFalse(auditRecord.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - REJECTED不是终态")
    void testIsFinalStateRejected() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        assertFalse(auditRecord.isFinalState());
    }
    
    // ========== submit方法测试 ==========
    
    @Test
    @DisplayName("submit - WAITING状态成功提交")
    void testSubmitValid() {
        auditRecord.submit("operator-001");
        
        assertEquals(AuditStatus.AUDITING, auditRecord.getAuditStatus());
        assertNotNull(auditRecord.getSubmitTime());
        assertNotNull(auditRecord.getUtime());
    }
    
    @Test
    @DisplayName("submit - 非WAITING状态抛出异常")
    void testSubmitInvalid() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.submit("operator-001");
        });
    }
    
    // ========== pass方法测试 ==========
    
    @Test
    @DisplayName("pass - AUDITING状态成功通过")
    void testPassValid() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        
        auditRecord.pass("auditor-001", "审核通过");
        
        assertEquals(AuditStatus.PASSED, auditRecord.getAuditStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
        assertEquals("审核通过", auditRecord.getComment());
        assertNotNull(auditRecord.getCompleteTime());
        assertNotNull(auditRecord.getUtime());
    }
    
    @Test
    @DisplayName("pass - 非AUDITING状态抛出异常")
    void testPassInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.pass("auditor-001", "审核通过");
        });
    }
    
    // ========== reject方法测试 ==========
    
    @Test
    @DisplayName("reject - AUDITING状态成功驳回")
    void testRejectValid() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        
        auditRecord.reject("auditor-001", "需要修改");
        
        assertEquals(AuditStatus.REJECTED, auditRecord.getAuditStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
        assertEquals("需要修改", auditRecord.getComment());
        assertNotNull(auditRecord.getCompleteTime());
        assertNotNull(auditRecord.getUtime());
    }
    
    @Test
    @DisplayName("reject - 非AUDITING状态抛出异常")
    void testRejectInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.reject("auditor-001", "需要修改");
        });
    }
    
    // ========== notPass方法测试 ==========
    
    @Test
    @DisplayName("notPass - AUDITING状态成功不通过")
    void testNotPassValid() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        
        auditRecord.notPass("auditor-001", "不符合要求");
        
        assertEquals(AuditStatus.NOT_PASSED, auditRecord.getAuditStatus());
        assertEquals("auditor-001", auditRecord.getAuditorId());
        assertEquals("不符合要求", auditRecord.getComment());
        assertNotNull(auditRecord.getCompleteTime());
        assertNotNull(auditRecord.getUtime());
    }
    
    @Test
    @DisplayName("notPass - 非AUDITING状态抛出异常")
    void testNotPassInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.notPass("auditor-001", "不符合要求");
        });
    }
    
    // ========== cancel方法测试 ==========
    
    @Test
    @DisplayName("cancel - WAITING状态成功取消")
    void testCancelFromWaiting() {
        auditRecord.cancel("operator-001", "取消审核");
        
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
        assertEquals("取消审核", auditRecord.getComment());
        assertNotNull(auditRecord.getCompleteTime());
        assertNotNull(auditRecord.getUtime());
    }
    
    @Test
    @DisplayName("cancel - REJECTED状态成功取消")
    void testCancelFromRejected() {
        auditRecord.setAuditStatus(AuditStatus.REJECTED);
        
        auditRecord.cancel("operator-001", "取消审核");
        
        assertEquals(AuditStatus.CANCELLED, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("cancel - AUDITING状态抛出异常")
    void testCancelInvalid() {
        auditRecord.setAuditStatus(AuditStatus.AUDITING);
        
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.cancel("operator-001", "取消审核");
        });
    }
    
    @Test
    @DisplayName("cancel - PASSED状态抛出异常")
    void testCancelFromPassedInvalid() {
        auditRecord.setAuditStatus(AuditStatus.PASSED);
        
        assertThrows(IllegalStateException.class, () -> {
            auditRecord.cancel("operator-001", "取消审核");
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
}
