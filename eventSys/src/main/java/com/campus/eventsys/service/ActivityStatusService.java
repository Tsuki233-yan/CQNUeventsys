package com.campus.eventsys.service;

import com.campus.eventsys.entity.Activity;

/**
 * 活动状态服务接口
 * 处理活动状态流转逻辑
 */
public interface ActivityStatusService {

    /**
     * 自动更新活动状态
     * 根据当前时间和活动计划时间自动流转状态
     *
     * @param activityId 活动ID
     * @return 更新后的活动
     */
    Activity autoUpdateStatus(Long activityId);

    /**
     * 开始活动（组织者/管理员手动开始）
     * 状态从 2(报名中) 或 3(即将开始) 转为 4(进行中)
     *
     * @param activityId 活动ID
     * @param userId     操作用户ID
     * @return 是否成功
     */
    boolean startActivity(Long activityId, Long userId);

    /**
     * 检查并更新所有活动的状态
     * 用于定时任务调用
     */
    void checkAndUpdateAllActivities();

    /**
     * 获取活动当前应该处于的状态
     *
     * @param activity 活动
     * @return 应该的状态码
     */
    int getExpectedStatus(Activity activity);
}
