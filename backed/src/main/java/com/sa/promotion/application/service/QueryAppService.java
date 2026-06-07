package com.sa.promotion.application.service;

import com.sa.promotion.domain.audit.entity.AuditRecord;
import com.sa.promotion.domain.audit.repository.AuditRecordRepository;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.promotion.entity.Promotion;
import com.sa.promotion.domain.promotion.repository.PromotionRepository;
import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.service.SkuDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询应用服务 - APP-004
 *
 * 职责：
 * - 跨域只读查询，不涉及状态变更
 * - 直接从仓储读取数据，无需经过域服务
 * - 为Controller层提供统一的查询入口
 */
@Service
public class QueryAppService {

    private final PromotionRepository promotionRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final SkuDomainService skuDomainService;

    public QueryAppService(PromotionRepository promotionRepository,
                           AuditRecordRepository auditRecordRepository,
                           SkuDomainService skuDomainService) {
        this.promotionRepository = promotionRepository;
        this.auditRecordRepository = auditRecordRepository;
        this.skuDomainService = skuDomainService;
    }

    /**
     * 查询活动详情
     */
    public Promotion getPromotion(String promotionId) {
        return promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found: " + promotionId));
    }

    /**
     * 查询活动列表
     */
    public List<Promotion> listPromotions() {
        return promotionRepository.findAll();
    }

    /**
     * 查询审核状态
     */
    public AuditRecord getAuditStatus(String promotionId) {
        return auditRecordRepository.findByPromotionId(promotionId)
            .orElseThrow(() -> new ResourceNotFoundException("Audit record not found for promotion: " + promotionId));
    }

    /**
     * 查询SKU详情
     */
    public Sku getSku(String skuId) {
        Sku sku = skuDomainService.findBySkuId(skuId);
        if (sku == null) {
            throw new ResourceNotFoundException("SKU not found: " + skuId);
        }
        return sku;
    }

    /**
     * 查询SKU列表
     */
    public List<Sku> listSkus() {
        return skuDomainService.listAllSkus();
    }
}
