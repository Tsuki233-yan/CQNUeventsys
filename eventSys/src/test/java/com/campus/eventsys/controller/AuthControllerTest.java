package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.security.JwtUtil;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 认证控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
    }

    @Test
    void login_Success() {
        // 准备
        AuthController.LoginRequest request = new AuthController.LoginRequest(USERNAME, PASSWORD);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(USERNAME)).thenReturn(TOKEN);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<Map<String, Object>> result = authController.login(request);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("登录成功", result.getMessage());
        assertNotNull(result.getData());
        assertEquals(TOKEN, result.getData().get("token"));
        assertEquals(testUser, result.getData().get("user"));
    }

    @Test
    void register_Success() {
        // 准备
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("newpass123");
        newUser.setNickname("新用户");

        when(userService.register(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = authController.register(newUser);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("注册成功", result.getMessage());
        verify(userService).register(newUser);
    }

    @Test
    void getUserInfo_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<User> result = authController.getUserInfo(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(USERNAME, result.getData().getUsername());
    }

    @Test
    void uploadAvatar_Success() {
        // 准备
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpeg", new byte[1000]);
        String avatarUrl = "/uploads/avatars/test.jpg";

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(userService.uploadAvatar(testUser.getId(), avatarFile)).thenReturn(avatarUrl);

        // 执行
        Result<String> result = authController.uploadAvatar(avatarFile, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("头像上传成功", result.getMessage());
        assertEquals(avatarUrl, result.getData());
    }

    @Test
    void uploadAvatar_UserNotFound() {
        // 准备
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpeg", new byte[1000]);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<String> result = authController.uploadAvatar(avatarFile, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void uploadAvatar_EmptyFile() {
        // 准备
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpeg", new byte[0]);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<String> result = authController.uploadAvatar(emptyFile, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("请选择要上传的头像图片", result.getMessage());
    }

    @Test
    void uploadAvatar_NullFile() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<String> result = authController.uploadAvatar(null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("请选择要上传的头像图片", result.getMessage());
    }

    @Test
    void uploadAvatar_RuntimeException() {
        // 准备
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpeg", new byte[1000]);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(userService.uploadAvatar(testUser.getId(), avatarFile))
                .thenThrow(new RuntimeException("文件上传失败"));

        // 执行
        Result<String> result = authController.uploadAvatar(avatarFile, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("文件上传失败", result.getMessage());
    }

    @Test
    void updateUserInfo_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(userService.updateUserInfo(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = authController.updateUserInfo(
                "新昵称", "new@email.com", "13800138000",
                "真实姓名", "学院", "专业", "2021", 1, "简介",
                authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("更新成功", result.getMessage());
    }

    @Test
    void updateUserInfo_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = authController.updateUserInfo(
                "新昵称", null, null, null, null, null, null, null, null,
                authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void updateUserInfo_Failed() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(userService.updateUserInfo(any(User.class))).thenReturn(false);

        // 执行
        Result<Void> result = authController.updateUserInfo(
                "新昵称", null, null, null, null, null, null, null, null,
                authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("更新失败", result.getMessage());
    }
}
