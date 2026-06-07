package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.sku.entity.Sku;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户控制器 - CTRL-004
 *
 * 提供面向外部客户的只读查询接口：活动详情、SKU详情、活动SKU列表
 * 异常处理统一由 GlobalExceptionHandler 处理
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final QueryAppService queryAppService;

    public CustomerController(QueryAppService queryAppService) {
        this.queryAppService = queryAppService;
    }

    /**
     * 查询活动详情
     */
    @GetMapping("/promotion/{id}")
    public ApiResponse<Promotion> getPromotion(@PathVariable String id) {
        Promotion promotion = queryAppService.getPromotion(id);
        return ApiResponse.success(promotion);
    }

    /**
     * 查询SKU详情（含折扣信息）
     */
    @GetMapping("/sku/{id}")
    public ApiResponse<Sku> getSku(@PathVariable String id) {
        Sku sku = queryAppService.getSku(id);
        return ApiResponse.success(sku);
    }

    /**
     * 查询活动SKU列表
     */
    @GetMapping("/sku")
    public ApiResponse<List<Sku>> listSkus() {
        List<Sku> skus = queryAppService.listSkus();
        return ApiResponse.success(skus);
    }
}
