package com.sa.promotion.domain.audit.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审核状态枚举单元测试
 */
@DisplayName("审核状态枚举测试")
class AuditStatusTest {
    
    @Test
    @DisplayName("fromCode - 有效code返回对应枚举")
    void testFromCodeValid() {
        assertEquals(AuditStatus.WAITING, AuditStatus.fromCode(0));
        assertEquals(AuditStatus.AUDITING, AuditStatus.fromCode(1));
        assertEquals(AuditStatus.PASSED, AuditStatus.fromCode(2));
        assertEquals(AuditStatus.REJECTED, AuditStatus.fromCode(3));
        assertEquals(AuditStatus.NOT_PASSED, AuditStatus.fromCode(4));
        assertEquals(AuditStatus.CANCELLED, AuditStatus.fromCode(5));
    }
    
    @Test
    @DisplayName("fromCode - 无效code抛出异常")
    void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            AuditStatus.fromCode(99);
        });
    }
    
    @Test
    @DisplayName("isFinalState - PASSED是终态")
    void testIsFinalStatePassed() {
        assertTrue(AuditStatus.PASSED.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - NOT_PASSED是终态")
    void testIsFinalStateNotPassed() {
        assertTrue(AuditStatus.NOT_PASSED.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - CANCELLED是终态")
    void testIsFinalStateCancelled() {
        assertTrue(AuditStatus.CANCELLED.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - WAITING不是终态")
    void testIsFinalStateWaiting() {
        assertFalse(AuditStatus.WAITING.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - AUDITING不是终态")
    void testIsFinalStateAuditing() {
        assertFalse(AuditStatus.AUDITING.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - REJECTED不是终态")
    void testIsFinalStateRejected() {
        assertFalse(AuditStatus.REJECTED.isFinalState());
    }
    
    @Test
    @DisplayName("getCode - 返回正确的code")
    void testGetCode() {
        assertEquals(0, AuditStatus.WAITING.getCode());
        assertEquals(1, AuditStatus.AUDITING.getCode());
        assertEquals(2, AuditStatus.PASSED.getCode());
        assertEquals(3, AuditStatus.REJECTED.getCode());
        assertEquals(4, AuditStatus.NOT_PASSED.getCode());
        assertEquals(5, AuditStatus.CANCELLED.getCode());
    }
    
    @Test
    @DisplayName("getDescription - 返回正确的描述")
    void testGetDescription() {
        assertEquals("等待审核", AuditStatus.WAITING.getDescription());
        assertEquals("审核中", AuditStatus.AUDITING.getDescription());
        assertEquals("审核通过", AuditStatus.PASSED.getDescription());
        assertEquals("审核驳回", AuditStatus.REJECTED.getDescription());
        assertEquals("审核不通过", AuditStatus.NOT_PASSED.getDescription());
        assertEquals("审核拟作废", AuditStatus.CANCELLED.getDescription());
    }
}
