package com.sa.promotion.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户模块集成测试
 * 每个测试方法独立构建所需数据
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("用户模块集成测试")
class UserIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ---- 辅助方法：注册一个新用户，返回 userId ----
    private String registerUser(String username, String password, int role) throws Exception {
        String resp = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":" + role + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.userId");
    }

    // ==================== 注册 ====================

    @Test
    @DisplayName("POST /api/user/register - 注册新用户成功")
    void testRegisterSuccess() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user_test\",\"password\":\"test123\",\"role\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("new_user_test"));
    }

    @Test
    @DisplayName("POST /api/user/register - 重复用户名返回400")
    void testRegisterDuplicate() throws Exception {
        // 先注册一个用户
        registerUser("dup_user", "pass123", 1);
        // 再用相同用户名注册
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup_user\",\"password\":\"pass456\",\"role\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 登录 ====================

    @Test
    @DisplayName("POST /api/user/login - 登录成功")
    void testLoginSuccess() throws Exception {
        registerUser("login_user", "mypassword", 1);
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"login_user\",\"password\":\"mypassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("login_user"));
    }

    @Test
    @DisplayName("POST /api/user/login - 密码错误返回400")
    void testLoginInvalidPassword() throws Exception {
        registerUser("bad_pwd_user", "correctpass", 1);
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad_pwd_user\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("GET /api/user/{id} - 查询用户成功")
    void testGetUserSuccess() throws Exception {
        String userId = registerUser("get_user", "testpass", 1);
        mockMvc.perform(get("/api/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    @DisplayName("GET /api/user/{id} - 用户不存在返回404")
    void testGetUserNotFound() throws Exception {
        mockMvc.perform(get("/api/user/nonexist_user_999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("PUT /api/user/update/{id} - 更新用户成功")
    void testUpdateUserSuccess() throws Exception {
        String userId = registerUser("update_user", "oldpass", 1);
        mockMvc.perform(put("/api/user/update/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"updated_name\",\"role\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("updated_name"));
    }

    // ==================== 登出 ====================

    @Test
    @DisplayName("POST /api/user/logout - 登出成功")
    void testLogoutSuccess() throws Exception {
        String userId = registerUser("logout_user", "test123", 1);
        mockMvc.perform(post("/api/user/logout")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
