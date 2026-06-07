package com.sa.promotion.interfaces.controller;

import com.sa.promotion.application.dto.request.LoginRequest;
import com.sa.promotion.application.dto.request.RegisterRequest;
import com.sa.promotion.application.dto.request.UpdateUserRequest;
import com.sa.promotion.application.service.UserAppService;
import com.sa.promotion.domain.exception.ResourceNotFoundException;
import com.sa.promotion.domain.user.entity.User;
import com.sa.promotion.interfaces.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("用户控制器测试")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserAppService userAppService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userAppService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/user/login - 登录成功")
    void testLoginSuccess() throws Exception {
        User user = User.builder().userId("u001").username("admin").build();
        when(userAppService.login("admin", "pass123")).thenReturn(user);

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"pass123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value("u001"));
    }

    @Test
    @DisplayName("POST /api/user/login - 登录失败返回400")
    void testLoginFailed() throws Exception {
        when(userAppService.login("admin", "wrong"))
            .thenThrow(new IllegalArgumentException("Invalid password"));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/user/register - 注册成功")
    void testRegisterSuccess() throws Exception {
        User user = User.builder().userId("u010").username("newUser").build();
        when(userAppService.register("newUser", "pass", 1)).thenReturn(user);

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newUser\",\"password\":\"pass\",\"role\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value("u010"));
    }

    @Test
    @DisplayName("POST /api/user/register - 用户名已存在返回400")
    void testRegisterDuplicate() throws Exception {
        when(userAppService.register("admin", "pass", 1))
            .thenThrow(new IllegalStateException("Username already exists: admin"));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"pass\",\"role\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/user/logout - 登出成功")
    void testLogoutSuccess() throws Exception {
        doNothing().when(userAppService).logout("u001");

        mockMvc.perform(post("/api/user/logout")
                .param("userId", "u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/user/{id} - 查询用户成功")
    void testGetUserSuccess() throws Exception {
        User user = User.builder().userId("u001").username("admin").build();
        when(userAppService.getUser("u001")).thenReturn(user);

        mockMvc.perform(get("/api/user/u001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("GET /api/user/{id} - 用户不存在返回404")
    void testGetUserNotFound() throws Exception {
        when(userAppService.getUser("nonexist"))
            .thenThrow(new ResourceNotFoundException("User not found: nonexist"));

        mockMvc.perform(get("/api/user/nonexist"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("PUT /api/user/update/{id} - 更新用户成功")
    void testUpdateUserSuccess() throws Exception {
        User user = User.builder().userId("u001").username("updated").build();
        when(userAppService.updateUser(eq("u001"), eq("updated"), isNull(), isNull())).thenReturn(user);

        mockMvc.perform(put("/api/user/update/u001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("updated"));
    }
}
