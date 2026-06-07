package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.service.AuditAppService;
import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.event.enums.EventType;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("审核控制器测试")
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditAppService auditAppService;

    @Mock
    private QueryAppService queryAppService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new AuditController(auditAppService, queryAppService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/audit/pass/{promotionId} - 审核通过成功")
    void testPassSuccess() throws Exception {
        Event event = Event.builder().eventId("evt001")
            .eventType(EventType.E_AUDIT_PASS).promotionId("promo001").build();
        when(auditAppService.pass(eq("promo001"), eq("u006"), anyString())).thenReturn(event);

        mockMvc.perform(post("/api/audit/pass/promo001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"auditorId\":\"u006\",\"comment\":\"审核通过\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventType").value("E_AUDIT_PASS"));
    }

    @Test
    @DisplayName("POST /api/audit/reject/{promotionId} - 审核驳回成功")
    void testRejectSuccess() throws Exception {
        Event event = Event.builder().eventId("evt002")
            .eventType(EventType.E_AUDIT_REJECT).promotionId("promo001").build();
        when(auditAppService.reject(eq("promo001"), eq("u006"), anyString())).thenReturn(event);

        mockMvc.perform(post("/api/audit/reject/promo001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"auditorId\":\"u006\",\"comment\":\"需要修改\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventType").value("E_AUDIT_REJECT"));
    }

    @Test
    @DisplayName("POST /api/audit/notpass/{promotionId} - 审核不通过成功")
    void testNotPassSuccess() throws Exception {
        Event event = Event.builder().eventId("evt003")
            .eventType(EventType.E_AUDIT_NOTPASS).promotionId("promo001").build();
        when(auditAppService.notPass(eq("promo001"), eq("u006"), anyString())).thenReturn(event);

        mockMvc.perform(post("/api/audit/notpass/promo001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"auditorId\":\"u006\",\"comment\":\"不符合规范\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventType").value("E_AUDIT_NOTPASS"));
    }

    @Test
    @DisplayName("POST /api/audit/cancel/{promotionId} - 审核作废成功")
    void testCancelSuccess() throws Exception {
        Event event = Event.builder().eventId("evt004")
            .eventType(EventType.E_AUDIT_CANCEL).promotionId("promo001").build();
        when(auditAppService.cancel(eq("promo001"), eq("u001"), anyString())).thenReturn(event);

        mockMvc.perform(post("/api/audit/cancel/promo001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"auditorId\":\"u001\",\"comment\":\"活动取消\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.eventType").value("E_AUDIT_CANCEL"));
    }

    @Test
    @DisplayName("POST /api/audit/pass/{promotionId} - 不存在返回400")
    void testPassNotFound() throws Exception {
        when(auditAppService.pass(eq("nonexist"), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Promotion not found"));

        mockMvc.perform(post("/api/audit/pass/nonexist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"auditorId\":\"u006\",\"comment\":\"ok\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/audit/status/{promotionId} - 查询审核状态成功")
    void testGetAuditStatusSuccess() throws Exception {
        AuditRecord record = AuditRecord.builder()
            .auditId("audit001").promotionId("promo001")
            .auditStatus(AuditStatus.AUDITING).build();
        when(queryAppService.getAuditStatus("promo001")).thenReturn(record);

        mockMvc.perform(get("/api/audit/status/promo001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.auditStatus").value("AUDITING"));
    }

    @Test
    @DisplayName("GET /api/audit/status/{promotionId} - 不存在返回404")
    void testGetAuditStatusNotFound() throws Exception {
        when(queryAppService.getAuditStatus("nonexist"))
            .thenThrow(new ResourceNotFoundException("Audit record not found"));

        mockMvc.perform(get("/api/audit/status/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }
}
