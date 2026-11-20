package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.ActivityStatusService;
import com.campus.eventsys.service.CategoryService;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 管理员活动管理控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class AdminActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityStatusService statusService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ActivityRegistrationService registrationService;

    @Mock
    private ActivityResultService activityResultService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminActivityController adminActivityController;

    private User adminUser;
    private User organizer;
    private Activity testActivity;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long ADMIN_ID = 1L;
    private static final Long ORGANIZER_ID = 2L;
    private static final String ADMIN_USERNAME = "admin";

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(ADMIN_ID);
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setNickname("管理员");
        adminUser.setRole(1); // 管理员

        organizer = new User();
        organizer.setId(ORGANIZER_ID);
        organizer.setUsername("organizer");
        organizer.setNickname("组织者");

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(1); // 待审核
        testActivity.setOrganizerId(ORGANIZER_ID);
    }

    @Test
    void getPendingActivities_Success() {
        // 准备
        Activity activity1 = new Activity();
        activity1.setId(1L);
        activity1.setTitle("活动1");
        activity1.setStatus(1);
        activity1.setOrganizerId(ORGANIZER_ID);

        Activity activity2 = new Activity();
        activity2.setId(2L);
        activity2.setTitle("活动2");
        activity2.setStatus(1);
        activity2.setOrganizerId(ORGANIZER_ID);

        List<Activity> activities = Arrays.asList(activity1, activity2);
        Page<Activity> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(activities);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
        when(userService.getById(ORGANIZER_ID)).thenReturn(organizer);

        // 执行
        Result<Page<AdminActivityController.ActivityWithOrganizerVO>> result = 
                adminActivityController.getPendingActivities(1, 10);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getTotal());
    }

    @Test
    void list_AllActivities() {
        // 准备
        Activity activity1 = new Activity();
        activity1.setId(1L);
        activity1.setTitle("活动1");
        activity1.setStatus(2);
        activity1.setOrganizerId(ORGANIZER_ID);

        List<Activity> activities = Collections.singletonList(activity1);
        Page<Activity> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(activities);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
        when(userService.getById(ORGANIZER_ID)).thenReturn(organizer);

        // 执行
        Result<Page<AdminActivityController.ActivityWithOrganizerVO>> result = 
                adminActivityController.list(1, 10, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_WithStatusFilter() {
        // 准备
        Integer status = 2;
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setTitle("活动");
        activity.setStatus(status);
        activity.setOrganizerId(ORGANIZER_ID);

        List<Activity> activities = Collections.singletonList(activity);
        Page<Activity> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(activities);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
        when(userService.getById(ORGANIZER_ID)).thenReturn(organizer);

        // 执行
        Result<Page<AdminActivityController.ActivityWithOrganizerVO>> result = 
                adminActivityController.list(1, 10, status, null);

        // 验证
        assertEquals(200, result.getCode());
    }

    @Test
    void list_WithKeyword() {
        // 准备
        String keyword = "测试";
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setTitle("测试活动");
        activity.setStatus(2);
        activity.setOrganizerId(ORGANIZER_ID);

        List<Activity> activities = Collections.singletonList(activity);
        Page<Activity> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(activities);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
        when(userService.getById(ORGANIZER_ID)).thenReturn(organizer);

        // 执行
        Result<Page<AdminActivityController.ActivityWithOrganizerVO>> result = 
                adminActivityController.list(1, 10, null, keyword);

        // 验证
        assertEquals(200, result.getCode());
    }

    @Test
    void approve_Success() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(authentication.getName()).thenReturn(ADMIN_USERNAME);
        when(userService.getByUsername(ADMIN_USERNAME)).thenReturn(adminUser);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);
        when(notificationService.sendAuditResult(any(), any(), any(), anyBoolean(), any())).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.approve(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("审核通过", result.getMessage());

        // 验证状态被更新为2
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityService).updateById(activityCaptor.capture());
        assertEquals(2, activityCaptor.getValue().getStatus());

        // 验证发送通知
        verify(notificationService).sendAuditResult(ORGANIZER_ID, ACTIVITY_ID, "测试活动", true, null);
    }

    @Test
    void approve_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.approve(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void reject_Success() {
        // 准备
        String reason = "不符合要求";
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);
        when(notificationService.sendAuditResult(any(), any(), any(), anyBoolean(), any())).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.reject(ACTIVITY_ID, reason);

        // 验证
        assertEquals(200, result.getCode());
        assertTrue(result.getMessage().startsWith("已拒绝"));

        // 验证状态被更新为-1
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityService).updateById(activityCaptor.capture());
        assertEquals(-1, activityCaptor.getValue().getStatus());

        // 验证发送通知
        verify(notificationService).sendAuditResult(ORGANIZER_ID, ACTIVITY_ID, "测试活动", false, reason);
    }

    @Test
    void reject_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.reject(ACTIVITY_ID, "原因");

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void takeDown_Success() {
        // 准备
        testActivity.setStatus(2); // 报名中
        ActivityRegistration reg = new ActivityRegistration();
        reg.setId(1L);
        reg.setActivityId(ACTIVITY_ID);
        reg.setUserId(3L);

        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(registrationService.getActivityParticipants(ACTIVITY_ID)).thenReturn(Collections.singletonList(reg));
        when(activityService.updateById(any(Activity.class))).thenReturn(true);
        User mockOrganizer = new User();
        mockOrganizer.setId(2L);
        mockOrganizer.setOrganizedCount(1);
        when(userService.getById(2L)).thenReturn(mockOrganizer);
        User mockParticipant = new User();
        mockParticipant.setId(3L);
        mockParticipant.setParticipatedCount(1);
        when(userService.getById(3L)).thenReturn(mockParticipant);
        when(activityResultService.takeDownByActivityId(ACTIVITY_ID)).thenReturn(true);

        // 执行
        Result<Void> result = adminActivityController.takeDown(ACTIVITY_ID, "违规内容");

        // 验证
        assertEquals(200, result.getCode());

        // 验证状态被更新为-2
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityService).updateById(activityCaptor.capture());
        assertEquals(-2, activityCaptor.getValue().getStatus());
    }

    @Test
    void takeDown_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.takeDown(ACTIVITY_ID, "原因");

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void delete_Success() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        User mockOrganizer = new User();
        mockOrganizer.setId(ORGANIZER_ID);
        mockOrganizer.setOrganizedCount(1);
        when(userService.getById(ORGANIZER_ID)).thenReturn(mockOrganizer);
        when(activityService.removeById(ACTIVITY_ID)).thenReturn(true);
        when(activityResultService.removeByActivityId(ACTIVITY_ID)).thenReturn(true);
        when(registrationService.getActivityParticipants(ACTIVITY_ID)).thenReturn(Collections.emptyList());

        // 执行
        Result<Void> result = adminActivityController.delete(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
    }

    @Test
    void delete_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = adminActivityController.delete(ACTIVITY_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }
}
