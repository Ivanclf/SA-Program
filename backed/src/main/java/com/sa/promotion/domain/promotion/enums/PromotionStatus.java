package com.sa.promotion.domain.promotion.enums;

/**
 * 活动状态枚举
 * 对应promotion表的status字段
 */
public enum PromotionStatus {
    
    /**
     * 草稿 - 初始状态
     */
    DRAFT(0, "草稿"),
    
    /**
     * 审核中 - 已提交审核
     */
    AUDITING(1, "审核中"),
    
    /**
     * 待生效 - 审核通过，等待开始时间
     */
    INIT(2, "待生效"),
    
    /**
     * 生效中 - 活动正在进行
     */
    ONLINE(3, "生效中"),
    
    /**
     * 过时 - 活动已结束
     */
    EXPIRE(4, "过时"),
    
    /**
     * 下线 - 手动下线
     */
    OFFLINE(5, "下线");
    
    private final int code;
    private final String description;
    
    PromotionStatus(int code, String description) {
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
    public static PromotionStatus fromCode(int code) {
        for (PromotionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid promotion status code: " + code);
    }
    
    /**
     * 判断是否为终态（不可再变更的状态）
     */
    public boolean isFinalState() {
        return this == EXPIRE || this == OFFLINE;
    }
}
