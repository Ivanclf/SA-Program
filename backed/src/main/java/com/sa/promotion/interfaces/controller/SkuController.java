package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.request.CreateSkuRequest;
import com.sa.promotion.application.dto.request.UpdateSkuRequest;
import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.service.SkuDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SKU管理控制器 - CTRL-005
 *
 * 提供SKU的创建、更新、删除、详情查询和列表查询接口
 * 异常处理统一由 GlobalExceptionHandler 处理
 */
@RestController
@RequestMapping("/api/sku")
public class SkuController {

    private final SkuDomainService skuDomainService;

    public SkuController(SkuDomainService skuDomainService) {
        this.skuDomainService = skuDomainService;
    }

    /**
     * 创建SKU
     */
    @PostMapping("/create")
    public ApiResponse<Sku> createSku(@RequestBody CreateSkuRequest request) {
        Sku sku = skuDomainService.createSku(request.getSkuName(), request.getOriginalPrice());
        return ApiResponse.success(sku);
    }

    /**
     * 更新SKU信息
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Sku> updateSku(@PathVariable String id, @RequestBody UpdateSkuRequest request) {
        Sku sku = skuDomainService.updateSku(id, request.getSkuName(), request.getOriginalPrice());
        return ApiResponse.success(sku);
    }

    /**
     * 删除SKU
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteSku(@PathVariable String id) {
        skuDomainService.deleteSku(id);
        return ApiResponse.success();
    }

    /**
     * 查询SKU详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Sku> getSku(@PathVariable String id) {
        Sku sku = skuDomainService.findBySkuId(id);
        if (sku == null) {
            return ApiResponse.notFound("SKU not found: " + id);
        }
        return ApiResponse.success(sku);
    }

    /**
     * 查询SKU列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Sku>> listSkus() {
        List<Sku> skus = skuDomainService.listAllSkus();
        return ApiResponse.success(skus);
    }
}
