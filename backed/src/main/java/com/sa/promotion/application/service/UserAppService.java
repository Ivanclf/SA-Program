package com.sa.promotion.application.service;

import com.sa.promotion.domain.user.entity.User;
import com.sa.promotion.domain.user.service.UserDomainService;
import org.springframework.stereotype.Service;

/**
 * 用户应用服务 - APP-001
 *
 * 职责：
 * - 协调用户域服务，处理用户注册、登录、信息更新等用例
 * - 应用层关注点：事务边界、日志记录、返回领域对象供Controller使用
 * - 不包含业务逻辑，全部委托给UserDomainService
 */
@Service
public class UserAppService {

    private final UserDomainService userDomainService;

    public UserAppService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    /**
     * 用户注册
     */
    public User register(String username, String password, Integer role) {
        return userDomainService.register(username, password, role);
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        return userDomainService.login(username, password);
    }

    /**
     * 用户登出
     */
    public void logout(String userId) {
        User user = userDomainService.getUser(userId);
        // 登出为无状态操作，实际项目中清除token/session
        System.out.println("User logged out: " + user.getUsername());
    }

    /**
     * 查询用户详情
     */
    public User getUser(String userId) {
        return userDomainService.getUser(userId);
    }

    /**
     * 更新用户信息
     */
    public User updateUser(String userId, String username, String password, Integer role) {
        return userDomainService.updateUser(userId, username, password, role);
    }
}
