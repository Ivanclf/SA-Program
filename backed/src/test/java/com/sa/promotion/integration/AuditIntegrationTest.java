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
 * 审核模块集成测试
 * 覆盖: 审核通过 / 驳回 / 不通过 / 作废 / 状态查询
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("审核模块集成测试")
class AuditIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ---- 辅助方法 ----
    private String createAndSubmit(String name) throws Exception {
        String resp = mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"stime\":\"2026-12-01T00:00:00\","
                                + "\"etime\":\"2026-12-31T23:59:59\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String promoId = JsonPath.read(resp, "$.data.promotionId");

        // 添加 SKU（submit 需要至少一个 SKU）
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku001").param("discount", "0.90").param("operatorId", "u_seed"))
                .andExpect(status().isOk());

        // 提交审核
        mockMvc.perform(post("/api/promotion/submit-audit/" + promoId)
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        return promoId;
    }

    // ==================== 审核通过 ====================

    @Test
    @DisplayName("POST /api/audit/pass/{promotionId} - 审核通过成功")
    void testPassSuccess() throws Exception {
        mockMvc.perform(post("/api/audit/pass/promo_auditing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"审核通过，同意上线\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/audit/pass/{promotionId} - 非审核中活动通过失败")
    void testPassNonAuditingPromotion() throws Exception {
        mockMvc.perform(post("/api/audit/pass/promo_draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"不应该能通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 审核驳回 ====================

    @Test
    @DisplayName("POST /api/audit/reject/{promotionId} - 审核驳回成功")
    void testRejectSuccess() throws Exception {
        mockMvc.perform(post("/api/audit/reject/promo_auditing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"折扣需要调整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    // ==================== 审核不通过 ====================

    @Test
    @DisplayName("POST /api/audit/notpass/{promotionId} - 审核不通过成功")
    void testNotPassSuccess() throws Exception {
        mockMvc.perform(post("/api/audit/notpass/promo_auditing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"不符合平台规范\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    // ==================== 审核作废 ====================

    @Test
    @DisplayName("POST /api/audit/cancel/{promotionId} - 审核作废成功（WAITING状态）")
    void testCancelSuccess() throws Exception {
        // promo_draft 的审计状态是 WAITING，可以被作废
        mockMvc.perform(post("/api/audit/cancel/promo_draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_seed\",\"comment\":\"活动已取消\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty());
    }

    // ==================== 审核状态查询 ====================

    @Test
    @DisplayName("GET /api/audit/status/{promotionId} - 查询审核状态成功")
    void testGetAuditStatusSuccess() throws Exception {
        mockMvc.perform(get("/api/audit/status/promo_auditing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.promotionId").value("promo_auditing"));
    }

    @Test
    @DisplayName("GET /api/audit/status/{promotionId} - 不存在返回404")
    void testGetAuditStatusNotFound() throws Exception {
        mockMvc.perform(get("/api/audit/status/nonexist_promo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 完整审核链路 ====================

    @Test
    @DisplayName("完整审核链路: 创建 → 添加SKU → 提交审核 → 审核通过 → 审核状态确认")
    void testFullAuditFlow() throws Exception {
        // 1. 创建草稿
        String resp1 = mockMvc.perform(post("/api/promotion/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"完整审核链路测试\",\"stime\":\"2026-12-01T00:00:00\","
                                + "\"etime\":\"2026-12-31T23:59:59\",\"creatorId\":\"u_seed\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String promoId = JsonPath.read(resp1, "$.data.promotionId");

        // 2. 添加 SKU（提交审核需要至少一个 SKU）
        mockMvc.perform(post("/api/promotion/" + promoId + "/sku")
                        .param("skuId", "sku001").param("discount", "0.85").param("operatorId", "u_seed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 3. 提交审核
        mockMvc.perform(post("/api/promotion/submit-audit/" + promoId)
                        .param("operatorId", "u_seed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 4. 审核通过
        mockMvc.perform(post("/api/audit/pass/" + promoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"u_auditor\",\"comment\":\"审核通过\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        // 5. 查询审核状态确认
        mockMvc.perform(get("/api/audit/status/" + promoId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
