package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CategoryService;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * 活动管理控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityManageControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityPermissionService permissionService;

    @Mock
    private ActivityRegistrationService registrationService;

    @Mock
    private ActivityResultService activityResultService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityManageController activityManageController;

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
        testUser.setOrganizedCount(5);

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(2);
        testActivity.setOrganizerId(USER_ID);
        testActivity.setCategoryId(1L);
    }

    @Test
    void create_Success() {
        // 准备
        Activity newActivity = new Activity();
        newActivity.setTitle("新活动");
        newActivity.setCategoryId(1L);

        Activity createdActivity = new Activity();
        createdActivity.setId(10L);
        createdActivity.setTitle("新活动");
        createdActivity.setOrganizerId(USER_ID);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getOne(any())).thenReturn(createdActivity);
        doNothing().when(categoryService).incrementActivityCount(1L);

        // 执行
        Result<Long> result = activityManageController.create(newActivity, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(10L, result.getData());
        verify(activityService).save(any(Activity.class));
        verify(permissionService).setUserAsOrganizer(10L, USER_ID);
        verify(userService).updateById(any(User.class));
    }

    @Test
    void create_UserNotFound() {
        // 准备
        Activity newActivity = new Activity();
        newActivity.setTitle("新活动");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Long> result = activityManageController.create(newActivity, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void update_Success() {
        // 准备
        Activity updateActivity = new Activity();
        updateActivity.setTitle("更新后的标题");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.canModifyActivity(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);

        // 执行
        Result<Void> result = activityManageController.update(ACTIVITY_ID, updateActivity, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("更新成功", result.getMessage());
    }

    @Test
    void update_ActivityNotFound() {
        // 准备
        Activity updateActivity = new Activity();
        updateActivity.setTitle("更新后的标题");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityManageController.update(ACTIVITY_ID, updateActivity, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void update_NoPermission() {
        // 准备
        Activity updateActivity = new Activity();
        updateActivity.setTitle("更新后的标题");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.canModifyActivity(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityManageController.update(ACTIVITY_ID, updateActivity, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有活动组织者可以修改活动信息", result.getMessage());
    }

    @Test
    void delete_Success() {
        // 准备
        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(1L);
        registration.setActivityId(ACTIVITY_ID);
        registration.setUserId(2L);

        User mockParticipant = new User();
        mockParticipant.setId(2L);
        mockParticipant.setParticipatedCount(1);
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(registrationService.getActivityParticipants(ACTIVITY_ID)).thenReturn(Collections.singletonList(registration));
        when(userService.getById(2L)).thenReturn(mockParticipant);
        when(userService.getById(1L)).thenReturn(testUser);

        // 执行
        Result<Void> result = activityManageController.delete(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
        verify(activityService).removeById(ACTIVITY_ID);
        verify(categoryService).decrementActivityCount(any());
    }

    @Test
    void delete_NotOrganizer() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityManageController.delete(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有活动组织者可以删除活动", result.getMessage());
    }

    @Test
    void getMyCreatedActivities_Success() {
        // 准备
        Activity activity1 = new Activity();
        activity1.setId(1L);
        activity1.setTitle("活动1");

        Activity activity2 = new Activity();
        activity2.setId(2L);
        activity2.setTitle("活动2");

        List<Activity> activities = Arrays.asList(activity1, activity2);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(activities);

        // 执行
        Result<List<Activity>> result = activityManageController.getMyCreatedActivities(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getMyCreatedActivities_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<List<Activity>> result = activityManageController.getMyCreatedActivities(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void getActivityRegistrations_Success() {
        // 准备
        ActivityRegistration reg = new ActivityRegistration();
        reg.setId(1L);
        reg.setActivityId(ACTIVITY_ID);
        reg.setUserId(2L);
        reg.setRole(2);
        reg.setStatus(1);

        User participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");
        participant.setRealName("参与者");
        participant.setStudentId("2021002");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(registrationService.getActivityRegistrations(ACTIVITY_ID)).thenReturn(Collections.singletonList(reg));
        when(userService.getById(2L)).thenReturn(participant);

        // 执行
        Result<List<ActivityManageController.RegistrationDetail>> result = 
                activityManageController.getActivityRegistrations(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getActivityRegistrations_NotOrganizer() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<List<ActivityManageController.RegistrationDetail>> result = 
                activityManageController.getActivityRegistrations(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有活动组织者可以查看报名列表", result.getMessage());
    }

    @Test
    void getActivityParticipants_Success() {
        // 准备
        ActivityRegistration reg = new ActivityRegistration();
        reg.setId(1L);
        reg.setActivityId(ACTIVITY_ID);
        reg.setUserId(2L);
        reg.setRole(2);
        reg.setStatus(1);

        User participant = new User();
        participant.setId(2L);
        participant.setUsername("participant");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(registrationService.getActivityParticipants(ACTIVITY_ID)).thenReturn(Collections.singletonList(reg));
        when(userService.getById(2L)).thenReturn(participant);

        // 执行
        Result<List<ActivityManageController.RegistrationDetail>> result = 
                activityManageController.getActivityParticipants(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }
}
