package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.CheckInRecord;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CheckInRecordService;
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
 * 活动签到控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityCheckInControllerTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityPermissionService permissionService;

    @Mock
    private CheckInRecordService checkInRecordService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityCheckInController activityCheckInController;

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

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(4); // 进行中
    }

    @Test
    void checkIn_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.canCheckIn(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(checkInRecordService.checkIn(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityCheckInController.checkIn(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("签到成功", result.getMessage());
        verify(checkInRecordService).checkIn(ACTIVITY_ID, USER_ID);
    }

    @Test
    void checkIn_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityCheckInController.checkIn(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void checkIn_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityCheckInController.checkIn(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void checkIn_NoPermission() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.canCheckIn(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityCheckInController.checkIn(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无法签到：您不是该活动的参与者，或活动尚未开始", result.getMessage());
    }

    @Test
    void checkIn_AlreadyCheckedIn() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.canCheckIn(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(checkInRecordService.checkIn(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityCheckInController.checkIn(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("签到失败：可能已签到", result.getMessage());
    }

    @Test
    void checkInStatus_HasCheckedIn() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(checkInRecordService.hasCheckedIn(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Boolean> result = activityCheckInController.checkInStatus(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertTrue(result.getData());
    }

    @Test
    void checkInStatus_NotCheckedIn() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(checkInRecordService.hasCheckedIn(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Boolean> result = activityCheckInController.checkInStatus(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertFalse(result.getData());
    }

    @Test
    void getActivityCheckInList_Success() {
        // 准备
        CheckInRecord record1 = new CheckInRecord();
        record1.setId(1L);
        record1.setActivityId(ACTIVITY_ID);
        record1.setUserId(USER_ID);

        CheckInRecord record2 = new CheckInRecord();
        record2.setId(2L);
        record2.setActivityId(ACTIVITY_ID);
        record2.setUserId(2L);

        List<CheckInRecord> records = Arrays.asList(record1, record2);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(checkInRecordService.getActivityCheckInRecords(ACTIVITY_ID)).thenReturn(records);

        // 执行
        Result<List<CheckInRecord>> result = activityCheckInController.getActivityCheckInList(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getActivityCheckInList_NotOrganizer() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<List<CheckInRecord>> result = activityCheckInController.getActivityCheckInList(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有活动组织者可以查看签到列表", result.getMessage());
    }

    @Test
    void getMyCheckInRecords_Success() {
        // 准备
        CheckInRecord record = new CheckInRecord();
        record.setId(1L);
        record.setActivityId(ACTIVITY_ID);
        record.setUserId(USER_ID);

        List<CheckInRecord> records = Collections.singletonList(record);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(checkInRecordService.getUserCheckInRecords(USER_ID)).thenReturn(records);

        // 执行
        Result<List<CheckInRecord>> result = activityCheckInController.getMyCheckInRecords(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getMyCheckInRecords_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<List<CheckInRecord>> result = activityCheckInController.getMyCheckInRecords(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }
}
