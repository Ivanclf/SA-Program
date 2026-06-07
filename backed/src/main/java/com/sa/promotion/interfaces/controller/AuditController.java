package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.request.AuditRequest;
import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.application.service.AuditAppService;
import com.sa.promotion.application.service.QueryAppService;
import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.event.entity.Event;
import org.springframework.web.bind.annotation.*;

/**
 * 审核控制器 - CTRL-003
 *
 * 提供审核通过、驳回、不通过、作废和审核状态查询接口
 * 异常处理统一由 GlobalExceptionHandler 处理
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditAppService auditAppService;
    private final QueryAppService queryAppService;

    public AuditController(AuditAppService auditAppService, QueryAppService queryAppService) {
        this.auditAppService = auditAppService;
        this.queryAppService = queryAppService;
    }

    /**
     * 审核通过
     */
    @PostMapping("/pass/{promotionId}")
    public ApiResponse<Event> pass(@PathVariable String promotionId, @RequestBody AuditRequest request) {
        Event event = auditAppService.pass(promotionId, request.getAuditorId(), request.getComment());
        return ApiResponse.success(event);
    }

    /**
     * 审核驳回（可重新提交）
     */
    @PostMapping("/reject/{promotionId}")
    public ApiResponse<Event> reject(@PathVariable String promotionId, @RequestBody AuditRequest request) {
        Event event = auditAppService.reject(promotionId, request.getAuditorId(), request.getComment());
        return ApiResponse.success(event);
    }

    /**
     * 审核不通过（终态）
     */
    @PostMapping("/notpass/{promotionId}")
    public ApiResponse<Event> notPass(@PathVariable String promotionId, @RequestBody AuditRequest request) {
        Event event = auditAppService.notPass(promotionId, request.getAuditorId(), request.getComment());
        return ApiResponse.success(event);
    }

    /**
     * 审核作废
     */
    @PostMapping("/cancel/{promotionId}")
    public ApiResponse<Event> cancel(@PathVariable String promotionId, @RequestBody AuditRequest request) {
        Event event = auditAppService.cancel(promotionId, request.getAuditorId(), request.getComment());
        return ApiResponse.success(event);
    }

    /**
     * 查询审核状态
     */
    @GetMapping("/status/{promotionId}")
    public ApiResponse<AuditRecord> getAuditStatus(@PathVariable String promotionId) {
        AuditRecord auditRecord = queryAppService.getAuditStatus(promotionId);
        return ApiResponse.success(auditRecord);
    }
}
