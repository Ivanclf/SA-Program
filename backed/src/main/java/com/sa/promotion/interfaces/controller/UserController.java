package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.request.LoginRequest;
import com.sa.promotion.application.dto.request.RegisterRequest;
import com.sa.promotion.application.dto.request.UpdateUserRequest;
import com.sa.promotion.application.dto.response.ApiResponse;
import com.sa.promotion.application.dto.response.UserResponse;
import com.sa.promotion.application.service.UserAppService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器 - CTRL-001
 *
 * 提供用户注册、登录、登出、查询和更新接口
 * 异常处理统一由 GlobalExceptionHandler 处理
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@RequestBody LoginRequest request) {
        UserResponse user = UserResponse.from(
            userAppService.login(request.getUsername(), request.getPassword()));
        return ApiResponse.success(user);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam String userId) {
        userAppService.logout(userId);
        return ApiResponse.success();
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable String id) {
        UserResponse user = UserResponse.from(userAppService.getUser(id));
        return ApiResponse.success(user);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse user = UserResponse.from(userAppService.register(
            request.getUsername(),
            request.getPassword(),
            request.getRole()));
        return ApiResponse.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        UserResponse user = UserResponse.from(userAppService.updateUser(
            id,
            request.getUsername(),
            request.getPassword(),
            request.getRole()));
        return ApiResponse.success(user);
    }
}
