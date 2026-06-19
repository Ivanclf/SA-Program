package com.sa.promotion.domain.user.service;

import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.user.entity.User;
import com.sa.promotion.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("用户域服务测试")
class UserDomainServiceTest {

    private UserDomainService userDomainService;

    @Mock
    private UserRepository userRepository;

    private User existingUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDomainService = new UserDomainService(userRepository);

        existingUser = User.builder()
            .userId("u001")
            .username("admin")
            .password("$2a$10$encryptedPasswordHashHere")
            .role(User.ROLE_ADMIN)
            .ctime(LocalDateTime.now().minusDays(10))
            .utime(LocalDateTime.now().minusDays(1))
            .build();
    }

    // ========== register ==========

    @Test
    @DisplayName("register - 成功注册新用户")
    void testRegisterSuccess() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        User result = userDomainService.register("newuser", "password123", User.ROLE_AUDITOR);

        assertNotNull(result);
        assertNotNull(result.getUserId());
        assertEquals("newuser", result.getUsername());
        assertEquals(Integer.valueOf(User.ROLE_AUDITOR), result.getRole());
        assertNotEquals("password123", result.getPassword()); // 密码已加密
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - 角色为null时默认管理员")
    void testRegisterNullRoleDefaultsToAdmin() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        User result = userDomainService.register("user", "pass", null);

        assertEquals(Integer.valueOf(User.ROLE_ADMIN), result.getRole());
    }

    @Test
    @DisplayName("register - 空用户名抛出异常")
    void testRegisterEmptyUsername() {
        assertThrows(IllegalArgumentException.class,
            () -> userDomainService.register("", "pass", User.ROLE_ADMIN));
        assertThrows(IllegalArgumentException.class,
            () -> userDomainService.register("   ", "pass", User.ROLE_ADMIN));
    }

    @Test
    @DisplayName("register - 空密码抛出异常")
    void testRegisterEmptyPassword() {
        assertThrows(IllegalArgumentException.class,
            () -> userDomainService.register("user", "", User.ROLE_ADMIN));
    }

    @Test
    @DisplayName("register - 用户名已存在抛出异常")
    void testRegisterDuplicateUsername() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalStateException.class,
            () -> userDomainService.register("admin", "pass", User.ROLE_ADMIN));
    }

    // ========== login ==========

    @Test
    @DisplayName("login - 登录成功")
    void testLoginSuccess() {
        // 使用真实的BCrypt编码器来生成可验证的密码
        User userWithRealHash = User.builder()
            .userId("u002")
            .username("testuser")
            .password(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct"))
            .role(User.ROLE_ADMIN)
            .ctime(LocalDateTime.now())
            .utime(LocalDateTime.now())
            .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithRealHash));

        User result = userDomainService.login("testuser", "correct");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("login - 密码错误抛出异常")
    void testLoginWrongPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class,
            () -> userDomainService.login("admin", "wrongpassword"));
    }

    @Test
    @DisplayName("login - 用户不存在抛出异常")
    void testLoginUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userDomainService.login("ghost", "pass"));
    }

    // ========== getUser ==========

    @Test
    @DisplayName("getUser - 查询成功")
    void testGetUserSuccess() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));

        User result = userDomainService.getUser("u001");

        assertEquals("admin", result.getUsername());
        assertEquals(Integer.valueOf(User.ROLE_ADMIN), result.getRole());
    }

    @Test
    @DisplayName("getUser - 不存在抛出异常")
    void testGetUserNotFound() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userDomainService.getUser("ghost"));
    }

    // ========== updateUser ==========

    @Test
    @DisplayName("updateUser - 成功更新用户名")
    void testUpdateUserUsername() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());

        User result = userDomainService.updateUser("u001", "newadmin", null, null);

        assertEquals("newadmin", result.getUsername());
        verify(userRepository).update(any(User.class));
    }

    @Test
    @DisplayName("updateUser - 成功更新密码")
    void testUpdateUserPassword() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));

        String oldPassword = existingUser.getPassword();
        User result = userDomainService.updateUser("u001", null, "newpass", null);

        assertNotEquals(oldPassword, result.getPassword()); // 密码已重新加密
        verify(userRepository).update(any(User.class));
    }

    @Test
    @DisplayName("updateUser - 成功更新角色")
    void testUpdateUserRole() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));

        User result = userDomainService.updateUser("u001", null, null, User.ROLE_AUDITOR);

        assertEquals(Integer.valueOf(User.ROLE_AUDITOR), result.getRole());
    }

    @Test
    @DisplayName("updateUser - 用户名被他人占用抛出异常")
    void testUpdateUserDuplicateUsername() {
        User anotherUser = User.builder()
            .userId("u002").username("taken").password("hash")
            .role(User.ROLE_ADMIN).build();
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(anotherUser));

        assertThrows(IllegalStateException.class,
            () -> userDomainService.updateUser("u001", "taken", null, null));
    }

    @Test
    @DisplayName("updateUser - 改为自己当前用户名不冲突")
    void testUpdateUserSameUsername() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

        // 改为自己的用户名不应该报冲突
        assertDoesNotThrow(() -> userDomainService.updateUser("u001", "admin", null, null));
    }

    @Test
    @DisplayName("updateUser - 空密码不修改")
    void testUpdateUserEmptyPasswordNotChanged() {
        when(userRepository.findById("u001")).thenReturn(Optional.of(existingUser));

        String oldPassword = existingUser.getPassword();
        User result = userDomainService.updateUser("u001", null, "", null);

        assertEquals(oldPassword, result.getPassword()); // 空密码不修改
    }

    @Test
    @DisplayName("updateUser - 用户不存在抛出异常")
    void testUpdateUserNotFound() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userDomainService.updateUser("ghost", "name", null, null));
    }
}
