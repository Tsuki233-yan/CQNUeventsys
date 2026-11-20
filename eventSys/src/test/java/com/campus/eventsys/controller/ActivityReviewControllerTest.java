package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.ActivityReview;
import com.campus.eventsys.entity.ReviewStatisticsVO;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityReviewService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动评价控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityReviewControllerTest {

    @Mock
    private ActivityReviewService reviewService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityPermissionService permissionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityReviewController activityReviewController;

    private User testUser;
    private User adminUser;
    private ActivityReview testReview;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long REVIEW_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 99L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setRole(0); // 普通用户

        adminUser = new User();
        adminUser.setId(ADMIN_ID);
        adminUser.setUsername("admin");
        adminUser.setNickname("管理员");
        adminUser.setRole(1); // 管理员

        testReview = new ActivityReview();
        testReview.setId(REVIEW_ID);
        testReview.setActivityId(ACTIVITY_ID);
        testReview.setUserId(USER_ID);
        testReview.setRating(5);
        testReview.setContent("活动很好！");
    }

    @Test
    void createReview_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(ACTIVITY_ID, USER_ID, 5, "活动很好！")).thenReturn(testReview);

        Map<String, Object> body = new HashMap<>();
        body.put("activityId", ACTIVITY_ID);
        body.put("rating", 5);
        body.put("content", "活动很好！");

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(body, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("评价成功", result.getMessage());
        assertNotNull(result.getData());
        assertEquals(REVIEW_ID, result.getData().get("reviewId"));
    }

    @Test
    void createReview_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        assertThrows(RuntimeException.class, () ->
                activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication));
    }

    @Test
    void createReview_ActivityNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new com.campus.eventsys.common.BusinessException(404, "活动不存在"));

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication);

        // 验证
        assertEquals(404, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void createReview_ActivityNotEnded() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new com.campus.eventsys.common.BusinessException(403, "活动尚未结束，无法评价"));

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication);

        // 验证
        assertEquals(403, result.getCode());
        assertEquals("活动尚未结束，无法评价", result.getMessage());
    }

    @Test
    void createReview_NotParticipated() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new com.campus.eventsys.common.BusinessException(403, "仅活动参与者可以评价"));

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication);

        // 验证
        assertEquals(403, result.getCode());
        assertEquals("仅活动参与者可以评价", result.getMessage());
    }

    @Test
    void createReview_NotCheckedIn() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new com.campus.eventsys.common.BusinessException(403, "您未完成签到，无法评价"));

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication);

        // 验证
        assertEquals(403, result.getCode());
        assertEquals("您未完成签到，无法评价", result.getMessage());
    }

    @Test
    void createReview_DuplicateReview() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new com.campus.eventsys.common.BusinessException(400, "您已评价过该活动"));

        // 执行
        Result<Map<String, Object>> result = activityReviewController.createReview(createReviewBody(ACTIVITY_ID, 5, "活动很好！"), authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("您已评价过该活动", result.getMessage());
    }

    @Test
    void getActivityReviews_Success() {
        // 准备
        Page<ActivityReview> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(testReview));
        when(reviewService.getActivityReviews(ACTIVITY_ID, 1, 10)).thenReturn(page);

        // 执行
        Result<Page<ActivityReview>> result = activityReviewController.getActivityReviews(ACTIVITY_ID, 1, 10);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getMyReviews_Success() {
        // 准备
        Page<ActivityReview> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(testReview));
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.getMyReviews(USER_ID, 1, 10)).thenReturn(page);

        // 执行
        Result<Page<ActivityReview>> result = activityReviewController.getMyReviews(1, 10, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
    }

    @Test
    void getOrganizerReviews_NoActivityId_Success() {
        // 准备
        Page<ActivityReview> page = new Page<>(1, 10, 0);
        page.setRecords(Collections.emptyList());
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(reviewService.getOrganizerReviews(USER_ID, null, 1, 10)).thenReturn(page);

        // 执行
        Result<Page<ActivityReview>> result = activityReviewController.getOrganizerReviews(null, 1, 10, authentication);

        // 验证
        assertEquals(200, result.getCode());
        verify(permissionService, never()).isOrganizer(anyLong(), anyLong());
    }

    @Test
    void getOrganizerReviews_WithActivityId_IsOrganizer_Success() {
        // 准备
        Page<ActivityReview> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(testReview));
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(true);
        when(reviewService.getOrganizerReviews(USER_ID, ACTIVITY_ID, 1, 10)).thenReturn(page);

        // 执行
        Result<Page<ActivityReview>> result = activityReviewController.getOrganizerReviews(ACTIVITY_ID, 1, 10, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        verify(permissionService).isOrganizer(ACTIVITY_ID, USER_ID);
    }

    @Test
    void getOrganizerReviews_WithActivityId_NotOrganizer_Forbidden() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(permissionService.isOrganizer(ACTIVITY_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Page<ActivityReview>> result = activityReviewController.getOrganizerReviews(ACTIVITY_ID, 1, 10, authentication);

        // 验证
        assertEquals(403, result.getCode());
        assertEquals("您不是该活动的组织者，无权查看评价", result.getMessage());
        verify(reviewService, never()).getOrganizerReviews(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void replyReview_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        doNothing().when(reviewService).replyReview(REVIEW_ID, USER_ID, "感谢评价！");

        // 执行
        Result<Void> result = activityReviewController.replyReview(REVIEW_ID, "感谢评价！", authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("回复成功", result.getMessage());
        verify(reviewService).replyReview(REVIEW_ID, USER_ID, "感谢评价！");
    }

    @Test
    void deleteReview_Admin_Success() {
        // 准备
        when(authentication.getName()).thenReturn("admin");
        when(userService.getByUsername("admin")).thenReturn(adminUser);
        doNothing().when(reviewService).deleteReview(REVIEW_ID);

        // 执行
        Result<Void> result = activityReviewController.deleteReview(REVIEW_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
        verify(reviewService).deleteReview(REVIEW_ID);
    }

    @Test
    void deleteReview_NotAdmin_Forbidden() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<Void> result = activityReviewController.deleteReview(REVIEW_ID, authentication);

        // 验证
        assertEquals(403, result.getCode());
        assertEquals("仅管理员可删除评价", result.getMessage());
        verify(reviewService, never()).deleteReview(anyLong());
    }

    @Test
    void getReviewStatistics_Success() {
        // 准备
        ReviewStatisticsVO stats = new ReviewStatisticsVO();
        stats.setActivityId(ACTIVITY_ID);
        stats.setAvgRating(4.5);
        stats.setTotalCount(10);
        when(reviewService.getReviewStatistics(ACTIVITY_ID)).thenReturn(stats);

        // 执行
        Result<ReviewStatisticsVO> result = activityReviewController.getReviewStatistics(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(ACTIVITY_ID, result.getData().getActivityId());
        assertEquals(4.5, result.getData().getAvgRating());
        assertEquals(10, result.getData().getTotalCount());
    }

    @Test
    void getReviewStatistics_ActivityNotFound() {
        // 准备
        when(reviewService.getReviewStatistics(ACTIVITY_ID))
                .thenThrow(new com.campus.eventsys.common.BusinessException(404, "活动不存在"));

        // 执行
        Result<ReviewStatisticsVO> result = activityReviewController.getReviewStatistics(ACTIVITY_ID);

        // 验证
        assertEquals(404, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    /**
     * 构建createReview请求体
     */
    private Map<String, Object> createReviewBody(Long activityId, Integer rating, String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("activityId", activityId);
        body.put("rating", rating);
        body.put("content", content);
        return body;
    }
}
