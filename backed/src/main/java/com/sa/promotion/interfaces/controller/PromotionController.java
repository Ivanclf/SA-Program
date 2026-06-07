package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.request.CreatePromotionRequest;
import com.sa.promotion.application.dto.request.UpdatePromotionRequest;
import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.application.service.PromotionAppService;
import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.event.entity.Event;
import com.sa.promotion.domain.promotion.entity.Promotion;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动控制器 - CTRL-002
 *
 * 提供活动的创建、更新、删除、提交审核、手动下线、列表查询和详情查询接口
 * 异常处理统一由 GlobalExceptionHandler 处理
 */
@RestController
@RequestMapping("/api/promotion")
public class PromotionController {

    private final PromotionAppService promotionAppService;
    private final QueryAppService queryAppService;

    public PromotionController(PromotionAppService promotionAppService, QueryAppService queryAppService) {
        this.promotionAppService = promotionAppService;
        this.queryAppService = queryAppService;
    }

    /**
     * 创建活动草稿
     */
    @PostMapping("/create")
    public ApiResponse<Promotion> createPromotion(@RequestBody CreatePromotionRequest request) {
        Promotion promotion = promotionAppService.createPromotion(
            request.getName(),
            LocalDateTime.parse(request.getStime()),
            LocalDateTime.parse(request.getEtime()),
            request.getCreatorId());
        return ApiResponse.success(promotion);
    }

    /**
     * 更新活动信息
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Promotion> updatePromotion(@PathVariable String id, @RequestBody UpdatePromotionRequest request) {
        LocalDateTime stime = request.getStime() != null ? LocalDateTime.parse(request.getStime()) : null;
        LocalDateTime etime = request.getEtime() != null ? LocalDateTime.parse(request.getEtime()) : null;
        Promotion promotion = promotionAppService.updatePromotion(
            id,
            request.getName(),
            stime,
            etime,
            request.getOperatorId());
        return ApiResponse.success(promotion);
    }

    /**
     * 删除活动（仅草稿状态可删除）
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deletePromotion(@PathVariable String id, @RequestParam String operatorId) {
        promotionAppService.deletePromotion(id, operatorId);
        return ApiResponse.success();
    }

    /**
     * 提交审核
     */
    @PostMapping("/submit-audit/{id}")
    public ApiResponse<Event> submitAudit(@PathVariable String id, @RequestParam String operatorId) {
        Event event = promotionAppService.submitAudit(id, operatorId);
        return ApiResponse.success(event);
    }

    /**
     * 手动下线活动
     */
    @PostMapping("/offline/{id}")
    public ApiResponse<Event> offline(@PathVariable String id, @RequestParam String operatorId) {
        Event event = promotionAppService.offline(id, operatorId);
        return ApiResponse.success(event);
    }

    /**
     * 查询活动列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Promotion>> listPromotions() {
        List<Promotion> promotions = queryAppService.listPromotions();
        return ApiResponse.success(promotions);
    }

    /**
     * 查询活动详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Promotion> getPromotion(@PathVariable String id) {
        Promotion promotion = queryAppService.getPromotion(id);
        return ApiResponse.success(promotion);
    }
}
