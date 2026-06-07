package com.sa.promotion.application.service;

import com.sa.promotion.domain.user.entity.User;
import com.sa.promotion.domain.user.service.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("用户应用服务测试")
class UserAppServiceTest {

    private UserAppService userAppService;

    @Mock
    private UserDomainService userDomainService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userAppService = new UserAppService(userDomainService);

        testUser = User.builder()
            .userId("u001")
            .username("admin")
            .password("pass123")
            .role(User.ROLE_ADMIN)
            .build();
    }

    @Test
    @DisplayName("register - 成功注册")
    void testRegister() {
        when(userDomainService.register(anyString(), anyString(), anyInt())).thenReturn(testUser);

        User result = userAppService.register("admin", "pass123", User.ROLE_ADMIN);

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        verify(userDomainService).register("admin", "pass123", User.ROLE_ADMIN);
    }

    @Test
    @DisplayName("login - 成功登录")
    void testLogin() {
        when(userDomainService.login("admin", "pass123")).thenReturn(testUser);

        User result = userAppService.login("admin", "pass123");

        assertNotNull(result);
        assertEquals("u001", result.getUserId());
    }

    @Test
    @DisplayName("logout - 不抛异常")
    void testLogout() {
        when(userDomainService.getUser("u001")).thenReturn(testUser);

        assertDoesNotThrow(() -> userAppService.logout("u001"));
    }

    @Test
    @DisplayName("getUser - 查询用户")
    void testGetUser() {
        when(userDomainService.getUser("u001")).thenReturn(testUser);

        User result = userAppService.getUser("u001");

        assertEquals("admin", result.getUsername());
    }

    @Test
    @DisplayName("updateUser - 更新用户")
    void testUpdateUser() {
        User updated = User.builder().userId("u001").username("newAdmin").password("newpass").role(User.ROLE_AUDITOR).build();
        when(userDomainService.updateUser("u001", "newAdmin", "newpass", User.ROLE_AUDITOR)).thenReturn(updated);

        User result = userAppService.updateUser("u001", "newAdmin", "newpass", User.ROLE_AUDITOR);

        assertEquals("newAdmin", result.getUsername());
        assertEquals(User.ROLE_AUDITOR, result.getRole());
    }
}
