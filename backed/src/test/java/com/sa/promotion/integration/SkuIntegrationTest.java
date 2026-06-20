package com.sa.promotion.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SKU模块集成测试
 * 每个测试方法独立构建所需数据
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("SKU模块集成测试")
class SkuIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ---- 辅助方法 ----
    private String createSku(String name, double price) throws Exception {
        String resp = mockMvc.perform(post("/api/sku/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuName\":\"" + name + "\",\"originalPrice\":" + price + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.skuId");
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("POST /api/sku/create - 创建SKU成功")
    void testCreateSkuSuccess() throws Exception {
        mockMvc.perform(post("/api/sku/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuName\":\"集成测试商品\",\"originalPrice\":299.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").isNotEmpty())
                .andExpect(jsonPath("$.data.skuName").value("集成测试商品"));
    }

    @Test
    @DisplayName("POST /api/sku/create - 参数无效返回400")
    void testCreateSkuInvalid() throws Exception {
        mockMvc.perform(post("/api/sku/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuName\":\"\",\"originalPrice\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("GET /api/sku/{id} - 查询SKU详情成功")
    void testGetSkuSuccess() throws Exception {
        String skuId = createSku("待查询商品", 199.00);
        mockMvc.perform(get("/api/sku/" + skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value(skuId));
    }

    @Test
    @DisplayName("GET /api/sku/{id} - SKU不存在返回404")
    void testGetSkuNotFound() throws Exception {
        mockMvc.perform(get("/api/sku/nonexist_sku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/sku/list - 查询SKU列表")
    void testListSkus() throws Exception {
        mockMvc.perform(get("/api/sku/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").isNotEmpty());
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("PUT /api/sku/update/{id} - 更新SKU成功")
    void testUpdateSkuSuccess() throws Exception {
        String skuId = createSku("待更新商品", 259.00);
        mockMvc.perform(put("/api/sku/update/" + skuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuName\":\"更新后的商品名\",\"originalPrice\":399.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuName").value("更新后的商品名"));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("DELETE /api/sku/delete/{id} - 删除SKU成功")
    void testDeleteSkuSuccess() throws Exception {
        String skuId = createSku("待删除商品", 99.00);
        mockMvc.perform(delete("/api/sku/delete/" + skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/sku/delete/{id} - 不存在返回404")
    void testDeleteSkuNotFound() throws Exception {
        mockMvc.perform(delete("/api/sku/delete/nonexist_sku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
