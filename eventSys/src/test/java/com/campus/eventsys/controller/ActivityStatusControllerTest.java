package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.ActivityStatusService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动状态管理控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityStatusControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private ActivityStatusService statusService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityStatusController activityStatusController;

    private User testUser;
    private Activity testActivity;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setRole(2);

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(2);
        testActivity.setStartTime(LocalDateTime.now().plusHours(1));
        testActivity.setEndTime(LocalDateTime.now().plusHours(3));
    }

    @Test
    void getStatusInfo_Success() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(statusService.getExpectedStatus(testActivity)).thenReturn(2);

        // 执行
        Result<ActivityStatusController.StatusInfo> result = activityStatusController.getStatusInfo(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(ACTIVITY_ID, result.getData().getActivityId());
        assertEquals(2, result.getData().getCurrentStatus());
        assertEquals("报名中", result.getData().getCurrentStatusName());
        assertFalse(result.getData().getNeedUpdate());
    }

    @Test
    void getStatusInfo_NeedUpdate() {
        // 准备
        testActivity.setStatus(2);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(statusService.getExpectedStatus(testActivity)).thenReturn(4); // 预期进行中

        // 执行
        Result<ActivityStatusController.StatusInfo> result = activityStatusController.getStatusInfo(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getNeedUpdate());
    }

    @Test
    void getStatusInfo_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<ActivityStatusController.StatusInfo> result = activityStatusController.getStatusInfo(ACTIVITY_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void startActivity_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(statusService.startActivity(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityStatusController.startActivity(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("活动已开始", result.getMessage());
    }

    @Test
    void startActivity_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityStatusController.startActivity(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void startActivity_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityStatusController.startActivity(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void startActivity_InvalidStatus() {
        // 准备
        testActivity.setStatus(4); // 进行中
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);

        // 执行
        Result<Void> result = activityStatusController.startActivity(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("活动状态不允许开始"));
    }

    @Test
    void startActivity_NoPermission() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(statusService.startActivity(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityStatusController.startActivity(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("开始活动失败，您可能没有权限", result.getMessage());
    }

    @Test
    void refreshStatus_Success() {
        // 准备
        Activity updatedActivity = new Activity();
        updatedActivity.setId(ACTIVITY_ID);
        updatedActivity.setStatus(3);
        updatedActivity.setStartTime(LocalDateTime.now().plusMinutes(10));
        updatedActivity.setEndTime(LocalDateTime.now().plusHours(2));

        when(statusService.autoUpdateStatus(ACTIVITY_ID)).thenReturn(updatedActivity);
        when(statusService.getExpectedStatus(updatedActivity)).thenReturn(3);

        // 执行
        Result<ActivityStatusController.StatusInfo> result = activityStatusController.refreshStatus(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getCurrentStatus());
        assertEquals("即将开始", result.getData().getCurrentStatusName());
        assertFalse(result.getData().getNeedUpdate());
    }

    @Test
    void refreshStatus_ActivityNotFound() {
        // 准备
        when(statusService.autoUpdateStatus(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<ActivityStatusController.StatusInfo> result = activityStatusController.refreshStatus(ACTIVITY_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void refreshAllStatus_Success() {
        // 准备
        doNothing().when(statusService).checkAndUpdateAllActivities();

        // 执行
        Result<Void> result = activityStatusController.refreshAllStatus();

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("批量刷新完成", result.getMessage());
        verify(statusService).checkAndUpdateAllActivities();
    }

    @Test
    void getStatusName_AllStatuses() {
        // 验证所有状态码的名称映射
        assertEquals("已下架", invokeGetStatusName(-2));
        assertEquals("已拒绝", invokeGetStatusName(-1));
        assertEquals("草稿", invokeGetStatusName(0));
        assertEquals("待审核", invokeGetStatusName(1));
        assertEquals("报名中", invokeGetStatusName(2));
        assertEquals("即将开始", invokeGetStatusName(3));
        assertEquals("进行中", invokeGetStatusName(4));
        assertEquals("已结束", invokeGetStatusName(5));
        assertEquals("未知", invokeGetStatusName(99));
        assertEquals("未知", invokeGetStatusName(null));
    }

    // 辅助方法调用私有方法
    private String invokeGetStatusName(Integer status) {
        try {
            java.lang.reflect.Method method = ActivityStatusController.class.getDeclaredMethod("getStatusName", Integer.class);
            method.setAccessible(true);
            return (String) method.invoke(activityStatusController, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
