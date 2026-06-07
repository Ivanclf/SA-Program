package com.sa.promotion.domain.promotion.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 活动-SKU关联实体
 * 
 * DDD设计说明：
 * - 作为Promotion聚合内的实体，不能独立存在
 * - 表示某个活动中特定SKU的折扣配置
 * - 通过promotionId和skuId联合唯一标识
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionSku {
    
    /**
     * 记录唯一标识
     */
    private String id;
    
    /**
     * 活动ID
     */
    private String promotionId;
    
    /**
     * SKU ID
     */
    private String skuId;
    
    /**
     * 折扣（0.01-1.00）
     */
    private BigDecimal discount;
    
    /**
     * 验证折扣是否在有效范围内
     * 
     * @return true-有效，false-无效
     */
    public boolean isDiscountValid() {
        return discount != null 
            && discount.compareTo(new BigDecimal("0.01")) >= 0 
            && discount.compareTo(BigDecimal.ONE) <= 0;
    }
    
    /**
     * 验证关联关系是否完整
     * 
     * @return true-完整，false-不完整
     */
    public boolean isAssociationValid() {
        return promotionId != null && !promotionId.isEmpty() 
            && skuId != null && !skuId.isEmpty();
    }
}
