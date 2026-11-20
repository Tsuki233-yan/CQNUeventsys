package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动公开查询控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController activityController;

    private Activity testActivity1;
    private Activity testActivity2;
    private static final Long ACTIVITY_ID = 1L;

    @BeforeEach
    void setUp() {
        testActivity1 = new Activity();
        testActivity1.setId(1L);
        testActivity1.setTitle("测试活动1");
        testActivity1.setStatus(2);
        testActivity1.setViewCount(10);
        testActivity1.setCategoryId(1L);

        testActivity2 = new Activity();
        testActivity2.setId(2L);
        testActivity2.setTitle("测试活动2");
        testActivity2.setStatus(4);
        testActivity2.setViewCount(20);
        testActivity2.setCategoryId(2L);
    }

    @Test
    void list_DefaultQuery() {
        // 准备
        List<Activity> activities = Arrays.asList(testActivity1, testActivity2);
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(activities);
        mockPage.setTotal(2);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, null, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().getTotal());

        // 验证查询条件
        ArgumentCaptor<LambdaQueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(activityService).page(any(Page.class), wrapperCaptor.capture());
    }

    @Test
    void list_WithKeyword() {
        // 准备
        String keyword = "测试";
        List<Activity> activities = Arrays.asList(testActivity1, testActivity2);
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(activities);
        mockPage.setTotal(2);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, keyword, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_WithStatus() {
        // 准备
        Integer status = 2;
        List<Activity> activities = Arrays.asList(testActivity1);
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(activities);
        mockPage.setTotal(1);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, null, status, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_WithCategoryId() {
        // 准备
        Long categoryId = 1L;
        List<Activity> activities = Arrays.asList(testActivity1);
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(activities);
        mockPage.setTotal(1);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, null, null, categoryId);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_WithAllParams() {
        // 准备
        String keyword = "测试";
        Integer status = 2;
        Long categoryId = 1L;
        List<Activity> activities = Arrays.asList(testActivity1);
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(activities);
        mockPage.setTotal(1);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, keyword, status, categoryId);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void list_EmptyResult() {
        // 准备
        Page<Activity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);

        when(activityService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // 执行
        Result<Page<Activity>> result = activityController.list(1, 10, null, null, null);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().getTotal());
    }

    @Test
    void detail_Success() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(testActivity1);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);

        // 执行
        Result<Activity> result = activityController.detail(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(ACTIVITY_ID, result.getData().getId());
        assertEquals("测试活动1", result.getData().getTitle());

        // 验证浏览次数+1
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityService).updateById(activityCaptor.capture());
        assertEquals(11, activityCaptor.getValue().getViewCount());
    }

    @Test
    void detail_ActivityNotFound() {
        // 准备
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        // 执行
        Result<Activity> result = activityController.detail(ACTIVITY_ID);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("活动不存在", result.getMessage());
    }

    @Test
    void detail_UpdateViewCount() {
        // 准备
        Activity activity = new Activity();
        activity.setId(ACTIVITY_ID);
        activity.setTitle("测试活动");
        activity.setViewCount(100);

        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(activityService.updateById(any(Activity.class))).thenReturn(true);

        // 执行
        Result<Activity> result = activityController.detail(ACTIVITY_ID);

        // 验证
        assertEquals(200, result.getCode());

        // 验证浏览次数正确增加
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityService).updateById(activityCaptor.capture());
        assertEquals(101, activityCaptor.getValue().getViewCount());
    }
}
