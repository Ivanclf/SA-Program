package com.sa.promotion.application.dto.response;

import com.sa.promotion.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应 DTO — 不含密码字段，防止敏感信息泄露
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String username;
    private Integer role;
    private LocalDateTime ctime;
    private LocalDateTime utime;

    public static UserResponse from(User user) {
        return UserResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .role(user.getRole())
            .ctime(user.getCtime())
            .utime(user.getUtime())
            .build();
    }
}
