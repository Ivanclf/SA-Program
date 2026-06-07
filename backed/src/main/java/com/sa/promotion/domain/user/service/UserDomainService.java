package com.sa.promotion.domain.user.service;

import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.user.entity.User;
import com.sa.promotion.domain.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户域服务
 */
@Service
public class UserDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDomainService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 注册用户
     */
    public User register(String username, String password, Integer role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username already exists: " + username);
        }

        User user = User.builder()
            .userId(UUID.randomUUID().toString())
            .username(username.trim())
            .password(passwordEncoder.encode(password))
            .role(role != null ? role : User.ROLE_ADMIN)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();

        userRepository.save(user);
        return user;
    }

    /**
     * 登录验证
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return user;
    }

    /**
     * 查询用户
     */
    public User getUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * 更新用户信息
     */
    public User updateUser(String userId, String username, String password, Integer role) {
        User user = getUser(userId);

        if (username != null && !username.trim().isEmpty()) {
            // 检查新用户名是否已被占用
            User existing = userRepository.findByUsername(username.trim()).orElse(null);
            if (existing != null && !existing.getUserId().equals(userId)) {
                throw new IllegalStateException("Username already taken: " + username);
            }
            user.setUsername(username.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (role != null) {
            user.setRole(role);
        }
        user.setUtime(LocalDateTime.now());
        userRepository.update(user);
        return user;
    }
}
