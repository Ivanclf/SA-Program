package com.sa.promotion.domain.sku.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SKU实体 - SKU管理域聚合根
 * 
 * DDD设计说明：
 * - 作为SKU域的聚合根，封装SKU的基本信息
 * - 独立于促销活动存在，可被多个活动引用
 * - 包含SKU名称和原价等核心属性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sku {
    
    /**
     * SKU唯一标识
     */
    private String skuId;
    
    /**
     * SKU名称
     */
    private String skuName;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 计算折扣后的价格
     * 
     * @param discount 折扣（0.01-1.00）
     * @return 折后价格
     */
    public BigDecimal calculateDiscountedPrice(BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0 
            || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Invalid discount: " + discount);
        }
        return originalPrice.multiply(discount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 验证折扣是否合法
     * 
     * @param discount 折扣值
     * @return true-合法，false-不合法
     */
    public boolean isValidDiscount(BigDecimal discount) {
        return discount != null 
            && discount.compareTo(new BigDecimal("0.01")) >= 0 
            && discount.compareTo(BigDecimal.ONE) <= 0;
    }
}
