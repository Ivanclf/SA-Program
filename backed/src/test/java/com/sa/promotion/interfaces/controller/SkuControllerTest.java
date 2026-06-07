package com.sa.promotion.interfaces.controller;

import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.service.SkuDomainService;
import com.sa.promotion.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SKU控制器测试")
class SkuControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SkuDomainService skuDomainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkuController(skuDomainService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/sku/create - 创建SKU成功")
    void testCreateSkuSuccess() throws Exception {
        Sku sku = Sku.builder()
            .skuId("sku020").skuName("新款鼠标")
            .originalPrice(new BigDecimal("199.00")).build();
        when(skuDomainService.createSku("新款鼠标", new BigDecimal("199.00"))).thenReturn(sku);

        mockMvc.perform(post("/api/sku/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuName\":\"新款鼠标\",\"originalPrice\":199.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.skuName").value("新款鼠标"));
    }

    @Test
    @DisplayName("POST /api/sku/create - 参数无效返回400")
    void testCreateSkuInvalid() throws Exception {
        when(skuDomainService.createSku(anyString(), any()))
            .thenThrow(new IllegalArgumentException("SKU name cannot be empty"));

        mockMvc.perform(post("/api/sku/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuName\":\"\",\"originalPrice\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /api/sku/update/{id} - 更新SKU成功")
    void testUpdateSkuSuccess() throws Exception {
        Sku sku = Sku.builder()
            .skuId("sku001").skuName("更新名称")
            .originalPrice(new BigDecimal("399.00")).build();
        when(skuDomainService.updateSku(eq("sku001"), eq("更新名称"), any())).thenReturn(sku);

        mockMvc.perform(put("/api/sku/update/sku001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuName\":\"更新名称\",\"originalPrice\":399.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.skuName").value("更新名称"));
    }

    @Test
    @DisplayName("PUT /api/sku/update/{id} - SKU不存在返回404")
    void testUpdateSkuNotFound() throws Exception {
        when(skuDomainService.updateSku(eq("nonexist"), any(), any()))
            .thenThrow(new ResourceNotFoundException("SKU not found"));

        mockMvc.perform(put("/api/sku/update/nonexist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuName\":\"新名称\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("DELETE /api/sku/delete/{id} - 删除SKU成功")
    void testDeleteSkuSuccess() throws Exception {
        doNothing().when(skuDomainService).deleteSku("sku001");

        mockMvc.perform(delete("/api/sku/delete/sku001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/sku/delete/{id} - 不存在返回404")
    void testDeleteSkuNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("SKU not found"))
            .when(skuDomainService).deleteSku("nonexist");

        mockMvc.perform(delete("/api/sku/delete/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/sku/{id} - 查询SKU详情成功")
    void testGetSkuSuccess() throws Exception {
        Sku sku = Sku.builder()
            .skuId("sku001").skuName("无线耳机")
            .originalPrice(new BigDecimal("299.00")).build();
        when(skuDomainService.findBySkuId("sku001")).thenReturn(sku);

        mockMvc.perform(get("/api/sku/sku001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.skuName").value("无线耳机"));
    }

    @Test
    @DisplayName("GET /api/sku/{id} - 不存在返回404")
    void testGetSkuNotFound() throws Exception {
        when(skuDomainService.findBySkuId("nonexist")).thenReturn(null);

        mockMvc.perform(get("/api/sku/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /api/sku/list - 查询SKU列表成功")
    void testListSkusSuccess() throws Exception {
        List<Sku> skus = Arrays.asList(
            Sku.builder().skuId("sku001").skuName("无线耳机").originalPrice(new BigDecimal("299.00")).build(),
            Sku.builder().skuId("sku002").skuName("机械键盘").originalPrice(new BigDecimal("599.00")).build()
        );
        when(skuDomainService.listAllSkus()).thenReturn(skus);

        mockMvc.perform(get("/api/sku/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2));
    }
}
