package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Notification;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 通知控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    private User testUser;
    private Notification testNotification;
    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setRole(0);

        testNotification = new Notification();
        testNotification.setId(NOTIFICATION_ID);
        testNotification.setUserId(USER_ID);
        testNotification.setTitle("测试通知");
        testNotification.setContent("测试内容");
        testNotification.setType(1);
        testNotification.setIsRead(0);
    }

    @Test
    void list_Success() {
        // 准备
        Page<Notification> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Collections.singletonList(testNotification));

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.getUserNotifications(USER_ID, 0, false, 1, 10)).thenReturn(mockPage);

        // 执行
        Result<Page<Notification>> result = notificationController.list(authentication, 1, 10);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void list_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Page<Notification>> result = notificationController.list(authentication, 1, 10);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void unreadCount_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.getUnreadCount(USER_ID, 0, false)).thenReturn(5L);

        // 执行
        Result<NotificationController.UnreadCountVO> result = notificationController.unreadCount(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(5L, result.getData().getCount());
    }

    @Test
    void unreadCount_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<NotificationController.UnreadCountVO> result = notificationController.unreadCount(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void markAsRead_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.markAsRead(NOTIFICATION_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = notificationController.markAsRead(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("标记已读成功", result.getMessage());
    }

    @Test
    void markAsRead_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = notificationController.markAsRead(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void markAsRead_NotFoundOrNoPermission() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.markAsRead(NOTIFICATION_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = notificationController.markAsRead(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("通知不存在或无权限", result.getMessage());
    }

    @Test
    void markAllAsRead_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.markAllAsRead(USER_ID)).thenReturn(3);

        // 执行
        Result<NotificationController.ReadAllVO> result = notificationController.markAllAsRead(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getMarkedCount());
    }

    @Test
    void markAllAsRead_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<NotificationController.ReadAllVO> result = notificationController.markAllAsRead(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void delete_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.deleteNotification(NOTIFICATION_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = notificationController.delete(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
    }

    @Test
    void delete_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = notificationController.delete(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void delete_NotFoundOrNoPermission() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(notificationService.deleteNotification(NOTIFICATION_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = notificationController.delete(authentication, NOTIFICATION_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("通知不存在或无权限", result.getMessage());
    }

    @Test
    void sendAnnouncement_Success() {
        // 准备
        String title = "系统公告";
        String content = "公告内容";
        Notification announcement = new Notification();
        announcement.setId(100L);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(1);

        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(1);

        when(authentication.getName()).thenReturn("admin");
        when(userService.getByUsername("admin")).thenReturn(admin);
        when(notificationService.createAnnouncement(99L, title, content, "ALL", false, null, 1, "SYSTEM"))
                .thenReturn(announcement);

        // 执行
        Result<Notification> result = notificationController.sendAnnouncement(
                authentication, title, content, "ALL", false, null, 1, "SYSTEM");

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("保存成功", result.getMessage());
        assertNotNull(result.getData());
        assertEquals(title, result.getData().getTitle());
    }
}
