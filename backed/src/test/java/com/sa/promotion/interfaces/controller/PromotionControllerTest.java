package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.service.PromotionAppService;
import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import com.sa.promotion.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("活动控制器测试")
class PromotionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PromotionAppService promotionAppService;

    @Mock
    private QueryAppService queryAppService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new PromotionController(promotionAppService, queryAppService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/promotion/create - 创建成功")
    void testCreateSuccess() throws Exception {
        Promotion promo = Promotion.builder()
            .promotionId("promo001").name("双十一促销")
            .status(PromotionStatus.DRAFT).build();
        when(promotionAppService.createPromotion(anyString(), any(), any(), anyString())).thenReturn(promo);

        mockMvc.perform(post("/api/promotion/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"双十一促销\",\"stime\":\"2025-12-01T00:00:00\","
                    + "\"etime\":\"2025-12-12T23:59:59\",\"creatorId\":\"u001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.promotionId").value("promo001"));
    }

    @Test
    @DisplayName("POST /api/promotion/create - 参数无效返回400")
    void testCreateInvalidParam() throws Exception {
        when(promotionAppService.createPromotion(anyString(), any(), any(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid time range"));

        mockMvc.perform(post("/api/promotion/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"促销\",\"stime\":\"2025-12-12T00:00:00\","
                    + "\"etime\":\"2025-12-01T00:00:00\",\"creatorId\":\"u001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /api/promotion/update/{id} - 更新成功")
    void testUpdateSuccess() throws Exception {
        Promotion promo = Promotion.builder()
            .promotionId("promo001").name("更新名称")
            .status(PromotionStatus.DRAFT).build();
        when(promotionAppService.updatePromotion(eq("promo001"), anyString(), any(), any(), anyString()))
            .thenReturn(promo);

        mockMvc.perform(put("/api/promotion/update/promo001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"更新名称\",\"operatorId\":\"u001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("更新名称"));
    }

    @Test
    @DisplayName("DELETE /api/promotion/delete/{id} - 删除成功")
    void testDeleteSuccess() throws Exception {
        doNothing().when(promotionAppService).deletePromotion("promo001", "u001");

        mockMvc.perform(delete("/api/promotion/delete/promo001")
                .param("operatorId", "u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/promotion/delete/{id} - 非草稿状态删除失败")
    void testDeleteNonDraft() throws Exception {
        doThrow(new IllegalStateException("Only DRAFT promotions can be deleted"))
            .when(promotionAppService).deletePromotion("promo003", "u001");

        mockMvc.perform(delete("/api/promotion/delete/promo003")
                .param("operatorId", "u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/promotion/submit-audit/{id} - 提交审核成功")
    void testSubmitAuditSuccess() throws Exception {
        Event event = Event.builder().eventId("evt001")
            .eventType(EventType.E_SUBMIT_AUDIT).promotionId("promo001").build();
        when(promotionAppService.submitAudit("promo001", "u001")).thenReturn(event);

        mockMvc.perform(post("/api/promotion/submit-audit/promo001")
                .param("operatorId", "u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventId").value("evt001"));
    }

    @Test
    @DisplayName("POST /api/promotion/offline/{id} - 手动下线成功")
    void testOfflineSuccess() throws Exception {
        Event event = Event.builder().eventId("evt002")
            .eventType(EventType.E_MANUAL_OFFLINE).promotionId("promo003").build();
        when(promotionAppService.offline("promo003", "u001")).thenReturn(event);

        mockMvc.perform(post("/api/promotion/offline/promo003")
                .param("operatorId", "u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventType").value("E_MANUAL_OFFLINE"));
    }

    @Test
    @DisplayName("GET /api/promotion/list - 查询列表")
    void testListPromotions() throws Exception {
        List<Promotion> list = Arrays.asList(
            Promotion.builder().promotionId("p1").name("活动1").build(),
            Promotion.builder().promotionId("p2").name("活动2").build()
        );
        when(queryAppService.listPromotions()).thenReturn(list);

        mockMvc.perform(get("/api/promotion/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/promotion/{id} - 查询详情成功")
    void testGetPromotionSuccess() throws Exception {
        Promotion promo = Promotion.builder().promotionId("promo001").name("双十一").build();
        when(queryAppService.getPromotion("promo001")).thenReturn(promo);

        mockMvc.perform(get("/api/promotion/promo001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("双十一"));
    }

    @Test
    @DisplayName("GET /api/promotion/{id} - 不存在返回404")
    void testGetPromotionNotFound() throws Exception {
        when(queryAppService.getPromotion("nonexist"))
            .thenThrow(new ResourceNotFoundException("Promotion not found"));

        mockMvc.perform(get("/api/promotion/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }
}
