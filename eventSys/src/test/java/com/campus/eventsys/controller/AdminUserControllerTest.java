package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 管理员用户管理控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserController adminUserController;

    private User testUser;
    private User adminUser;
    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setRole(0); // 普通用户
        testUser.setStatus(1); // 正常
        testUser.setPassword("encodedPassword");

        adminUser = new User();
        adminUser.setId(ADMIN_ID);
        adminUser.setUsername("admin");
        adminUser.setNickname("管理员");
        adminUser.setRole(1); // 管理员
        adminUser.setStatus(1);
    }

    @Test
    void list_AllUsers() {
        // 准备
        Page<User> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(Arrays.asList(testUser, adminUser));

        when(userService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<User>> result = adminUserController.list(1, 10, null, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void list_WithKeyword() {
        // 准备
        String keyword = "test";
        Page<User> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Collections.singletonList(testUser));

        when(userService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<User>> result = adminUserController.list(1, 10, keyword, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void list_WithRoleFilter() {
        // 准备
        Integer role = 1;
        Page<User> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Collections.singletonList(adminUser));

        when(userService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<User>> result = adminUserController.list(1, 10, null, role, null);

        // 验证
        assertEquals(200, result.getCode());
    }

    @Test
    void list_WithStatusFilter() {
        // 准备
        Integer status = 1;
        Page<User> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(Arrays.asList(testUser, adminUser));

        when(userService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<User>> result = adminUserController.list(1, 10, null, null, status);

        // 验证
        assertEquals(200, result.getCode());
    }

    @Test
    void detail_Success() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);

        // 执行
        Result<User> result = adminUserController.detail(USER_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(USER_ID, result.getData().getId());
        assertNull(result.getData().getPassword()); // 密码应被隐藏
    }

    @Test
    void detail_UserNotFound() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(null);

        // 执行
        Result<User> result = adminUserController.detail(USER_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void update_Success() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.update(
                USER_ID, "新昵称", "new@email.com", "13800138000", 
                "真实姓名", "学院", "专业", "2021", 1, "简介");

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("更新成功", result.getMessage());

        // 验证字段更新
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals("新昵称", userCaptor.getValue().getNickname());
        assertEquals("new@email.com", userCaptor.getValue().getEmail());
        assertEquals("13800138000", userCaptor.getValue().getPhone());
        assertEquals("真实姓名", userCaptor.getValue().getRealName());
        assertEquals(1, userCaptor.getValue().getGender());
    }

    @Test
    void update_UserNotFound() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminUserController.update(USER_ID, "昵称", null, null, null, null, null, null, null, null);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void resetPassword_Success() {
        // 准备
        String newPassword = "newpass123";
        String encodedPassword = "encodedNewPass";

        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.resetPassword(USER_ID, newPassword);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("密码重置成功", result.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(encodedPassword, userCaptor.getValue().getPassword());
    }

    @Test
    void resetPassword_UserNotFound() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminUserController.resetPassword(USER_ID, "newpass123");

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void resetPassword_TooShort() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);

        // 执行
        Result<Void> result = adminUserController.resetPassword(USER_ID, "123");

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("密码长度不能少于6位", result.getMessage());
    }

    @Test
    void delete_Success() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(userService.removeById(USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.delete(USER_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
    }

    @Test
    void delete_UserNotFound() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminUserController.delete(USER_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void setAdmin_Success() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.setAdmin(USER_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("已设置为管理员", result.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getRole());
    }

    @Test
    void setAdmin_UserNotFound() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminUserController.setAdmin(USER_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void cancelAdmin_Success() {
        // 准备
        when(userService.getById(ADMIN_ID)).thenReturn(adminUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.cancelAdmin(ADMIN_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("已取消管理员权限", result.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(0, userCaptor.getValue().getRole());
    }

    @Test
    void banUser_Success() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.banUser(USER_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("封禁成功", result.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(0, userCaptor.getValue().getStatus());
    }

    @Test
    void banUser_AdminCannotBan() {
        // 准备
        when(userService.getById(ADMIN_ID)).thenReturn(adminUser);

        // 执行
        Result<Void> result = adminUserController.banUser(ADMIN_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("不能封禁管理员，请先取消管理员权限", result.getMessage());
    }

    @Test
    void banUser_AlreadyBanned() {
        // 准备
        testUser.setStatus(0); // 已禁用
        when(userService.getById(USER_ID)).thenReturn(testUser);

        // 执行
        Result<Void> result = adminUserController.banUser(USER_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("该用户已被封禁", result.getMessage());
    }

    @Test
    void unbanUser_Success() {
        // 准备
        testUser.setStatus(0); // 已禁用
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = adminUserController.unbanUser(USER_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("解禁成功", result.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getStatus());
    }

    @Test
    void unbanUser_AlreadyNormal() {
        // 准备
        when(userService.getById(USER_ID)).thenReturn(testUser); // status=1 正常

        // 执行
        Result<Void> result = adminUserController.unbanUser(USER_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("该用户状态正常，无需解禁", result.getMessage());
    }
}
