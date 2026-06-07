package com.sa.promotion.domain.event.enums;

/**
 * 事件类型枚举
 * 定义所有可能的事件类型，用于驱动状态机流转
 */
public enum EventType {
    
    // ========== 活动创建与提交事件 ==========
    
    /**
     * 创建活动草稿
     * 触发：DRAFT状态创建
     */
    E_CREATE_DRAFT("E_CREATE_DRAFT", "创建活动草稿"),
    
    /**
     * 提交审核
     * 触发：DRAFT -> AUDITING, WAITING -> AUDITING
     */
    E_SUBMIT_AUDIT("E_SUBMIT_AUDIT", "提交审核"),
    
    // ========== 审核相关事件 ==========
    
    /**
     * 审核通过
     * 触发：AUDITING -> PASSED, 活动状态 AUDITING -> INIT
     */
    E_AUDIT_PASS("E_AUDIT_PASS", "审核通过"),
    
    /**
     * 审核驳回
     * 触发：AUDITING -> REJECTED, 活动状态 AUDITING -> DRAFT
     */
    E_AUDIT_REJECT("E_AUDIT_REJECT", "审核驳回"),
    
    /**
     * 审核不通过
     * 触发：AUDITING -> NOT_PASSED, 活动状态 AUDITING -> 终态
     */
    E_AUDIT_NOTPASS("E_AUDIT_NOTPASS", "审核不通过"),
    
    /**
     * 审核作废
     * 触发：WAITING/REJECTED -> CANCELLED, 活动状态 DRAFT/AUDITING -> 终态
     */
    E_AUDIT_CANCEL("E_AUDIT_CANCEL", "审核作废"),
    
    // ========== 活动生命周期事件 ==========
    
    /**
     * 活动上线（自动或手动）
     * 触发：INIT -> ONLINE
     */
    E_ACTIVE_ONLINE("E_ACTIVE_ONLINE", "活动上线"),
    
    /**
     * 活动过期（自动）
     * 触发：ONLINE -> EXPIRE
     */
    E_ACTIVE_EXPIRE("E_ACTIVE_EXPIRE", "活动过期"),
    
    /**
     * 手动下线
     * 触发：ONLINE -> OFFLINE
     */
    E_MANUAL_OFFLINE("E_MANUAL_OFFLINE", "手动下线"),

    /**
     * 更新活动信息
     * 触发：活动信息更新（仅草稿状态）
     */
    E_UPDATE_ACTIVITY("E_UPDATE_ACTIVITY", "更新活动"),

    /**
     * 删除活动
     * 触发：活动删除（仅草稿状态）
     */
    E_DELETE_ACTIVITY("E_DELETE_ACTIVITY", "删除活动");

    private final String code;
    private final String description;
    
    EventType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static EventType fromCode(String code) {
        for (EventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid event type code: " + code);
    }
}
