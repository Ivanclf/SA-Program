package com.sa.promotion.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 客户模块集成测试
 * 覆盖: 客户侧活动查询 / SKU查询 / SKU列表
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("客户模块集成测试")
class CustomerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @DisplayName("GET /api/customer/promotion/{id} - 查询活动详情成功")
    void testGetPromotionSuccess() throws Exception {
        mockMvc.perform(get("/api/customer/promotion/promo_online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.promotionId").value("promo_online"))
                .andExpect(jsonPath("$.data.name").value("Active Promo"));
    }

    @Test
    @DisplayName("GET /api/customer/promotion/{id} - 活动不存在返回404")
    void testGetPromotionNotFound() throws Exception {
        mockMvc.perform(get("/api/customer/promotion/nonexist_promo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/customer/sku/{id} - 查询SKU详情成功")
    void testGetSkuSuccess() throws Exception {
        mockMvc.perform(get("/api/customer/sku/sku001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value("sku001"));
    }

    @Test
    @DisplayName("GET /api/customer/sku/{id} - SKU不存在返回404")
    void testGetSkuNotFound() throws Exception {
        mockMvc.perform(get("/api/customer/sku/nonexist_sku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/customer/sku - 查询SKU列表成功")
    void testListSkus() throws Exception {
        mockMvc.perform(get("/api/customer/sku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").isNotEmpty());
    }
}
