package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.audit.repository.AuditRecordRepository;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.service.SkuDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("查询应用服务测试")
class QueryAppServiceTest {

    private QueryAppService queryAppService;

    @Mock private PromotionRepository promotionRepository;
    @Mock private AuditRecordRepository auditRecordRepository;
    @Mock private SkuDomainService skuDomainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        queryAppService = new QueryAppService(promotionRepository, auditRecordRepository, skuDomainService);
    }

    @Test
    @DisplayName("getPromotion - 查询活动详情")
    void testGetPromotion() {
        Promotion promotion = createPromotion();
        when(promotionRepository.findById("promo-001")).thenReturn(Optional.of(promotion));

        Promotion result = queryAppService.getPromotion("promo-001");

        assertEquals("测试活动", result.getName());
        assertEquals(PromotionStatus.ONLINE, result.getStatus());
    }

    @Test
    @DisplayName("getPromotion - 不存在抛出异常")
    void testGetPromotionNotFound() {
        when(promotionRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(com.sa.promotion.domain.exception.ResourceNotFoundException.class,
            () -> queryAppService.getPromotion("non-existent"));
    }

    @Test
    @DisplayName("listPromotions - 查询活动列表")
    void testListPromotions() {
        Promotion p1 = createPromotion();
        Promotion p2 = Promotion.builder().promotionId("promo-002").name("活动2")
            .status(PromotionStatus.DRAFT).build();
        when(promotionRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        List<Promotion> result = queryAppService.listPromotions();

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getAuditStatus - 查询审核状态")
    void testGetAuditStatus() {
        AuditRecord auditRecord = AuditRecord.builder()
            .auditId("promo-001").promotionId("promo-001")
            .auditStatus(AuditStatus.PASSED).build();
        when(auditRecordRepository.findByPromotionId("promo-001")).thenReturn(Optional.of(auditRecord));

        AuditRecord result = queryAppService.getAuditStatus("promo-001");

        assertEquals(AuditStatus.PASSED, result.getAuditStatus());
    }

    @Test
    @DisplayName("getSku - 查询SKU详情")
    void testGetSku() {
        Sku sku = Sku.builder().skuId("sku-001").skuName("iPhone").originalPrice(new BigDecimal("7999")).build();
        when(skuDomainService.findBySkuId("sku-001")).thenReturn(sku);

        Sku result = queryAppService.getSku("sku-001");

        assertEquals("iPhone", result.getSkuName());
    }

    @Test
    @DisplayName("getSku - 不存在抛出异常")
    void testGetSkuNotFound() {
        when(skuDomainService.findBySkuId("non-existent")).thenReturn(null);

        assertThrows(com.sa.promotion.domain.exception.ResourceNotFoundException.class,
            () -> queryAppService.getSku("non-existent"));
    }

    @Test
    @DisplayName("listSkus - 查询SKU列表")
    void testListSkus() {
        Sku s1 = Sku.builder().skuId("sku-001").skuName("SKU1").originalPrice(new BigDecimal("100")).build();
        Sku s2 = Sku.builder().skuId("sku-002").skuName("SKU2").originalPrice(new BigDecimal("200")).build();
        when(skuDomainService.listAllSkus()).thenReturn(Arrays.asList(s1, s2));

        List<Sku> result = queryAppService.listSkus();

        assertEquals(2, result.size());
    }

    private Promotion createPromotion() {
        return Promotion.builder()
            .promotionId("promo-001")
            .name("测试活动")
            .status(PromotionStatus.ONLINE)
            .auditStatus(AuditStatus.PASSED)
            .stime(LocalDateTime.now().minusDays(1))
            .etime(LocalDateTime.now().plusDays(7))
            .creator("u001")
            .build();
    }
}
