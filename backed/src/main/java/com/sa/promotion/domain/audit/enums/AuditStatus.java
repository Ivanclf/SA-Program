package com.sa.promotion.domain.audit.enums;

/**
 * 审核状态枚举
 * 对应promotion表的audit_status字段
 */
public enum AuditStatus {
    
    /**
     * 等待审核 - 初始状态
     */
    WAITING(0, "等待审核"),
    
    /**
     * 审核中 - 已提交审核
     */
    AUDITING(1, "审核中"),
    
    /**
     * 审核通过
     */
    PASSED(2, "审核通过"),
    
    /**
     * 审核驳回 - 可重新提交
     */
    REJECTED(3, "审核驳回"),
    
    /**
     * 审核不通过 - 终态
     */
    NOT_PASSED(4, "审核不通过"),
    
    /**
     * 审核拟作废 - 终态
     */
    CANCELLED(5, "审核拟作废");
    
    private final int code;
    private final String description;
    
    AuditStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static AuditStatus fromCode(int code) {
        for (AuditStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid audit status code: " + code);
    }
    
    /**
     * 判断是否为终态（不可再变更的状态）
     */
    public boolean isFinalState() {
        return this == PASSED || this == NOT_PASSED || this == CANCELLED;
    }
}
