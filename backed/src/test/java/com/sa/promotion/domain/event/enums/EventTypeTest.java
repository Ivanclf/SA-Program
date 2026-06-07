package com.sa.promotion.domain.event.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件类型枚举单元测试
 */
@DisplayName("事件类型枚举测试")
class EventTypeTest {
    
    @Test
    @DisplayName("fromCode - 有效code返回对应枚举")
    void testFromCodeValid() {
        assertEquals(EventType.E_CREATE_DRAFT, EventType.fromCode("E_CREATE_DRAFT"));
        assertEquals(EventType.E_SUBMIT_AUDIT, EventType.fromCode("E_SUBMIT_AUDIT"));
        assertEquals(EventType.E_AUDIT_PASS, EventType.fromCode("E_AUDIT_PASS"));
        assertEquals(EventType.E_AUDIT_REJECT, EventType.fromCode("E_AUDIT_REJECT"));
        assertEquals(EventType.E_AUDIT_NOTPASS, EventType.fromCode("E_AUDIT_NOTPASS"));
        assertEquals(EventType.E_AUDIT_CANCEL, EventType.fromCode("E_AUDIT_CANCEL"));
        assertEquals(EventType.E_ACTIVE_ONLINE, EventType.fromCode("E_ACTIVE_ONLINE"));
        assertEquals(EventType.E_ACTIVE_EXPIRE, EventType.fromCode("E_ACTIVE_EXPIRE"));
        assertEquals(EventType.E_MANUAL_OFFLINE, EventType.fromCode("E_MANUAL_OFFLINE"));
    }
    
    @Test
    @DisplayName("fromCode - 无效code抛出异常")
    void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            EventType.fromCode("INVALID_CODE");
        });
    }
    
    @Test
    @DisplayName("getCode - 返回正确的code")
    void testGetCode() {
        assertEquals("E_CREATE_DRAFT", EventType.E_CREATE_DRAFT.getCode());
        assertEquals("E_SUBMIT_AUDIT", EventType.E_SUBMIT_AUDIT.getCode());
        assertEquals("E_AUDIT_PASS", EventType.E_AUDIT_PASS.getCode());
    }
    
    @Test
    @DisplayName("getDescription - 返回正确的描述")
    void testGetDescription() {
        assertEquals("创建活动草稿", EventType.E_CREATE_DRAFT.getDescription());
        assertEquals("提交审核", EventType.E_SUBMIT_AUDIT.getDescription());
        assertEquals("审核通过", EventType.E_AUDIT_PASS.getDescription());
        assertEquals("审核驳回", EventType.E_AUDIT_REJECT.getDescription());
        assertEquals("审核不通过", EventType.E_AUDIT_NOTPASS.getDescription());
        assertEquals("审核作废", EventType.E_AUDIT_CANCEL.getDescription());
        assertEquals("活动上线", EventType.E_ACTIVE_ONLINE.getDescription());
        assertEquals("活动过期", EventType.E_ACTIVE_EXPIRE.getDescription());
        assertEquals("手动下线", EventType.E_MANUAL_OFFLINE.getDescription());
    }
}
