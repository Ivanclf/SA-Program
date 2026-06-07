package com.sa.promotion.domain.audit.entity;

import com.sa.promotion.domain.audit.enums.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核记录实体 - 审核流程域聚合根
 * 
 * DDD设计说明：
 * - 作为审核域的聚合根，封装审核流程的所有业务逻辑
 * - 维护审核状态机，独立于活动状态机运行
 * - 记录审核过程中的关键信息和操作人
 * 
 * 事件驱动架构：
 * - 审核状态的变更由事件驱动（E_AUDIT_PASS、E_AUDIT_REJECT等）
 * - 审核状态变化会触发活动状态的联动变化
 * - 每次审核操作都会产生事件，记录到event_log表
 * 
 * 双状态机联动：
 * - 审核状态机与活动状态机相互独立但通过事件联动
 * - 审核通过 -> 活动状态从AUDITING变为INIT
 * - 审核驳回 -> 活动状态从AUDITING变回DRAFT
 * - 审核不通过 -> 活动状态进入终态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {
    
    /**
     * 审核记录唯一标识（通常与promotionId相同）
     */
    private String auditId;
    
    /**
     * 关联的活动ID
     */
    private String promotionId;
    
    /**
     * 审核状态
     */
    private AuditStatus auditStatus;
    
    /**
     * 提交审核的时间
     */
    private LocalDateTime submitTime;
    
    /**
     * 完成审核的时间
     */
    private LocalDateTime completeTime;
    
    /**
     * 审核员用户ID
     */
    private String auditorId;
    
    /**
     * 审核意见/备注
     */
    private String comment;
    
    /**
     * 创建时间
     */
    private LocalDateTime ctime;
    
    /**
     * 更新时间
     */
    private LocalDateTime utime;
    
    // ========== 业务方法 ==========
    
    /**
     * 判断是否可以提交审核
     * 
     * @return true-可以提交，false-不可以提交
     */
    public boolean canSubmit() {
        return this.auditStatus == AuditStatus.WAITING;
    }
    
    /**
     * 判断是否可以进行审核操作
     * 
     * @return true-可以审核，false-不可以审核
     */
    public boolean canAudit() {
        return this.auditStatus == AuditStatus.AUDITING;
    }
    
    /**
     * 判断是否为终态（不可再变更）
     * 
     * @return true-终态，false-非终态
     */
    public boolean isFinalState() {
        return this.auditStatus != null && this.auditStatus.isFinalState();
    }
    
    /**
     * 提交审核
     * 状态流转：WAITING -> AUDITING
     */
    public void submit(String operatorId) {
        if (!canSubmit()) {
            throw new IllegalStateException("Cannot submit audit when status is: " + this.auditStatus);
        }
        
        this.auditStatus = AuditStatus.AUDITING;
        this.submitTime = LocalDateTime.now();
        this.utime = LocalDateTime.now();
    }
    
    /**
     * 审核通过
     * 状态流转：AUDITING -> PASSED
     * 
     * @param auditorId 审核员ID
     * @param comment 审核意见
     */
    public void pass(String auditorId, String comment) {
        if (!canAudit()) {
            throw new IllegalStateException("Cannot pass audit when status is: " + this.auditStatus);
        }
        
        this.auditStatus = AuditStatus.PASSED;
        this.auditorId = auditorId;
        this.comment = comment;
        this.completeTime = LocalDateTime.now();
        this.utime = LocalDateTime.now();
    }
    
    /**
     * 审核驳回（可重新提交）
     * 状态流转：AUDITING -> REJECTED
     * 
     * @param auditorId 审核员ID
     * @param comment 驳回原因
     */
    public void reject(String auditorId, String comment) {
        if (!canAudit()) {
            throw new IllegalStateException("Cannot reject audit when status is: " + this.auditStatus);
        }
        
        this.auditStatus = AuditStatus.REJECTED;
        this.auditorId = auditorId;
        this.comment = comment;
        this.completeTime = LocalDateTime.now();
        this.utime = LocalDateTime.now();
    }
    
    /**
     * 审核不通过（终态，不可重新提交）
     * 状态流转：AUDITING -> NOT_PASSED
     * 
     * @param auditorId 审核员ID
     * @param comment 不通过原因
     */
    public void notPass(String auditorId, String comment) {
        if (!canAudit()) {
            throw new IllegalStateException("Cannot notPass audit when status is: " + this.auditStatus);
        }
        
        this.auditStatus = AuditStatus.NOT_PASSED;
        this.auditorId = auditorId;
        this.comment = comment;
        this.completeTime = LocalDateTime.now();
        this.utime = LocalDateTime.now();
    }
    
    /**
     * 取消审核（作废）
     * 状态流转：WAITING/REJECTED -> CANCELLED
     * 
     * @param operatorId 操作人ID
     * @param comment 取消原因
     */
    public void cancel(String operatorId, String comment) {
        if (this.auditStatus != AuditStatus.WAITING && this.auditStatus != AuditStatus.REJECTED) {
            throw new IllegalStateException("Cannot cancel audit when status is: " + this.auditStatus);
        }
        
        this.auditStatus = AuditStatus.CANCELLED;
        this.comment = comment;
        this.completeTime = LocalDateTime.now();
        this.utime = LocalDateTime.now();
    }
}
