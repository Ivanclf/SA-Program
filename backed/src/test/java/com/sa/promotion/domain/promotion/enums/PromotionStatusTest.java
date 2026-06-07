package com.sa.promotion.domain.promotion.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动状态枚举单元测试
 */
@DisplayName("活动状态枚举测试")
class PromotionStatusTest {
    
    @Test
    @DisplayName("fromCode - 有效code返回对应枚举")
    void testFromCodeValid() {
        assertEquals(PromotionStatus.DRAFT, PromotionStatus.fromCode(0));
        assertEquals(PromotionStatus.AUDITING, PromotionStatus.fromCode(1));
        assertEquals(PromotionStatus.INIT, PromotionStatus.fromCode(2));
        assertEquals(PromotionStatus.ONLINE, PromotionStatus.fromCode(3));
        assertEquals(PromotionStatus.EXPIRE, PromotionStatus.fromCode(4));
        assertEquals(PromotionStatus.OFFLINE, PromotionStatus.fromCode(5));
    }
    
    @Test
    @DisplayName("fromCode - 无效code抛出异常")
    void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            PromotionStatus.fromCode(99);
        });
    }
    
    @Test
    @DisplayName("isFinalState - EXPIRE是终态")
    void testIsFinalStateExpire() {
        assertTrue(PromotionStatus.EXPIRE.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - OFFLINE是终态")
    void testIsFinalStateOffline() {
        assertTrue(PromotionStatus.OFFLINE.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - DRAFT不是终态")
    void testIsFinalStateDraft() {
        assertFalse(PromotionStatus.DRAFT.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - AUDITING不是终态")
    void testIsFinalStateAuditing() {
        assertFalse(PromotionStatus.AUDITING.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - INIT不是终态")
    void testIsFinalStateInit() {
        assertFalse(PromotionStatus.INIT.isFinalState());
    }
    
    @Test
    @DisplayName("isFinalState - ONLINE不是终态")
    void testIsFinalStateOnline() {
        assertFalse(PromotionStatus.ONLINE.isFinalState());
    }
    
    @Test
    @DisplayName("getCode - 返回正确的code")
    void testGetCode() {
        assertEquals(0, PromotionStatus.DRAFT.getCode());
        assertEquals(1, PromotionStatus.AUDITING.getCode());
        assertEquals(2, PromotionStatus.INIT.getCode());
        assertEquals(3, PromotionStatus.ONLINE.getCode());
        assertEquals(4, PromotionStatus.EXPIRE.getCode());
        assertEquals(5, PromotionStatus.OFFLINE.getCode());
    }
    
    @Test
    @DisplayName("getDescription - 返回正确的描述")
    void testGetDescription() {
        assertEquals("草稿", PromotionStatus.DRAFT.getDescription());
        assertEquals("审核中", PromotionStatus.AUDITING.getDescription());
        assertEquals("待生效", PromotionStatus.INIT.getDescription());
        assertEquals("生效中", PromotionStatus.ONLINE.getDescription());
        assertEquals("过时", PromotionStatus.EXPIRE.getDescription());
        assertEquals("下线", PromotionStatus.OFFLINE.getDescription());
    }
}
