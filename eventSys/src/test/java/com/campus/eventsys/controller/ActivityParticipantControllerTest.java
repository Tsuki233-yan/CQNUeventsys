package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动参与者控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityParticipantControllerTest {

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
    private Authentication authentication;

    @InjectMocks
    private ActivityParticipantController activityParticipantController;

    private User testUser;
    private Activity testActivity;
    private ActivityRegistration testRegistration;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setParticipatedCount(3);

        testActivity = new Activity();
        testActivity.setId(ACTIVITY_ID);
        testActivity.setTitle("测试活动");
        testActivity.setStatus(2);
        testActivity.setCurrentParticipants(10);

        testRegistration = new ActivityRegistration();
        testRegistration.setId(1L);
        testRegistration.setActivityId(ACTIVITY_ID);
        testRegistration.setUserId(USER_ID);
        testRegistration.setRole(2);
        testRegistration.setStatus(1);

        // 默认桩：用户已绑定身份（个别用例可覆盖为未绑定）；lenient 避免未触达身份检查的用例报多余打桩
        lenient().when(userService.isIdentityBound(testUser)).thenReturn(true);
    }

    @Test
    void joinActivity_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);
        when(permissionService.addParticipantWithImages(any(), any(), any(), any(), any())).thenReturn(true);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(ACTIVITY_ID, "备注", null, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("报名成功", result.getMessage());
        verify(permissionService).addParticipantWithImages(ACTIVITY_ID, USER_ID, ActivityRole.PARTICIPANT, "备注", null);
    }

    @Test
    void joinActivity_IdentityNotBound() {
        // 准备：未绑定学号/工号的用户不允许报名（覆盖 setUp 的默认桩）
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(userService.isIdentityBound(testUser)).thenReturn(false);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(ACTIVITY_ID, null, null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("请先完善身份认证（绑定学号/工号）后再报名活动", result.getMessage());
        verify(permissionService, never()).addParticipantWithImages(any(), any(), any(), any(), any());
    }

    @Test
    void joinActivity_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(ACTIVITY_ID, null, null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void joinActivity_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(ACTIVITY_ID, null, null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void joinActivity_AlreadyJoined() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(testRegistration);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(ACTIVITY_ID, null, null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("您已报名该活动", result.getMessage());
    }

    @Test
    void joinActivity_TooManyImages() {
        // 准备
        MockMultipartFile image1 = new MockMultipartFile("images", "test1.jpg", "image/jpeg", new byte[100]);
        MockMultipartFile image2 = new MockMultipartFile("images", "test2.jpg", "image/jpeg", new byte[100]);
        MockMultipartFile image3 = new MockMultipartFile("images", "test3.jpg", "image/jpeg", new byte[100]);
        MockMultipartFile image4 = new MockMultipartFile("images", "test4.jpg", "image/jpeg", new byte[100]);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(
                ACTIVITY_ID, null, java.util.Arrays.asList(image1, image2, image3, image4), authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("报名图片最多上传3张", result.getMessage());
    }

    @Test
    void joinActivity_InvalidFileType() {
        // 准备
        MockMultipartFile textFile = new MockMultipartFile("images", "test.txt", "text/plain", new byte[100]);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(
                ACTIVITY_ID, null, Collections.singletonList(textFile), authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("只能上传图片文件"));
    }

    @Test
    void joinActivity_ImageTooLarge() {
        // 准备 - 11MB 的图片
        byte[] largeImage = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("images", "large.jpg", "image/jpeg", largeImage);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.joinActivity(
                ACTIVITY_ID, null, Collections.singletonList(largeFile), authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("图片大小不能超过"));
    }

    @Test
    void cancelJoin_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(testRegistration);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(registrationService.removeById(1L)).thenReturn(true);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);
        when(userService.updateById(any(User.class))).thenReturn(true);

        // 执行
        Result<Void> result = activityParticipantController.cancelJoin(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("取消报名成功", result.getMessage());
        verify(registrationService).removeById(1L);
    }

    @Test
    void cancelJoin_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.cancelJoin(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void cancelJoin_NotJoined() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityParticipantController.cancelJoin(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("您未报名该活动", result.getMessage());
    }

    @Test
    void getActivityRole_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        testActivity.setDeleted(0);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(testRegistration);
        when(permissionService.canModifyActivity(ACTIVITY_ID, USER_ID)).thenReturn(false);
        when(permissionService.canCheckIn(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<ActivityParticipantController.ActivityRoleInfo> result = activityParticipantController.getActivityRole(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getRole());
    }

    @Test
    void getActivityRole_NoRole() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(permissionService.getUserRole(ACTIVITY_ID, USER_ID)).thenReturn(null);

        // 执行
        Result<ActivityParticipantController.ActivityRoleInfo> result = activityParticipantController.getActivityRole(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNull(result.getData().getRole());
    }

    @Test
    void getActivityRole_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<ActivityParticipantController.ActivityRoleInfo> result = activityParticipantController.getActivityRole(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void getMyActivities_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        testActivity.setDeleted(0);
        when(registrationService.getUserParticipantRegistrations(USER_ID)).thenReturn(Collections.singletonList(testRegistration));
        when(activityService.listByIds(any())).thenReturn(Collections.singletonList(testActivity));

        // 执行
        Result<java.util.List<Activity>> result = activityParticipantController.getMyActivities(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getMyActivities_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<java.util.List<Activity>> result = activityParticipantController.getMyActivities(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }
}
