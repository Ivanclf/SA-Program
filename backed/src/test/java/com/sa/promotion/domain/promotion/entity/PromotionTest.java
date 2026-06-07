package com.sa.promotion.domain.promotion.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 促销活动实体单元测试
 * 
 * 测试覆盖：
 * 1. SKU管理功能（添加、删除、更新折扣）
 * 2. 业务规则验证（时间有效性、状态检查）
 * 3. 边界条件和异常处理
 */
@DisplayName("促销活动实体测试")
class PromotionTest {
    
    private Promotion promotion;
    
    @BeforeEach
    void setUp() {
        promotion = createPromotion(PromotionStatus.DRAFT, AuditStatus.WAITING);
    }
    
    // ========== SKU管理测试 ==========
    
    @Test
    @DisplayName("addSku - DRAFT状态下成功添加SKU")
    void testAddSkuInDraftStatus() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        
        promotion.addSku(sku);
        
        assertEquals(1, promotion.getPromotionSkus().size());
        assertEquals("sku-001", promotion.getPromotionSkus().get(0).getSkuId());
        assertEquals("test-promotion-id", promotion.getPromotionSkus().get(0).getPromotionId());
    }
    
    @Test
    @DisplayName("addSku - 非DRAFT状态抛出异常")
    void testAddSkuNotInDraftStatus() {
        promotion.setStatus(PromotionStatus.AUDITING);
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        
        assertThrows(IllegalStateException.class, () -> {
            promotion.addSku(sku);
        });
    }
    
    @Test
    @DisplayName("addSku - 无效折扣抛出异常")
    void testAddSkuInvalidDiscount() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("1.5"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            promotion.addSku(sku);
        });
    }
    
    @Test
    @DisplayName("addSku - 重复SKU抛出异常")
    void testAddSkuDuplicate() {
        PromotionSku sku1 = createPromotionSku("sku-001", new BigDecimal("0.8"));
        PromotionSku sku2 = createPromotionSku("sku-001", new BigDecimal("0.9"));
        
        promotion.addSku(sku1);
        
        assertThrows(IllegalStateException.class, () -> {
            promotion.addSku(sku2);
        });
    }
    
    @Test
    @DisplayName("addSku - null参数抛出异常")
    void testAddSkuNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            promotion.addSku(null);
        });
    }
    
    @Test
    @DisplayName("removeSku - DRAFT状态下成功移除SKU")
    void testRemoveSkuInDraftStatus() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        
        promotion.removeSku("sku-001");
        
        assertEquals(0, promotion.getPromotionSkus().size());
    }
    
    @Test
    @DisplayName("removeSku - 非DRAFT状态抛出异常")
    void testRemoveSkuNotInDraftStatus() {
        promotion.setStatus(PromotionStatus.AUDITING);
        
        assertThrows(IllegalStateException.class, () -> {
            promotion.removeSku("sku-001");
        });
    }
    
    @Test
    @DisplayName("updateSkuDiscount - DRAFT状态下成功更新折扣")
    void testUpdateSkuDiscountInDraftStatus() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        
        promotion.updateSkuDiscount("sku-001", new BigDecimal("0.9"));
        
        assertEquals(new BigDecimal("0.9"), promotion.getPromotionSkus().get(0).getDiscount());
    }
    
    @Test
    @DisplayName("updateSkuDiscount - 不存在的SKU抛出异常")
    void testUpdateSkuDiscountNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            promotion.updateSkuDiscount("non-existent-sku", new BigDecimal("0.9"));
        });
    }
    
    @Test
    @DisplayName("updateSkuDiscount - 无效折扣抛出异常")
    void testUpdateSkuDiscountInvalidDiscount() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        
        assertThrows(IllegalArgumentException.class, () -> {
            promotion.updateSkuDiscount("sku-001", new BigDecimal("1.5"));
        });
    }
    
    // ========== 业务规则验证测试 ==========
    
    @Test
    @DisplayName("isTimeValid - 有效时间范围返回true")
    void testIsTimeValid() {
        assertTrue(promotion.isTimeValid());
    }
    
    @Test
    @DisplayName("isTimeValid - stime晚于etime返回false")
    void testIsTimeValidInvalidRange() {
        promotion.setStime(LocalDateTime.now().plusDays(7));
        promotion.setEtime(LocalDateTime.now());
        
        assertFalse(promotion.isTimeValid());
    }
    
    @Test
    @DisplayName("isTimeValid - null时间返回false")
    void testIsTimeValidNull() {
        promotion.setStime(null);
        
        assertFalse(promotion.isTimeValid());
    }
    
    @Test
    @DisplayName("canSubmitAudit - DRAFT+WAITING+有效时间+有SKU返回true")
    void testCanSubmitAuditValid() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        
        assertTrue(promotion.canSubmitAudit());
    }
    
    @Test
    @DisplayName("canSubmitAudit - 没有SKU返回false")
    void testCanSubmitAuditNoSku() {
        assertFalse(promotion.canSubmitAudit());
    }
    
    @Test
    @DisplayName("canSubmitAudit - 非DRAFT状态返回false")
    void testCanSubmitAuditNotDraft() {
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        promotion.setStatus(PromotionStatus.AUDITING);
        
        assertFalse(promotion.canSubmitAudit());
    }
    
    @Test
    @DisplayName("canSubmitAudit - 非WAITING审核状态返回false")
    void testCanSubmitAuditNotWaiting() {
        promotion.setAuditStatus(AuditStatus.AUDITING);
        PromotionSku sku = createPromotionSku("sku-001", new BigDecimal("0.8"));
        promotion.addSku(sku);
        
        assertFalse(promotion.canSubmitAudit());
    }
    
    @Test
    @DisplayName("canGoOnline - INIT+PASSED+有效时间返回true")
    void testCanGoOnlineValid() {
        promotion.setStatus(PromotionStatus.INIT);
        promotion.setAuditStatus(AuditStatus.PASSED);
        
        assertTrue(promotion.canGoOnline());
    }
    
    @Test
    @DisplayName("canGoOnline - 非INIT状态返回false")
    void testCanGoOnlineNotInit() {
        promotion.setAuditStatus(AuditStatus.PASSED);
        
        assertFalse(promotion.canGoOnline());
    }
    
    @Test
    @DisplayName("canManualOffline - ONLINE状态返回true")
    void testCanManualOfflineValid() {
        promotion.setStatus(PromotionStatus.ONLINE);
        
        assertTrue(promotion.canManualOffline());
    }
    
    @Test
    @DisplayName("canManualOffline - 非ONLINE状态返回false")
    void testCanManualOfflineNotOnline() {
        assertFalse(promotion.canManualOffline());
    }
    
    @Test
    @DisplayName("isExpired - ONLINE且当前时间晚于etime返回true")
    void testIsExpiredValid() {
        promotion.setStatus(PromotionStatus.ONLINE);
        promotion.setEtime(LocalDateTime.now().minusHours(1));
        
        assertTrue(promotion.isExpired());
    }
    
    @Test
    @DisplayName("isExpired - 非ONLINE状态返回false")
    void testIsExpiredNotOnline() {
        promotion.setEtime(LocalDateTime.now().minusHours(1));
        
        assertFalse(promotion.isExpired());
    }
    
    @Test
    @DisplayName("isExpired - 未过期返回false")
    void testIsExpiredNotYet() {
        promotion.setStatus(PromotionStatus.ONLINE);
        promotion.setEtime(LocalDateTime.now().plusDays(1));
        
        assertFalse(promotion.isExpired());
    }
    
    @Test
    @DisplayName("shouldAutoActivate - INIT+PASSED+stime已到返回true")
    void testShouldAutoActivateValid() {
        promotion.setStatus(PromotionStatus.INIT);
        promotion.setAuditStatus(AuditStatus.PASSED);
        promotion.setStime(LocalDateTime.now().minusHours(1));
        
        assertTrue(promotion.shouldAutoActivate());
    }
    
    @Test
    @DisplayName("shouldAutoActivate - stime未到返回false")
    void testShouldAutoActivateTimeNotReached() {
        promotion.setStatus(PromotionStatus.INIT);
        promotion.setAuditStatus(AuditStatus.PASSED);
        promotion.setStime(LocalDateTime.now().plusHours(1));
        
        assertFalse(promotion.shouldAutoActivate());
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
    
    private PromotionSku createPromotionSku(String skuId, BigDecimal discount) {
        return PromotionSku.builder()
            .id("test-id")
            .promotionId("")
            .skuId(skuId)
            .discount(discount)
            .build();
    }
}
