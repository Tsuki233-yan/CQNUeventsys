package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.mapper.UserMapper;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityStatusService;
import com.campus.eventsys.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import com.campus.eventsys.config.TimeZoneConfig;
import java.util.List;

/**
 * 活动状态服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityStatusServiceImpl implements ActivityStatusService {

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final ActivityPermissionService permissionService;
    private final NotificationService notificationService;
    private final ActivityRegistrationService registrationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity autoUpdateStatus(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return null;
        }

        int expectedStatus = getExpectedStatus(activity);
        int oldStatus = activity.getStatus();
        
        // 只有特定状态才自动流转
        if (oldStatus >= 2 && oldStatus <= 4) {
            if (oldStatus != expectedStatus) {
                activity.setStatus(expectedStatus);
                activityMapper.updateById(activity);
                log.info("活动状态自动更新: activityId={}, oldStatus={}, newStatus={}", 
                        activityId, oldStatus, expectedStatus);
                
                // 发送活动状态变更通知
                sendStatusChangeNotification(activity, oldStatus, expectedStatus);
            }
        }

        return activity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("活动不存在: activityId={}", activityId);
            return false;
        }

        // 检查权限（组织者或管理员）
        boolean isOrganizer = permissionService.isOrganizer(activityId, userId);
        User operator = userMapper.selectById(userId);
        boolean isAdmin = operator != null && operator.getRole() != null && operator.getRole() == 1;
        if (!isOrganizer && !isAdmin) {
            log.warn("无权开始活动: activityId={}, userId={}", activityId, userId);
            return false;
        }

        // 检查当前状态是否允许开始
        if (activity.getStatus() != 2 && activity.getStatus() != 3) {
            log.warn("活动状态不允许开始: activityId={}, status={}", activityId, activity.getStatus());
            return false;
        }

        // 更新状态为进行中，同时更新开始时间为当前时间
        int oldStatus = activity.getStatus();
        activity.setStatus(4);
        activity.setStartTime(ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime());
        activityMapper.updateById(activity);
        log.info("活动已开始: activityId={}, operator={}, newStartTime={}", activityId, userId, activity.getStartTime());

        // 发送活动状态变更通知
        sendStatusChangeNotification(activity, oldStatus, 4);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpdateAllActivities() {
        // 查询所有已通过审核且未结束的活动
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Activity::getStatus, 2)  // 已通过审核
               .le(Activity::getStatus, 4);  // 未结束

        List<Activity> activities = activityMapper.selectList(wrapper);
        
        LocalDateTime now = ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime();
        int updatedCount = 0;

        for (Activity activity : activities) {
            int expectedStatus = getExpectedStatus(activity);
            if (activity.getStatus() != expectedStatus) {
                int oldStatus = activity.getStatus();
                activity.setStatus(expectedStatus);
                activityMapper.updateById(activity);
                updatedCount++;
                
                // 发送活动状态变更通知
                sendStatusChangeNotification(activity, oldStatus, expectedStatus);
            }
        }

        log.info("批量更新活动状态完成: 检查{}个活动, 更新{}个", activities.size(), updatedCount);
    }

    @Override
    public int getExpectedStatus(Activity activity) {
        LocalDateTime now = ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime();
        LocalDateTime startTime = activity.getStartTime();
        LocalDateTime endTime = activity.getEndTime();
        int currentStatus = activity.getStatus();

        // 已结束(5) 状态不再变更
        if (currentStatus == 5) {
            return 5;
        }

        // 如果已结束（根据结束时间判断）
        if (endTime != null && now.isAfter(endTime)) {
            return 5; // 已结束
        }

        // 进行中(4) 只能保持或变为已结束，不能回退
        if (currentStatus == 4) {
            // 如果还在进行中时间段内，保持进行中
            if (startTime != null && !now.isBefore(startTime) && 
                (endTime == null || now.isBefore(endTime))) {
                return 4; // 进行中
            }
            // 否则变为已结束（上面已判断过结束时间）
            return 4; // 保持进行中，不自动回退
        }

        // 即将开始(3) 可以变为进行中(4)或保持，不能回退到报名中
        if (currentStatus == 3) {
            // 如果已经开始
            if (startTime != null && !now.isBefore(startTime)) {
                return 4; // 进行中
            }
            // 否则保持即将开始，不回退到报名中
            return 3;
        }

        // 报名中(2) 可以向前流转
        if (currentStatus == 2) {
            // 如果已经开始
            if (startTime != null && !now.isBefore(startTime)) {
                return 4; // 进行中
            }
            // 如果即将开始（开始前30分钟）
            if (startTime != null) {
                LocalDateTime almostStart = startTime.minusMinutes(30);
                if (now.isAfter(almostStart)) {
                    return 3; // 即将开始
                }
            }
            // 保持报名中
            return 2;
        }

        // 其他状态保持不变
        return currentStatus;
    }

    /**
     * 发送活动状态变更通知
     * @param activity 活动
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    private void sendStatusChangeNotification(Activity activity, int oldStatus, int newStatus) {
        try {
            // 获取活动的所有参与者
            List<ActivityRegistration> registrations = registrationService.getActivityRegistrations(activity.getId());
            
            String oldStatusName = getStatusName(oldStatus);
            String newStatusName = getStatusName(newStatus);
            
            for (ActivityRegistration registration : registrations) {
                boolean approvedOnly = registration.getStatus() != null && registration.getStatus() == 1;
                boolean approvedOrCheckedIn = registration.getStatus() != null
                        && (registration.getStatus() == 1 || registration.getStatus() == 3);

                if (approvedOnly) {
                    boolean isOrganizer = registration.getRole() != null
                            && registration.getRole() == ActivityRole.ORGANIZER.getCode();

                    // 活动即将开始或已结束时发送通知
                    if (newStatus == 3 || newStatus == 5) {
                        notificationService.sendStatusChange(
                                registration.getUserId(),
                                activity.getId(),
                                activity.getTitle(),
                                oldStatusName,
                                newStatusName
                        );
                    }
                    // 活动即将开始时额外发送提醒，根据角色区分消息内容
                    if (newStatus == 3) {
                        if (isOrganizer) {
                            notificationService.sendOrganizerActivityReminder(
                                    registration.getUserId(),
                                    activity.getId(),
                                    activity.getTitle(),
                                    activity.getStartTime() != null ? activity.getStartTime().toString() : ""
                            );
                        } else {
                            notificationService.sendActivityReminder(
                                    registration.getUserId(),
                                    activity.getId(),
                                    activity.getTitle(),
                                    activity.getStartTime() != null ? activity.getStartTime().toString() : ""
                            );
                        }
                    }
                }

                // 进入进行中：已通过或已签到的报名用户收到「活动已开始」提醒（与 30 分钟「即将开始」区分）
                if (newStatus == 4 && (oldStatus == 2 || oldStatus == 3) && approvedOrCheckedIn) {
                    boolean isOrganizer = registration.getRole() != null
                            && registration.getRole() == ActivityRole.ORGANIZER.getCode();
                    if (!notificationService.hasActivityReminderSent(
                            registration.getUserId(), activity.getId(), "【活动已开始】")) {
                        notificationService.sendActivityJustStartedReminder(
                                registration.getUserId(),
                                activity.getId(),
                                activity.getTitle(),
                                isOrganizer);
                    }
                }
            }
            
            log.info("发送活动状态变更通知: activityId={}, oldStatus={}, newStatus={}, notified={}", 
                    activity.getId(), oldStatusName, newStatusName, registrations.size());
        } catch (Exception e) {
            log.error("发送活动状态变更通知失败: activityId={}", activity.getId(), e);
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(int status) {
        switch (status) {
            case -2: return "已下架";
            case -1: return "已拒绝";
            case 0: return "草稿";
            case 1: return "待审核";
            case 2: return "报名中";
            case 3: return "即将开始";
            case 4: return "进行中";
            case 5: return "已结束";
            default: return "未知";
        }
    }
}
