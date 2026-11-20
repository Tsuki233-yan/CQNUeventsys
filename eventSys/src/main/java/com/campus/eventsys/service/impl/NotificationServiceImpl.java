package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Notification;
import com.campus.eventsys.mapper.NotificationMapper;
import com.campus.eventsys.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    /** 定时发布时间由前端按本地日历选择传入，与 SQLite datetime('now')/UTC 比较会错位；列表可见性用东八区当前时刻比较 */
    private static final ZoneId ANNOUNCEMENT_VISIBLE_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter ACTIVITY_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationMapper notificationMapper;

    /** 个人/业务通知：不参与公告筛选字段，保证列表 SQL 分支正确 */
    private void fillPersonalNotificationDefaults(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        notification.setPublishTime(now);
        notification.setStatus(1);
        notification.setIsPinned(0);
    }

    @Override
    public Notification sendSystemAnnouncement(String title, String content) {
        return createAnnouncement(null, title, content, "ALL", false, LocalDateTime.now(), 1, "SYSTEM");
    }

    @Override
    public Notification createAnnouncement(Long createdBy, String title, String content, String publishScope,
                                             boolean pinned, LocalDateTime publishTime, Integer status, String announceCategory) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(0L);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(1);
        notification.setIsRead(0);
        notification.setPublishScope(normalizeScope(publishScope));
        notification.setIsPinned(pinned ? 1 : 0);
        notification.setStatus(status != null ? status : 1);
        notification.setAnnounceCategory(normalizeCategory(announceCategory));
        notification.setCreatedBy(createdBy);
        if (notification.getStatus() != null && notification.getStatus() == 0) {
            notification.setPublishTime(null);
        } else {
            notification.setPublishTime(publishTime != null ? publishTime : LocalDateTime.now());
        }
        notificationMapper.insert(notification);
        log.info("创建系统公告: id={}, title={}, status={}, scope={}", notification.getId(), title, notification.getStatus(), notification.getPublishScope());
        return notification;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(Long id, String title, String content, String publishScope,
                                   boolean pinned, LocalDateTime publishTime, Integer status, String announceCategory) {
        Notification existing = notificationMapper.selectById(id);
        if (existing == null || existing.getType() == null || existing.getType() != 1
                || existing.getUserId() == null || existing.getUserId() != 0L) {
            throw new IllegalArgumentException("公告不存在");
        }
        LambdaUpdateWrapper<Notification> uw = new LambdaUpdateWrapper<>();
        uw.eq(Notification::getId, id)
                .set(Notification::getTitle, title)
                .set(Notification::getContent, content)
                .set(Notification::getPublishScope, normalizeScope(publishScope))
                .set(Notification::getIsPinned, pinned ? 1 : 0)
                .set(Notification::getStatus, status != null ? status : existing.getStatus())
                .set(Notification::getAnnounceCategory, normalizeCategory(announceCategory))
                .set(Notification::getUpdateTime, LocalDateTime.now());
        int st = status != null ? status : (existing.getStatus() != null ? existing.getStatus() : 1);
        if (st == 0) {
            uw.set(Notification::getPublishTime, null);
        } else {
            LocalDateTime pt = publishTime != null ? publishTime
                    : (existing.getPublishTime() != null ? existing.getPublishTime() : LocalDateTime.now());
            uw.set(Notification::getPublishTime, pt);
        }
        notificationMapper.update(null, uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallAnnouncement(Long id) {
        Notification existing = notificationMapper.selectById(id);
        if (existing == null || existing.getType() == null || existing.getType() != 1
                || existing.getUserId() == null || existing.getUserId() != 0L) {
            throw new IllegalArgumentException("公告不存在");
        }
        LambdaUpdateWrapper<Notification> uw = new LambdaUpdateWrapper<>();
        uw.eq(Notification::getId, id)
                .set(Notification::getStatus, 2)
                .set(Notification::getUpdateTime, LocalDateTime.now());
        notificationMapper.update(null, uw);
    }

    @Override
    public Notification getAdminAnnouncement(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || n.getType() == null || n.getType() != 1
                || n.getUserId() == null || n.getUserId() != 0L) {
            return null;
        }
        return n;
    }

    @Override
    public Page<Notification> pageAdminAnnouncements(Integer page, Integer size) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getType, 1)
                .eq(Notification::getUserId, 0L)
                .orderByDesc(Notification::getUpdateTime)
                .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(new Page<>(page, size), w);
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "ALL";
        }
        String u = scope.trim().toUpperCase();
        if ("ALL".equals(u) || "USER".equals(u) || "ADMIN".equals(u)) {
            return u;
        }
        return "ALL";
    }

    private static String normalizeCategory(String cat) {
        if (cat == null || cat.isBlank()) {
            return "SYSTEM";
        }
        String u = cat.trim().toUpperCase();
        if ("SYSTEM".equals(u) || "ACTIVITY".equals(u) || "URGENT".equals(u)) {
            return u;
        }
        return "SYSTEM";
    }

    @Override
    public Notification sendAuditResult(Long userId, Long activityId, String activityTitle, boolean approved, String reason) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setType(3);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        if (approved) {
            notification.setTitle("活动审核通过");
            notification.setContent(String.format("您创建的活动《%s》已通过审核，现已发布。", activityTitle));
        } else {
            notification.setTitle("活动审核未通过");
            notification.setContent(String.format("您创建的活动《%s》未通过审核。原因：%s",
                    activityTitle, reason != null ? reason : "不符合发布要求"));
        }

        notificationMapper.insert(notification);
        log.info("发送审核结果通知: userId={}, activityId={}, approved={}", userId, activityId, approved);
        return notification;
    }

    @Override
    public Notification sendStatusChange(Long userId, Long activityId, String activityTitle, String oldStatus, String newStatus) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("活动状态变更");
        notification.setContent(String.format("您参与的活动《%s》状态已变更：%s → %s",
                activityTitle, oldStatus, newStatus));
        notification.setType(4);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        notificationMapper.insert(notification);
        log.info("发送状态变更通知: userId={}, activityId={}, {} -> {}", userId, activityId, oldStatus, newStatus);
        return notification;
    }

    @Override
    public Notification sendActivityReminder(Long userId, Long activityId, String activityTitle, String startTime) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("活动即将开始");
        notification.setContent(String.format("您报名的活动《%s》将于 %s 开始，请准时参加。",
                activityTitle, startTime));
        notification.setType(2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        notificationMapper.insert(notification);
        log.info("发送活动提醒(参与者): userId={}, activityId={}", userId, activityId);
        return notification;
    }

    @Override
    public Notification sendOrganizerActivityReminder(Long userId, Long activityId, String activityTitle, String startTime) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("活动即将开始");
        notification.setContent(String.format("您组织的活动《%s》将于 %s 开始，请做好活动准备。",
                activityTitle, startTime));
        notification.setType(2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        notificationMapper.insert(notification);
        log.info("发送活动提醒(组织者): userId={}, activityId={}", userId, activityId);
        return notification;
    }

    private long nextPersonalNotificationId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(0, 1000);
    }

    @Override
    public boolean hasActivityReminderSent(Long userId, Long activityId, String titleMarker) {
        if (userId == null || activityId == null || titleMarker == null || titleMarker.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getUserId, userId)
                .eq(Notification::getActivityId, activityId)
                .eq(Notification::getType, 2)
                .like(Notification::getTitle, titleMarker);
        Long c = notificationMapper.selectCount(w);
        return c != null && c > 0;
    }

    @Override
    public Notification sendActivityThreeDaysCalendarReminder(Long userId, Long activityId, String activityTitle,
                                                              LocalDateTime startTime, boolean isOrganizer) {
        String when = startTime != null ? startTime.format(ACTIVITY_TIME_FMT) : "";
        Notification notification = new Notification();
        notification.setId(nextPersonalNotificationId());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("【3天后开始】活动提醒");
        if (isOrganizer) {
            notification.setContent(String.format("您组织的活动《%s》将于 3 天后（%s）开始，请提前做好现场准备。",
                    activityTitle, when));
        } else {
            notification.setContent(String.format("您报名的活动《%s》将于 3 天后（%s）开始，请提前安排时间。",
                    activityTitle, when));
        }
        notification.setType(2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);
        notificationMapper.insert(notification);
        log.info("发送活动3日历日前提醒: userId={}, activityId={}, organizer={}", userId, activityId, isOrganizer);
        return notification;
    }

    @Override
    public Notification sendActivityOneDayCalendarReminder(Long userId, Long activityId, String activityTitle,
                                                           LocalDateTime startTime, boolean isOrganizer) {
        String when = startTime != null ? startTime.format(ACTIVITY_TIME_FMT) : "";
        Notification notification = new Notification();
        notification.setId(nextPersonalNotificationId());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("【明天开始】活动提醒");
        if (isOrganizer) {
            notification.setContent(String.format("您组织的活动《%s》将于明天（%s）开始，请做好最后确认与物料准备。",
                    activityTitle, when));
        } else {
            notification.setContent(String.format("您报名的活动《%s》将于明天（%s）开始，请留意时间与地点。",
                    activityTitle, when));
        }
        notification.setType(2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);
        notificationMapper.insert(notification);
        log.info("发送活动前1日历日提醒: userId={}, activityId={}, organizer={}", userId, activityId, isOrganizer);
        return notification;
    }

    @Override
    public Notification sendActivityJustStartedReminder(Long userId, Long activityId, String activityTitle, boolean isOrganizer) {
        Notification notification = new Notification();
        notification.setId(nextPersonalNotificationId());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("【活动已开始】活动提醒");
        if (isOrganizer) {
            notification.setContent(String.format("您组织的活动《%s》已开始，请关注现场秩序与签到情况。",
                    activityTitle));
        } else {
            notification.setContent(String.format("您报名的活动《%s》已开始，请准时到场并关注现场安排。",
                    activityTitle));
        }
        notification.setType(2);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);
        notificationMapper.insert(notification);
        log.info("发送活动已开始提醒: userId={}, activityId={}, organizer={}", userId, activityId, isOrganizer);
        return notification;
    }

    @Override
    public Notification sendCommentReply(Long userId, Long activityId, String activityTitle, String replyUserName, String content) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("评论回复提醒");
        notification.setContent(String.format("用户 %s 回复了您在《%s》中的评论：%s",
                replyUserName, activityTitle, content));
        notification.setType(5);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        notificationMapper.insert(notification);
        log.info("发送评论回复通知: userId={}, activityId={}", userId, activityId);
        return notification;
    }

    @Override
    public Notification sendResultPublish(Long userId, Long activityId, String activityTitle, String creatorName) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setTitle("成果展示贴发布");
        notification.setContent(String.format("您参与的活动《%s》已发布成果展示贴，欢迎查看。", activityTitle));
        notification.setType(4);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        notificationMapper.insert(notification);
        log.info("发送成果展示贴发布通知: userId={}, activityId={}", userId, activityId);
        return notification;
    }

    @Override
    public Notification sendOrganizerChange(Long userId, Long activityId, String activityTitle, String organizerName, boolean isAdded) {
        Notification notification = new Notification();
        notification.setId(System.currentTimeMillis());
        notification.setUserId(userId);
        notification.setActivityId(activityId);
        notification.setType(4);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        fillPersonalNotificationDefaults(notification);

        if (isAdded) {
            notification.setTitle("被添加为活动管理者");
            notification.setContent(String.format("您被 %s 添加为活动《%s》的管理者，可以协助管理该活动。",
                    organizerName, activityTitle));
        } else {
            notification.setTitle("活动管理者身份已移除");
            notification.setContent(String.format("您已被 %s 从活动《%s》的管理者列表中移除。",
                    organizerName, activityTitle));
        }

        notificationMapper.insert(notification);
        log.info("发送管理者变更通知: userId={}, activityId={}, isAdded={}", userId, activityId, isAdded);
        return notification;
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, Integer role, boolean admin, Integer page, Integer size) {
        Page<Notification> p = new Page<>(page, size);
        int r = role != null ? role : 0;
        LocalDateTime now = ZonedDateTime.now(ANNOUNCEMENT_VISIBLE_ZONE).toLocalDateTime();
        notificationMapper.selectUserNotificationPage(p, userId, r, admin, now);
        return p;
    }

    @Override
    public Long getUnreadCount(Long userId, Integer role, boolean admin) {
        int r = role != null ? role : 0;
        LocalDateTime now = ZonedDateTime.now(ANNOUNCEMENT_VISIBLE_ZONE).toLocalDateTime();
        Long c = notificationMapper.countUnreadUserNotifications(userId, r, admin, now);
        return c == null ? 0L : c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long notificationId, Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .eq(Notification::getUserId, 0L))
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now());

        int rows = notificationMapper.update(null, wrapper);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .eq(Notification::getUserId, 0L))
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now());

        return notificationMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotification(Long notificationId, Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .eq(Notification::getUserId, 0L));

        int rows = notificationMapper.delete(wrapper);
        return rows > 0;
    }
}
