package com.campus.eventsys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 活动开始相关定时提醒：按东八区日历日在「开始前第 3 天」「开始前第 1 天」向已通过报名用户发通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityStartReminderService {

    private static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String MARKER_3D = "【3天后开始】";
    private static final String MARKER_1D = "【明天开始】";

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationService registrationService;
    private final NotificationService notificationService;

    public void sendCalendarDayReminders() {
        LocalDate today = LocalDate.now(CALENDAR_ZONE);
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<>();
        w.ge(Activity::getStatus, 2)
                .le(Activity::getStatus, 4)
                .isNotNull(Activity::getStartTime);
        List<Activity> activities = activityMapper.selectList(w);
        for (Activity activity : activities) {
            try {
                processCalendarRemindersForActivity(activity, today);
            } catch (Exception e) {
                log.error("活动日历日提醒失败: activityId={}", activity.getId(), e);
            }
        }
    }

    private void processCalendarRemindersForActivity(Activity activity, LocalDate today) {
        LocalDateTime startTime = activity.getStartTime();
        if (startTime == null) {
            return;
        }
        LocalDate startDay = startTime.atZone(CALENDAR_ZONE).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(today, startDay);
        if (daysBetween != 3 && daysBetween != 1) {
            return;
        }

        List<ActivityRegistration> registrations = registrationService.getActivityRegistrations(activity.getId());
        for (ActivityRegistration reg : registrations) {
            if (!isApprovedOrCheckedIn(reg)) {
                continue;
            }
            boolean organizer = reg.getRole() != null
                    && reg.getRole() == ActivityRole.ORGANIZER.getCode();
            Long uid = reg.getUserId();
            Long aid = activity.getId();
            if (daysBetween == 3) {
                if (notificationService.hasActivityReminderSent(uid, aid, MARKER_3D)) {
                    continue;
                }
                notificationService.sendActivityThreeDaysCalendarReminder(
                        uid, aid, activity.getTitle(), startTime, organizer);
            } else {
                if (notificationService.hasActivityReminderSent(uid, aid, MARKER_1D)) {
                    continue;
                }
                notificationService.sendActivityOneDayCalendarReminder(
                        uid, aid, activity.getTitle(), startTime, organizer);
            }
        }
    }

    private static boolean isApprovedOrCheckedIn(ActivityRegistration reg) {
        Integer s = reg.getStatus();
        return s != null && (s == 1 || s == 3);
    }
}
