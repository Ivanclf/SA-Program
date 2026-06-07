package com.sa.promotion.domain.promotion.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import com.sa.promotion.domain.promotion.enums.PromotionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 促销活动实体 - 促销活动域聚合根
 * 
 * DDD设计说明：
 * - 作为促销活动域的聚合根，封装活动的所有业务逻辑
 * - 维护双状态机：活动状态（status）和审核状态（auditStatus）
 * - 管理活动与SKU的关联关系（PromotionSku集合）
 * - 所有对活动的操作必须通过聚合根进行，保证数据一致性
 * 
 * 事件驱动架构：
 * - 状态的变更由事件驱动，通过EventType触发状态流转
 * - 每次状态变更都会产生事件，记录到event_log表
 * - 双状态机联动：审核状态变化会触发活动状态变化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    
    /**
     * 活动唯一标识
     */
    private String promotionId;
    
    /**
     * 促销名称
     */
    private String name;
    
    /**
     * 开始时间
     */
    private LocalDateTime stime;
    
    /**
     * 结束时间
     */
    private LocalDateTime etime;
    
    /**
     * 创建人用户ID
     */
    private String creator;
    
    /**
     * 最近操作人用户ID
     */
    private String operator;
    
    /**
     * 活动状态
     */
    private PromotionStatus status;
    
    /**
     * 审核状态
     */
    private AuditStatus auditStatus;
    
    /**
     * 创建时间
     */
    private LocalDateTime ctime;
    
    /**
     * 更新时间
     */
    private LocalDateTime utime;
    
    /**
     * 活动关联的SKU列表（聚合内的子实体）
     */
    @Builder.Default
    private List<PromotionSku> promotionSkus = new ArrayList<>();
    
    // ========== 业务方法 ==========
    
    /**
     * 添加SKU到活动
     * 
     * @param promotionSku SKU关联对象
     */
    public void addSku(PromotionSku promotionSku) {
        if (promotionSku == null) {
            throw new IllegalArgumentException("PromotionSku cannot be null");
        }
        
        // 只有草稿状态才能修改SKU
        if (this.status != PromotionStatus.DRAFT) {
            throw new IllegalStateException("Cannot add SKU when promotion is not in DRAFT status");
        }
        
        // 验证折扣有效性
        if (!promotionSku.isDiscountValid()) {
            throw new IllegalArgumentException("Invalid discount: " + promotionSku.getDiscount());
        }
        
        // 检查是否已存在相同SKU
        boolean exists = this.promotionSkus.stream()
            .anyMatch(ps -> ps.getSkuId().equals(promotionSku.getSkuId()));
        
        if (exists) {
            throw new IllegalStateException("SKU already exists in this promotion: " + promotionSku.getSkuId());
        }
        
        promotionSku.setPromotionId(this.promotionId);
        this.promotionSkus.add(promotionSku);
    }
    
    /**
     * 从活动中移除SKU
     * 
     * @param skuId SKU ID
     */
    public void removeSku(String skuId) {
        if (skuId == null || skuId.isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
        
        // 只有草稿状态才能修改SKU
        if (this.status != PromotionStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove SKU when promotion is not in DRAFT status");
        }
        
        this.promotionSkus.removeIf(ps -> ps.getSkuId().equals(skuId));
    }
    
    /**
     * 更新SKU折扣
     * 
     * @param skuId SKU ID
     * @param discount 新折扣
     */
    public void updateSkuDiscount(String skuId, BigDecimal discount) {
        if (skuId == null || skuId.isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty");
        }
        
        if (discount == null || discount.compareTo(new BigDecimal("0.01")) < 0 
            || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Invalid discount: " + discount);
        }
        
        // 只有草稿状态才能修改SKU
        if (this.status != PromotionStatus.DRAFT) {
            throw new IllegalStateException("Cannot update SKU discount when promotion is not in DRAFT status");
        }
        
        PromotionSku promotionSku = this.promotionSkus.stream()
            .filter(ps -> ps.getSkuId().equals(skuId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));
        
        promotionSku.setDiscount(discount);
    }
    
    /**
     * 验证活动时间是否有效
     * 
     * @return true-有效，false-无效
     */
    public boolean isTimeValid() {
        if (stime == null || etime == null) {
            return false;
        }
        return stime.isBefore(etime);
    }
    
    /**
     * 判断活动是否可以提交审核
     * 
     * @return true-可以提交，false-不可以提交
     */
    public boolean canSubmitAudit() {
        return this.status == PromotionStatus.DRAFT 
            && this.auditStatus == AuditStatus.WAITING
            && isTimeValid()
            && !this.promotionSkus.isEmpty();
    }
    
    /**
     * 判断活动是否可以上线
     * 
     * @return true-可以上线，false-不可以上线
     */
    public boolean canGoOnline() {
        return this.status == PromotionStatus.INIT 
            && this.auditStatus == AuditStatus.PASSED
            && isTimeValid();
    }
    
    /**
     * 判断活动是否可以手动下线
     * 
     * @return true-可以下线，false-不可以下线
     */
    public boolean canManualOffline() {
        return this.status == PromotionStatus.ONLINE;
    }
    
    /**
     * 判断活动是否已过期
     * 
     * @return true-已过期，false-未过期
     */
    public boolean isExpired() {
        return this.status == PromotionStatus.ONLINE 
            && etime != null 
            && LocalDateTime.now().isAfter(etime);
    }
    
    /**
     * 判断活动是否应该自动生效
     * 
     * @return true-应该生效，false-不应该生效
     */
    public boolean shouldAutoActivate() {
        return this.status == PromotionStatus.INIT 
            && this.auditStatus == AuditStatus.PASSED
            && stime != null 
            && !LocalDateTime.now().isBefore(stime);
    }
}
