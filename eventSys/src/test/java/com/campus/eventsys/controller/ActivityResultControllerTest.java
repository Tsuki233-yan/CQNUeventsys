package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityResult;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
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
 * 活动成果展示贴控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityResultControllerTest {

    @Mock
    private ActivityResultService resultService;

    @Mock
    private ActivityService activityService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityPermissionService permissionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityRegistrationService registrationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityResultController activityResultController;

    private User testUser;
    private Activity testActivity;
    private ActivityResult testResult;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long RESULT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setRole(2); // 普通用户

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(4);

        testResult = new ActivityResult();
        testResult.setId(RESULT_ID);
        testResult.setActivityId(ACTIVITY_ID);
        testResult.setTitle("成果展示");
        testResult.setContent("活动内容");
        testResult.setCreatorId(USER_ID);
        testResult.setViewCount(10);
        testResult.setStatus(1);
    }

    @Test
    void getByActivityId_Success() {
        // 准备
        when(resultService.getByActivityId(ACTIVITY_ID)).thenReturn(testResult);
        doNothing().when(resultService).incrementViewCount(RESULT_ID);
        when(userService.getById(USER_ID)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);

        // 执行
        Result<ActivityResult> result = activityResultController.getByActivityId(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(RESULT_ID, result.getData().getId());
        verify(resultService).incrementViewCount(RESULT_ID);
    }

    @Test
    void getByActivityId_NotFound() {
        // 准备
        when(resultService.getByActivityId(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<ActivityResult> result = activityResultController.getByActivityId(ACTIVITY_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("该活动暂无成果展示贴", result.getMessage());
    }

    @Test
    void existsByActivityId_True() {
        // 准备
        when(resultService.existsByActivityId(ACTIVITY_ID)).thenReturn(true);

        // 执行
        Result<Boolean> result = activityResultController.existsByActivityId(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertTrue(result.getData());
    }

    @Test
    void existsByActivityId_False() {
        // 准备
        when(resultService.existsByActivityId(ACTIVITY_ID)).thenReturn(false);

        // 执行
        Result<Boolean> result = activityResultController.existsByActivityId(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertFalse(result.getData());
    }

    @Test
    void create_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(resultService.existsByActivityId(ACTIVITY_ID)).thenReturn(false);
        when(resultService.createOrRestore(any(ActivityResult.class))).thenReturn(true);

        // 执行
        Result<Void> result = activityResultController.create(ACTIVITY_ID, "标题", "内容", null, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("创建成功", result.getMessage());
    }

    @Test
    void create_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityResultController.create(ACTIVITY_ID, "标题", "内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void create_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityResultController.create(ACTIVITY_ID, "标题", "内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void create_NoPermission() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityResultController.create(ACTIVITY_ID, "标题", "内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有活动组织者或管理员可以创建成果展示贴", result.getMessage());
    }

    @Test
    void create_AlreadyExists() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(resultService.existsByActivityId(ACTIVITY_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityResultController.create(ACTIVITY_ID, "标题", "内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("该活动已有成果展示贴，请使用更新接口", result.getMessage());
    }

    @Test
    void update_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(testResult);
        when(resultService.updateById(any(ActivityResult.class))).thenReturn(true);

        // 执行
        Result<Void> result = activityResultController.update(RESULT_ID, "新标题", "新内容", null, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("更新成功", result.getMessage());
    }

    @Test
    void update_ResultNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityResultController.update(RESULT_ID, "新标题", "新内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("成果展示贴不存在", result.getMessage());
    }

    @Test
    void update_NoPermission() {
        // 准备
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("other");
        otherUser.setRole(2);

        ActivityResult otherResult = new ActivityResult();
        otherResult.setId(RESULT_ID);
        otherResult.setCreatorId(2L);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(otherResult);

        // 执行
        Result<Void> result = activityResultController.update(RESULT_ID, "新标题", "新内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有创建者或管理员可以更新成果展示贴", result.getMessage());
    }

    @Test
    void delete_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(testResult);
        when(resultService.removeById(RESULT_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityResultController.delete(RESULT_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
    }

    @Test
    void delete_ResultNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityResultController.delete(RESULT_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("成果展示贴不存在", result.getMessage());
    }

    @Test
    void delete_NoPermission() {
        // 准备
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("other");
        otherUser.setRole(2);

        ActivityResult otherResult = new ActivityResult();
        otherResult.setId(RESULT_ID);
        otherResult.setCreatorId(2L);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(resultService.getById(RESULT_ID)).thenReturn(otherResult);

        // 执行
        Result<Void> result = activityResultController.delete(RESULT_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有创建者或管理员可以删除成果展示贴", result.getMessage());
    }
}
