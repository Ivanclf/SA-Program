package com.sa.promotion.domain.promotion.service;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 促销活动域服务单元测试
 * 
 * 测试覆盖：
 * 1. createDraft - 创建活动草稿
 * 2. addSkuToPromotion/removeSkuFromPromotion/updateSkuDiscount - SKU管理
 * 3. submitAudit - 提交审核（双状态机联动）
 * 4. goOnline/goOffline/expire - 活动生命周期
 * 5. autoActivateIfNeeded - 自动激活
 */
@DisplayName("促销活动域服务测试")
class PromotionDomainServiceTest {
    
    private PromotionDomainService promotionDomainService;
    
    @Mock
    private PromotionStateEngine promotionStateEngine;

    @Mock
    private AuditStateEngine auditStateEngine;

    @Mock
    private StateMachineLinkageValidator linkageValidator;

    private Promotion promotion;
    private AuditRecord auditRecord;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promotionDomainService = new PromotionDomainService(promotionStateEngine, auditStateEngine, linkageValidator);
        
        promotion = createPromotion(PromotionStatus.DRAFT, AuditStatus.WAITING);
        auditRecord = createAuditRecord(AuditStatus.WAITING);
        
        // 默认mock行为
        when(linkageValidator.validateLinkageTransition(any(), any(), any())).thenReturn(true);

        // Mock: AuditStateEngine 状态转换
        when(auditStateEngine.transition(any(), any())).thenAnswer(invocation -> {
            AuditRecord record = invocation.getArgument(0);
            EventType eventType = invocation.getArgument(1);
            if (eventType == EventType.E_SUBMIT_AUDIT) {
                record.setAuditStatus(AuditStatus.AUDITING);
            }
            return record.getAuditStatus();
        });

        when(promotionStateEngine.transition(any(), any())).thenAnswer(invocation -> {
            Promotion p = invocation.getArgument(0);
            EventType eventType = invocation.getArgument(1);
            
            if (eventType == EventType.E_SUBMIT_AUDIT) {
                p.setStatus(PromotionStatus.AUDITING);
            } else if (eventType == EventType.E_ACTIVE_ONLINE) {
                p.setStatus(PromotionStatus.ONLINE);
            } else if (eventType == EventType.E_MANUAL_OFFLINE) {
                p.setStatus(PromotionStatus.OFFLINE);
            } else if (eventType == EventType.E_ACTIVE_EXPIRE) {
                p.setStatus(PromotionStatus.EXPIRE);
            }
            return p.getStatus();
        });
    }
    
    // ========== createDraft测试 ==========
    
    @Test
    @DisplayName("createDraft - 成功创建活动草稿")
    void testCreateDraftValid() {
        LocalDateTime stime = LocalDateTime.now();
        LocalDateTime etime = stime.plusDays(7);
        
        Promotion created = promotionDomainService.createDraft("Test Promotion", stime, etime, "user-001");
        
        assertNotNull(created);
        assertNotNull(created.getPromotionId());
        assertEquals("Test Promotion", created.getName());
        assertEquals(PromotionStatus.DRAFT, created.getStatus());
        assertEquals(AuditStatus.WAITING, created.getAuditStatus());
        assertTrue(created.isTimeValid());
    }
    
    @Test
    @DisplayName("createDraft - 无效时间范围抛出异常")
    void testCreateDraftInvalidTime() {
        LocalDateTime stime = LocalDateTime.now().plusDays(7);
        LocalDateTime etime = LocalDateTime.now();
        
        assertThrows(IllegalArgumentException.class, () -> {
            promotionDomainService.createDraft("Test Promotion", stime, etime, "user-001");
        });
    }
    
    // ========== SKU管理测试 ==========
    
    @Test
    @DisplayName("addSkuToPromotion - 成功添加SKU")
    void testAddSkuToPromotion() {
        promotionDomainService.addSkuToPromotion(promotion, "sku-001", new BigDecimal("0.8"), "user-001");
        
        assertEquals(1, promotion.getPromotionSkus().size());
        assertEquals("user-001", promotion.getOperator());
        assertNotNull(promotion.getUtime());
    }
    
    @Test
    @DisplayName("removeSkuFromPromotion - 成功移除SKU")
    void testRemoveSkuFromPromotion() {
        promotionDomainService.addSkuToPromotion(promotion, "sku-001", new BigDecimal("0.8"), "user-001");
        promotionDomainService.removeSkuFromPromotion(promotion, "sku-001", "user-001");
        
        assertEquals(0, promotion.getPromotionSkus().size());
    }
    
    @Test
    @DisplayName("updateSkuDiscount - 成功更新折扣")
    void testUpdateSkuDiscount() {
        promotionDomainService.addSkuToPromotion(promotion, "sku-001", new BigDecimal("0.8"), "user-001");
        promotionDomainService.updateSkuDiscount(promotion, "sku-001", new BigDecimal("0.9"), "user-001");
        
        assertEquals(new BigDecimal("0.9"), promotion.getPromotionSkus().get(0).getDiscount());
    }
    
    // ========== submitAudit测试 ==========
    
    @Test
    @DisplayName("submitAudit - 成功提交审核")
    void testSubmitAuditValid() {
        promotionDomainService.addSkuToPromotion(promotion, "sku-001", new BigDecimal("0.8"), "user-001");
        
        Event event = promotionDomainService.submitAudit(promotion, auditRecord, "user-001");
        
        assertNotNull(event);
        assertEquals(EventType.E_SUBMIT_AUDIT, event.getEventType());
        assertEquals(PromotionStatus.AUDITING, promotion.getStatus());
        assertEquals(AuditStatus.AUDITING, auditRecord.getAuditStatus());
    }
    
    @Test
    @DisplayName("submitAudit - 不能提交时抛出异常")
    void testSubmitAuditCannotSubmit() {
        assertThrows(IllegalStateException.class, () -> {
            promotionDomainService.submitAudit(promotion, auditRecord, "user-001");
        });
    }
    
    @Test
    @DisplayName("submitAudit - 联动验证失败抛出异常")
    void testSubmitAuditLinkageValidationFailed() {
        promotionDomainService.addSkuToPromotion(promotion, "sku-001", new BigDecimal("0.8"), "user-001");
        when(linkageValidator.validateLinkageTransition(any(), any(), any())).thenReturn(false);
        
        assertThrows(IllegalStateException.class, () -> {
            promotionDomainService.submitAudit(promotion, auditRecord, "user-001");
        });
    }
    
    // ========== goOnline测试 ==========
    
    @Test
    @DisplayName("goOnline - 成功上线")
    void testGoOnlineValid() {
        promotion.setStatus(PromotionStatus.INIT);
        promotion.setAuditStatus(AuditStatus.PASSED);
        
        Event event = promotionDomainService.goOnline(promotion, "user-001");
        
        assertNotNull(event);
        assertEquals(EventType.E_ACTIVE_ONLINE, event.getEventType());
        assertEquals(PromotionStatus.ONLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("goOnline - 不能上线时抛出异常")
    void testGoOnlineCannotGoOnline() {
        assertThrows(IllegalStateException.class, () -> {
            promotionDomainService.goOnline(promotion, "user-001");
        });
    }
    
    // ========== goOffline测试 ==========
    
    @Test
    @DisplayName("goOffline - 成功下线")
    void testGoOfflineValid() {
        promotion.setStatus(PromotionStatus.ONLINE);
        
        Event event = promotionDomainService.goOffline(promotion, "user-001");
        
        assertNotNull(event);
        assertEquals(EventType.E_MANUAL_OFFLINE, event.getEventType());
        assertEquals(PromotionStatus.OFFLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("goOffline - 不能下线时抛出异常")
    void testGoOfflineCannotGoOffline() {
        assertThrows(IllegalStateException.class, () -> {
            promotionDomainService.goOffline(promotion, "user-001");
        });
    }
    
    // ========== expire测试 ==========
    
    @Test
    @DisplayName("expire - 成功过期")
    void testExpireValid() {
        promotion.setStatus(PromotionStatus.ONLINE);
        promotion.setEtime(LocalDateTime.now().minusHours(1));
        
        Event event = promotionDomainService.expire(promotion);
        
        assertNotNull(event);
        assertEquals(EventType.E_ACTIVE_EXPIRE, event.getEventType());
        assertEquals(PromotionStatus.EXPIRE, promotion.getStatus());
        assertEquals("SYSTEM", event.getOperator());
    }
    
    @Test
    @DisplayName("expire - 未过期时抛出异常")
    void testExpireNotExpired() {
        assertThrows(IllegalStateException.class, () -> {
            promotionDomainService.expire(promotion);
        });
    }
    
    // ========== autoActivateIfNeeded测试 ==========
    
    @Test
    @DisplayName("autoActivateIfNeeded - 应该激活时返回true")
    void testAutoActivateIfNeededShouldActivate() {
        promotion.setStatus(PromotionStatus.INIT);
        promotion.setAuditStatus(AuditStatus.PASSED);
        promotion.setStime(LocalDateTime.now().minusHours(1));
        
        boolean result = promotionDomainService.autoActivateIfNeeded(promotion, "SYSTEM");
        
        assertTrue(result);
        assertEquals(PromotionStatus.ONLINE, promotion.getStatus());
    }
    
    @Test
    @DisplayName("autoActivateIfNeeded - 不应该激活时返回false")
    void testAutoActivateIfNeededShouldNotActivate() {
        boolean result = promotionDomainService.autoActivateIfNeeded(promotion, "SYSTEM");
        
        assertFalse(result);
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
