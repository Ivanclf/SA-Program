package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("客户控制器测试")
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QueryAppService queryAppService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(queryAppService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/customer/promotion/{id} - 查询活动详情成功")
    void testGetPromotionSuccess() throws Exception {
        Promotion promo = Promotion.builder()
            .promotionId("promo003").name("双十一促销")
            .status(PromotionStatus.ONLINE).build();
        when(queryAppService.getPromotion("promo003")).thenReturn(promo);

        mockMvc.perform(get("/api/customer/promotion/promo003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("双十一促销"))
            .andExpect(jsonPath("$.data.status").value("ONLINE"));
    }

    @Test
    @DisplayName("GET /api/customer/promotion/{id} - 不存在返回404")
    void testGetPromotionNotFound() throws Exception {
        when(queryAppService.getPromotion("nonexist"))
            .thenThrow(new ResourceNotFoundException("Promotion not found"));

        mockMvc.perform(get("/api/customer/promotion/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/customer/sku/{id} - 查询SKU详情成功")
    void testGetSkuSuccess() throws Exception {
        Sku sku = Sku.builder()
            .skuId("sku001").skuName("无线耳机")
            .originalPrice(new BigDecimal("299.00")).build();
        when(queryAppService.getSku("sku001")).thenReturn(sku);

        mockMvc.perform(get("/api/customer/sku/sku001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.skuName").value("无线耳机"));
    }

    @Test
    @DisplayName("GET /api/customer/sku/{id} - 不存在返回404")
    void testGetSkuNotFound() throws Exception {
        when(queryAppService.getSku("nonexist"))
            .thenThrow(new ResourceNotFoundException("SKU not found"));

        mockMvc.perform(get("/api/customer/sku/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/customer/sku - 查询SKU列表成功")
    void testListSkusSuccess() throws Exception {
        List<Sku> skus = Arrays.asList(
            Sku.builder().skuId("sku001").skuName("无线耳机").originalPrice(new BigDecimal("299.00")).build(),
            Sku.builder().skuId("sku002").skuName("机械键盘").originalPrice(new BigDecimal("599.00")).build()
        );
        when(queryAppService.listSkus()).thenReturn(skus);

        mockMvc.perform(get("/api/customer/sku"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2));
    }
}
