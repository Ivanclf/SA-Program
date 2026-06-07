package com.sa.promotion.domain.promotion.engine;

import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动状态机引擎单元测试
 * 
 * 测试覆盖：
 * 1. 所有合法的状态转换
 * 2. 所有非法的状态转换
 * 3. 终态保护机制
 * 4. 边界条件和异常处理
 */
@DisplayName("活动状态机引擎测试")
class PromotionStateEngineTest {
    
    private PromotionStateEngine engine;
    private Promotion promotion;
    
    @BeforeEach
    void setUp() {
        engine = new PromotionStateEngine();
        promotion = createPromotion(PromotionStatus.DRAFT);
    }
    
    // ========== 合法状态转换测试 ==========
    
    @Test
    @DisplayName("DRAFT + E_SUBMIT_AUDIT -> AUDITING")
    void testDraftToAuditing() {
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_SUBMIT_AUDIT);
        assertEquals(PromotionStatus.AUDITING, newStatus);
        assertEquals(PromotionStatus.AUDITING, promotion.getStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_PASS -> INIT")
    void testAuditingToInit() {
        promotion.setStatus(PromotionStatus.AUDITING);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_AUDIT_PASS);
        assertEquals(PromotionStatus.INIT, newStatus);
        assertEquals(PromotionStatus.INIT, promotion.getStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_REJECT -> DRAFT")
    void testAuditingToDraft() {
        promotion.setStatus(PromotionStatus.AUDITING);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_AUDIT_REJECT);
        assertEquals(PromotionStatus.DRAFT, newStatus);
        assertEquals(PromotionStatus.DRAFT, promotion.getStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_NOTPASS -> EXPIRE")
    void testAuditingToExpire() {
        promotion.setStatus(PromotionStatus.AUDITING);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_AUDIT_NOTPASS);
        assertEquals(PromotionStatus.EXPIRE, newStatus);
        assertEquals(PromotionStatus.EXPIRE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("INIT + E_ACTIVE_ONLINE -> ONLINE")
    void testInitToOnline() {
        promotion.setStatus(PromotionStatus.INIT);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_ACTIVE_ONLINE);
        assertEquals(PromotionStatus.ONLINE, newStatus);
        assertEquals(PromotionStatus.ONLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("ONLINE + E_ACTIVE_EXPIRE -> EXPIRE")
    void testOnlineToExpire() {
        promotion.setStatus(PromotionStatus.ONLINE);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_ACTIVE_EXPIRE);
        assertEquals(PromotionStatus.EXPIRE, newStatus);
        assertEquals(PromotionStatus.EXPIRE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("ONLINE + E_MANUAL_OFFLINE -> OFFLINE")
    void testOnlineToOffline() {
        promotion.setStatus(PromotionStatus.ONLINE);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_MANUAL_OFFLINE);
        assertEquals(PromotionStatus.OFFLINE, newStatus);
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("DRAFT + E_AUDIT_CANCEL -> OFFLINE")
    void testDraftToOffline() {
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_AUDIT_CANCEL);
        assertEquals(PromotionStatus.OFFLINE, newStatus);
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("AUDITING + E_AUDIT_CANCEL -> OFFLINE")
    void testAuditingToOffline() {
        promotion.setStatus(PromotionStatus.AUDITING);
        PromotionStatus newStatus = engine.transition(promotion, EventType.E_AUDIT_CANCEL);
        assertEquals(PromotionStatus.OFFLINE, newStatus);
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
    }
    
    // ========== 非法状态转换测试 ==========
    
    @Test
    @DisplayName("DRAFT状态下不允许E_ACTIVE_ONLINE事件")
    void testInvalidTransitionFromDraft() {
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(promotion, EventType.E_ACTIVE_ONLINE);
        });
    }
    
    @Test
    @DisplayName("INIT状态下不允许E_SUBMIT_AUDIT事件")
    void testInvalidTransitionFromInit() {
        promotion.setStatus(PromotionStatus.INIT);
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(promotion, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    @Test
    @DisplayName("ONLINE状态下不允许E_SUBMIT_AUDIT事件")
    void testInvalidTransitionFromOnline() {
        promotion.setStatus(PromotionStatus.ONLINE);
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(promotion, EventType.E_SUBMIT_AUDIT);
        });
    }
    
    // ========== 终态保护测试 ==========
    
    @Test
    @DisplayName("EXPIRE终态不允许任何状态转换")
    void testFinalStateExpireNoTransition() {
        promotion.setStatus(PromotionStatus.EXPIRE);
        
        for (EventType eventType : EventType.values()) {
            assertFalse(engine.validateTransition(PromotionStatus.EXPIRE, eventType),
                "EXPIRE状态不应该允许事件: " + eventType);
        }
        
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(promotion, EventType.E_ACTIVE_ONLINE);
        });
    }
    
    @Test
    @DisplayName("OFFLINE终态不允许任何状态转换")
    void testFinalStateOfflineNoTransition() {
        promotion.setStatus(PromotionStatus.OFFLINE);
        
        for (EventType eventType : EventType.values()) {
            assertFalse(engine.validateTransition(PromotionStatus.OFFLINE, eventType),
                "OFFLINE状态不应该允许事件: " + eventType);
        }
        
        assertThrows(IllegalStateException.class, () -> {
            engine.transition(promotion, EventType.E_ACTIVE_ONLINE);
        });
    }
    
    // ========== 验证方法测试 ==========
    
    @Test
    @DisplayName("validateTransition - 合法的转换返回true")
    void testValidateTransitionValid() {
        assertTrue(engine.validateTransition(PromotionStatus.DRAFT, EventType.E_SUBMIT_AUDIT));
        assertTrue(engine.validateTransition(PromotionStatus.AUDITING, EventType.E_AUDIT_PASS));
        assertTrue(engine.validateTransition(PromotionStatus.INIT, EventType.E_ACTIVE_ONLINE));
        assertTrue(engine.validateTransition(PromotionStatus.ONLINE, EventType.E_ACTIVE_EXPIRE));
    }
    
    @Test
    @DisplayName("validateTransition - 非法的转换返回false")
    void testValidateTransitionInvalid() {
        assertFalse(engine.validateTransition(PromotionStatus.DRAFT, EventType.E_ACTIVE_ONLINE));
        assertFalse(engine.validateTransition(PromotionStatus.INIT, EventType.E_SUBMIT_AUDIT));
        assertFalse(engine.validateTransition(PromotionStatus.EXPIRE, EventType.E_ACTIVE_ONLINE));
    }
    
    @Test
    @DisplayName("validateTransition - null参数返回false")
    void testValidateTransitionNullParams() {
        assertFalse(engine.validateTransition(null, EventType.E_SUBMIT_AUDIT));
        assertFalse(engine.validateTransition(PromotionStatus.DRAFT, null));
        assertFalse(engine.validateTransition(null, null));
    }
    
    // ========== getAllowedEvents测试 ==========
    
    @Test
    @DisplayName("getAllowedEvents - DRAFT状态允许的事件")
    void testGetAllowedEventsDraft() {
        EventType[] events = engine.getAllowedEvents(PromotionStatus.DRAFT);
        assertEquals(2, events.length);
        assertTrue(containsEvent(events, EventType.E_SUBMIT_AUDIT));
        assertTrue(containsEvent(events, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("getAllowedEvents - AUDITING状态允许的事件")
    void testGetAllowedEventsAuditing() {
        EventType[] events = engine.getAllowedEvents(PromotionStatus.AUDITING);
        assertEquals(4, events.length);
        assertTrue(containsEvent(events, EventType.E_AUDIT_PASS));
        assertTrue(containsEvent(events, EventType.E_AUDIT_REJECT));
        assertTrue(containsEvent(events, EventType.E_AUDIT_NOTPASS));
        assertTrue(containsEvent(events, EventType.E_AUDIT_CANCEL));
    }
    
    @Test
    @DisplayName("getAllowedEvents - INIT状态允许的事件")
    void testGetAllowedEventsInit() {
        EventType[] events = engine.getAllowedEvents(PromotionStatus.INIT);
        assertEquals(1, events.length);
        assertTrue(containsEvent(events, EventType.E_ACTIVE_ONLINE));
    }
    
    @Test
    @DisplayName("getAllowedEvents - ONLINE状态允许的事件")
    void testGetAllowedEventsOnline() {
        EventType[] events = engine.getAllowedEvents(PromotionStatus.ONLINE);
        assertEquals(2, events.length);
        assertTrue(containsEvent(events, EventType.E_ACTIVE_EXPIRE));
        assertTrue(containsEvent(events, EventType.E_MANUAL_OFFLINE));
    }
    
    @Test
    @DisplayName("getAllowedEvents - 终态不允许任何事件")
    void testGetAllowedEventsFinalState() {
        assertEquals(0, engine.getAllowedEvents(PromotionStatus.EXPIRE).length);
        assertEquals(0, engine.getAllowedEvents(PromotionStatus.OFFLINE).length);
    }
    
    // ========== isFinalState测试 ==========
    
    @Test
    @DisplayName("isFinalState - 终态判断")
    void testIsFinalState() {
        assertTrue(engine.isFinalState(PromotionStatus.EXPIRE));
        assertTrue(engine.isFinalState(PromotionStatus.OFFLINE));
        assertFalse(engine.isFinalState(PromotionStatus.DRAFT));
        assertFalse(engine.isFinalState(PromotionStatus.AUDITING));
        assertFalse(engine.isFinalState(PromotionStatus.INIT));
        assertFalse(engine.isFinalState(PromotionStatus.ONLINE));
        assertFalse(engine.isFinalState(null));
    }
    
    // ========== getTransitionDescription测试 ==========
    
    @Test
    @DisplayName("getTransitionDescription - 获取转换描述")
    void testGetTransitionDescription() {
        String desc = engine.getTransitionDescription(PromotionStatus.DRAFT, EventType.E_SUBMIT_AUDIT);
        assertTrue(desc.contains("草稿"));
        assertTrue(desc.contains("提交审核"));
        assertTrue(desc.contains("审核中"));
    }
    
    @Test
    @DisplayName("getTransitionDescription - 非法转换返回Invalid")
    void testGetTransitionDescriptionInvalid() {
        String desc = engine.getTransitionDescription(PromotionStatus.DRAFT, EventType.E_ACTIVE_ONLINE);
        assertEquals("Invalid transition", desc);
    }
    
    // ========== 异常处理测试 ==========
    
    @Test
    @DisplayName("transition - null促销对象抛出异常")
    void testTransitionNullPromotion() {
        assertThrows(IllegalArgumentException.class, () -> {
            engine.transition(null, EventType.E_SUBMIT_AUDIT);
        });
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
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
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
