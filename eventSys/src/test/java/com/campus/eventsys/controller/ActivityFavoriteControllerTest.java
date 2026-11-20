package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityFavorite;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityFavoriteService;
import com.campus.eventsys.service.ActivityService;
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
 * 活动收藏控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityFavoriteControllerTest {

    @Mock
    private ActivityFavoriteService favoriteService;

    @Mock
    private ActivityService activityService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityFavoriteController activityFavoriteController;

    private User testUser;
    private Activity testActivity;
    private ActivityFavorite testFavorite;
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
        testActivity.setStatus(2);

        testFavorite = new ActivityFavorite();
        testFavorite.setId(1L);
        testFavorite.setActivityId(ACTIVITY_ID);
        testFavorite.setUserId(USER_ID);
    }

    @Test
    void favorite_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(false);
        when(favoriteService.favorite(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityFavoriteController.favorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("收藏成功", result.getMessage());
        verify(favoriteService).favorite(ACTIVITY_ID, USER_ID);
    }

    @Test
    void favorite_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityFavoriteController.favorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void favorite_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Void> result = activityFavoriteController.favorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void favorite_AlreadyFavorited() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityFavoriteController.favorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("您已收藏该活动", result.getMessage());
    }

    @Test
    void favorite_Failed() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(false);
        when(favoriteService.favorite(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityFavoriteController.favorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("收藏失败", result.getMessage());
    }

    @Test
    void unfavorite_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(favoriteService.unfavorite(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Void> result = activityFavoriteController.unfavorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("取消收藏成功", result.getMessage());
        verify(favoriteService).unfavorite(ACTIVITY_ID, USER_ID);
    }

    @Test
    void unfavorite_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = activityFavoriteController.unfavorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void unfavorite_NotFavorited() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityFavoriteController.unfavorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("您未收藏该活动", result.getMessage());
    }

    @Test
    void unfavorite_Failed() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(favoriteService.unfavorite(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = activityFavoriteController.unfavorite(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("取消收藏失败", result.getMessage());
    }

    @Test
    void isFavorited_True() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(true);

        // 执行
        Result<Boolean> result = activityFavoriteController.isFavorited(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertTrue(result.getData());
    }

    @Test
    void isFavorited_False() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.isFavorited(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Boolean> result = activityFavoriteController.isFavorited(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertFalse(result.getData());
    }

    @Test
    void isFavorited_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Boolean> result = activityFavoriteController.isFavorited(ACTIVITY_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void getMyFavorites_Success() {
        // 准备
        ActivityFavorite favorite1 = new ActivityFavorite();
        favorite1.setId(1L);
        favorite1.setActivityId(1L);
        favorite1.setUserId(USER_ID);

        ActivityFavorite favorite2 = new ActivityFavorite();
        favorite2.setId(2L);
        favorite2.setActivityId(2L);
        favorite2.setUserId(USER_ID);

        List<ActivityFavorite> favorites = Arrays.asList(favorite1, favorite2);

        Activity activity1 = new Activity();
        activity1.setId(1L);
        activity1.setTitle("活动1");

        Activity activity2 = new Activity();
        activity2.setId(2L);
        activity2.setTitle("活动2");

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.getUserFavorites(USER_ID)).thenReturn(favorites);
        when(activityService.getById(1L)).thenReturn(activity1);
        when(activityService.getById(2L)).thenReturn(activity2);

        // 执行
        Result<List<Activity>> result = activityFavoriteController.getMyFavorites(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getMyFavorites_Empty() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.getUserFavorites(USER_ID)).thenReturn(Collections.emptyList());

        // 执行
        Result<List<Activity>> result = activityFavoriteController.getMyFavorites(authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
    }

    @Test
    void getMyFavorites_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<List<Activity>> result = activityFavoriteController.getMyFavorites(authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void getMyFavorites_WithNullActivity() {
        // 准备 - 测试收藏中存在但活动已被删除的情况
        ActivityFavorite favorite = new ActivityFavorite();
        favorite.setId(1L);
        favorite.setActivityId(999L);
        favorite.setUserId(USER_ID);

        List<ActivityFavorite> favorites = Collections.singletonList(favorite);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(favoriteService.getUserFavorites(USER_ID)).thenReturn(favorites);
        when(activityService.getById(999L)).thenReturn(null);

        // 执行
        Result<List<Activity>> result = activityFavoriteController.getMyFavorites(authentication);

        // 验证 - null 的活动应该被过滤掉
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
    }

    @Test
    void getFavoriteCount_Success() {
        // 准备
        when(favoriteService.getFavoriteCount(ACTIVITY_ID)).thenReturn(100L);

        // 执行
        Result<Long> result = activityFavoriteController.getFavoriteCount(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(100L, result.getData());
    }

    @Test
    void getFavoriteCount_Zero() {
        // 准备
        when(favoriteService.getFavoriteCount(ACTIVITY_ID)).thenReturn(0L);

        // 执行
        Result<Long> result = activityFavoriteController.getFavoriteCount(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(0L, result.getData());
    }
}
