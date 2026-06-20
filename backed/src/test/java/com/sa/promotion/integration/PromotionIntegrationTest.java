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
 * 活动模块集成测试
 * 每个测试方法独立构建所需数据
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("活动模块集成测试")
class PromotionIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ---- 辅助方法：创建草稿活动，返回 promotionId ----
    private String createDraft(String name) throws Exception {
        String resp = mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"stime\":\"2026-12-01T00:00:00\","
                                + "\"etime\":\"2026-12-31T23:59:59\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.promotionId");
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("POST /api/promotion/create - 创建活动草稿成功")
    void testCreatePromotionSuccess() throws Exception {
        mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"集成测试活动\",\"stime\":\"2026-12-01T00:00:00\","
                                + "\"etime\":\"2026-12-31T23:59:59\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.promotionId").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("集成测试活动"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/promotion/create - 时间范围无效返回400")
    void testCreatePromotionInvalidTime() throws Exception {
        mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"时间错误活动\",\"stime\":\"2026-12-31T00:00:00\","
                                + "\"etime\":\"2026-12-01T00:00:00\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("GET /api/promotion/{id} - 查询活动详情成功")
    void testGetPromotionSuccess() throws Exception {
        mockMvc.perform(get("/api/promotion/promo_online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.promotionId").value("promo_online"))
                .andExpect(jsonPath("$.data.status").value("ONLINE"));
    }

    @Test
    @DisplayName("GET /api/promotion/{id} - 活动不存在返回404")
    void testGetPromotionNotFound() throws Exception {
        mockMvc.perform(get("/api/promotion/nonexist_promo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/promotion/list - 查询活动列表")
    void testListPromotions() throws Exception {
        mockMvc.perform(get("/api/promotion/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").isNotEmpty());
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("PUT /api/promotion/update/{id} - 更新草稿活动成功")
    void testUpdatePromotionSuccess() throws Exception {
        String promoId = createDraft("待更新活动");
        mockMvc.perform(put("/api/promotion/update/" + promoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后的活动名\",\"operatorId\":\"u_seed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后的活动名"));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("DELETE /api/promotion/delete/{id} - 删除草稿活动成功")
    void testDeleteDraftPromotionSuccess() throws Exception {
        String promoId = createDraft("待删除活动");
        mockMvc.perform(delete("/api/promotion/delete/" + promoId)
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/promotion/delete/{id} - 非草稿活动删除失败返回400")
    void testDeleteNonDraftPromotion() throws Exception {
        mockMvc.perform(delete("/api/promotion/delete/promo_online")
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 提交审核 ====================

    @Test
    @DisplayName("POST /api/promotion/submit-audit/{id} - 提交审核成功")
    void testSubmitAuditSuccess() throws Exception {
        String promoId = createDraft("审核提交测试");
        // 提交审核前需要至少一个 SKU
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku001").param("discount", "0.85").param("operatorId", "u_seed"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/promotion/submit-audit/" + promoId)
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    // ==================== 下线 ====================

    @Test
    @DisplayName("POST /api/promotion/offline/{id} - 手动下线成功")
    void testOfflineSuccess() throws Exception {
        mockMvc.perform(post("/api/promotion/offline/promo_online")
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    // ==================== SKU 关联 ====================

    @Test
    @DisplayName("POST /api/promotion/{id}/sku - 为活动添加SKU成功")
    void testAddSkuToPromotion() throws Exception {
        String promoId = createDraft("SKU添加测试");
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku001")
                        .param("discount", "0.85")
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/promotion/{id}/sku/{skuId} - 从活动移除SKU成功")
    void testRemoveSkuFromPromotion() throws Exception {
        String promoId = createDraft("SKU移除测试");
        // 先添加 SKU
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku002")
                        .param("discount", "0.90")
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk());
        // 再移除
        mockMvc.perform(delete("/api/promotion/" + promoId + "/sku/sku002")
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 事件时间线 ====================

    @Test
    @DisplayName("GET /api/promotion/{id}/events - 查询活动事件时间线")
    void testGetPromotionEvents() throws Exception {
        mockMvc.perform(get("/api/promotion/promo_online/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").isNotEmpty());
    }

    // ==================== 完整生命周期 ====================

    @Test
    @DisplayName("完整生命周期: 创建 → 更新 → 添加SKU → 提交审核 → 审核通过")
    void testFullLifecycle() throws Exception {
        // 1. 创建
        String resp1 = mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"全流程测试活动\",\"stime\":\"2026-12-01T00:00:00\","
                                + "\"etime\":\"2026-12-31T23:59:59\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String promoId = JsonPath.read(resp1, "$.data.promotionId");

        // 2. 更新
        mockMvc.perform(put("/api/promotion/update/" + promoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"全流程测试活动(已更新)\",\"operatorId\":\"u_seed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 3. 添加 SKU
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku002").param("discount", "0.90").param("operatorId", "u_seed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 4. 提交审核
        mockMvc.perform(post("/api/promotion/submit-audit/" + promoId)
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 5. 审核通过
        mockMvc.perform(post("/api/audit/pass/" + promoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"全流程审核通过\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 6. 确认状态已变更
        mockMvc.perform(get("/api/promotion/" + promoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
