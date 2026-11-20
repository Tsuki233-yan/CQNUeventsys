package com.campus.eventsys.scheduler;

import com.campus.eventsys.service.ActivityStartReminderService;
import com.campus.eventsys.service.ActivityStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动状态定时任务调度器
 * 定时自动检查和更新活动状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final ActivityStatusService activityStatusService;
    private final ActivityStartReminderService activityStartReminderService;

    /**
     * 定时检查并更新所有活动状态
     * 每30秒执行一次
     *
     * 状态流转规则：
     * - 开始前30分钟：2(报名中) → 3(即将开始)
     * - 到达开始时间：2/3 → 4(进行中)
     * - 到达结束时间：2/3/4 → 5(已结束)
     */
    @Scheduled(fixedRate = 30000) // 30秒 = 30000毫秒
    public void scheduledStatusUpdate() {
        log.debug("开始执行活动状态自动更新任务");
        try {
            activityStatusService.checkAndUpdateAllActivities();
            activityStartReminderService.sendCalendarDayReminders();
        } catch (Exception e) {
            log.error("活动状态自动更新任务执行失败", e);
        }
    }
}
