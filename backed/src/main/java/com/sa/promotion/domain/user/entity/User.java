package com.sa.promotion.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体 - 用户域聚合根
 * 
 * DDD设计说明：
 * - 作为用户域的聚合根，封装用户相关的所有业务逻辑
 * - 包含用户基本信息和角色权限
 * - 负责用户身份的验证和权限控制
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户唯一标识
     */
    private String userId;
    
    /**
     * 用户名（唯一）
     */
    private String username;
    
    /**
     * 加密密码
     */
    private String password;
    
    /**
     * 角色：1-管理员，2-审核员
     */
    private Integer role;
    
    /**
     * 创建时间
     */
    private LocalDateTime ctime;
    
    /**
     * 更新时间
     */
    private LocalDateTime utime;
    
    /**
     * 角色常量
     */
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_AUDITOR = 2;
    
    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return this.role != null && ROLE_ADMIN == this.role;
    }

    /**
     * 判断是否为审核员
     */
    public boolean isAuditor() {
        return this.role != null && ROLE_AUDITOR == this.role;
    }
    
    /**
     * 验证用户是否有操作权限
     * 
     * @param requiredRole 所需角色
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(int requiredRole) {
        return this.role != null && this.role >= requiredRole;
    }
}
