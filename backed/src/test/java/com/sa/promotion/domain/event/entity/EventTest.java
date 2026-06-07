package com.sa.promotion.domain.event.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件实体单元测试
 * 
 * 测试覆盖：
 * 1. addParam/getParam - 参数管理
 * 2. isPromotionLifecycleEvent - 活动生命周期事件判断
 * 3. isAuditEvent - 审核事件判断
 * 4. requiresLinkageUpdate - 联动更新判断
 */
@DisplayName("事件实体测试")
class EventTest {
    
    private Event event;
    
    @BeforeEach
    void setUp() {
        event = createEvent(EventType.E_CREATE_DRAFT);
    }
    
    // ========== 参数管理测试 ==========
    
    @Test
    @DisplayName("addParam - 成功添加参数")
    void testAddParam() {
        event.addParam("name", "Test Promotion");
        event.addParam("stime", "2024-01-01");
        
        assertEquals("Test Promotion", event.getParam("name"));
        assertEquals("2024-01-01", event.getParam("stime"));
    }
    
    @Test
    @DisplayName("getParam - 不存在的参数返回null")
    void testGetParamNotFound() {
        assertNull(event.getParam("non-existent"));
    }
    
    @Test
    @DisplayName("addParam - null参数Map时自动初始化")
    void testAddParamWithNullMap() {
        Event newEvent = Event.builder().build();
        
        assertDoesNotThrow(() -> {
            newEvent.addParam("key", "value");
        });
        
        assertEquals("value", newEvent.getParam("key"));
    }
    
    // ========== isPromotionLifecycleEvent测试 ==========
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_CREATE_DRAFT是生命周期事件")
    void testIsPromotionLifecycleEventCreateDraft() {
        assertTrue(event.isPromotionLifecycleEvent());
    }
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_SUBMIT_AUDIT是生命周期事件")
    void testIsPromotionLifecycleEventSubmitAudit() {
        event.setEventType(EventType.E_SUBMIT_AUDIT);
        assertTrue(event.isPromotionLifecycleEvent());
    }
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_ACTIVE_ONLINE是生命周期事件")
    void testIsPromotionLifecycleEventActiveOnline() {
        event.setEventType(EventType.E_ACTIVE_ONLINE);
        assertTrue(event.isPromotionLifecycleEvent());
    }
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_ACTIVE_EXPIRE是生命周期事件")
    void testIsPromotionLifecycleEventActiveExpire() {
        event.setEventType(EventType.E_ACTIVE_EXPIRE);
        assertTrue(event.isPromotionLifecycleEvent());
    }
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_MANUAL_OFFLINE是生命周期事件")
    void testIsPromotionLifecycleEventManualOffline() {
        event.setEventType(EventType.E_MANUAL_OFFLINE);
        assertTrue(event.isPromotionLifecycleEvent());
    }
    
    @Test
    @DisplayName("isPromotionLifecycleEvent - E_AUDIT_PASS不是生命周期事件")
    void testIsPromotionLifecycleEventAuditPass() {
        event.setEventType(EventType.E_AUDIT_PASS);
        assertFalse(event.isPromotionLifecycleEvent());
    }
    
    // ========== isAuditEvent测试 ==========
    
    @Test
    @DisplayName("isAuditEvent - E_AUDIT_PASS是审核事件")
    void testIsAuditEventPass() {
        event.setEventType(EventType.E_AUDIT_PASS);
        assertTrue(event.isAuditEvent());
    }
    
    @Test
    @DisplayName("isAuditEvent - E_AUDIT_REJECT是审核事件")
    void testIsAuditEventReject() {
        event.setEventType(EventType.E_AUDIT_REJECT);
        assertTrue(event.isAuditEvent());
    }
    
    @Test
    @DisplayName("isAuditEvent - E_AUDIT_NOTPASS是审核事件")
    void testIsAuditEventNotPass() {
        event.setEventType(EventType.E_AUDIT_NOTPASS);
        assertTrue(event.isAuditEvent());
    }
    
    @Test
    @DisplayName("isAuditEvent - E_AUDIT_CANCEL是审核事件")
    void testIsAuditEventCancel() {
        event.setEventType(EventType.E_AUDIT_CANCEL);
        assertTrue(event.isAuditEvent());
    }
    
    @Test
    @DisplayName("isAuditEvent - E_CREATE_DRAFT不是审核事件")
    void testIsAuditEventCreateDraft() {
        assertFalse(event.isAuditEvent());
    }
    
    // ========== requiresLinkageUpdate测试 ==========
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_AUDIT_PASS需要联动")
    void testRequiresLinkageUpdatePass() {
        event.setEventType(EventType.E_AUDIT_PASS);
        assertTrue(event.requiresLinkageUpdate());
    }
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_AUDIT_REJECT需要联动")
    void testRequiresLinkageUpdateReject() {
        event.setEventType(EventType.E_AUDIT_REJECT);
        assertTrue(event.requiresLinkageUpdate());
    }
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_AUDIT_NOTPASS需要联动")
    void testRequiresLinkageUpdateNotPass() {
        event.setEventType(EventType.E_AUDIT_NOTPASS);
        assertTrue(event.requiresLinkageUpdate());
    }
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_AUDIT_CANCEL需要联动")
    void testRequiresLinkageUpdateCancel() {
        event.setEventType(EventType.E_AUDIT_CANCEL);
        assertTrue(event.requiresLinkageUpdate());
    }
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_CREATE_DRAFT不需要联动")
    void testRequiresLinkageUpdateCreateDraft() {
        assertFalse(event.requiresLinkageUpdate());
    }
    
    @Test
    @DisplayName("requiresLinkageUpdate - E_SUBMIT_AUDIT不需要联动")
    void testRequiresLinkageUpdateSubmitAudit() {
        event.setEventType(EventType.E_SUBMIT_AUDIT);
        assertFalse(event.requiresLinkageUpdate());
    }
    
    // ========== 辅助方法 ==========
    
    private Event createEvent(EventType eventType) {
        return Event.builder()
            .eventId("test-event-id")
            .eventType(eventType)
            .promotionId("test-promotion-id")
            .prevActivityStatus(PromotionStatus.DRAFT)
            .prevAuditStatus(AuditStatus.WAITING)
            .operator("test-user")
            .eventTime(LocalDateTime.now())
            .build();
    }
}
