package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 发送系统公告（全体用户）
     *
     * @param title   标题
     * @param content 内容
     * @return 通知对象
     */
    Notification sendSystemAnnouncement(String title, String content);

    /**
     * 创建系统公告（管理员）
     */
    Notification createAnnouncement(Long createdBy, String title, String content, String publishScope,
                                    boolean pinned, LocalDateTime publishTime, Integer status, String announceCategory);

    /**
     * 更新系统公告
     */
    void updateAnnouncement(Long id, String title, String content, String publishScope,
                            boolean pinned, LocalDateTime publishTime, Integer status, String announceCategory);

    /** 撤回公告 */
    void recallAnnouncement(Long id);

    /** 管理员获取单条公告（type=1） */
    Notification getAdminAnnouncement(Long id);

    /** 管理员分页：全部系统公告 */
    Page<Notification> pageAdminAnnouncements(Integer page, Integer size);

    /**
     * 发送活动审核结果通知
     *
     * @param userId     接收用户ID
     * @param activityId 活动ID
     * @param activityTitle 活动标题
     * @param approved   是否通过
     * @param reason     原因（拒绝时）
     * @return 通知对象
     */
    Notification sendAuditResult(Long userId, Long activityId, String activityTitle, boolean approved, String reason);

    /**
     * 发送活动状态变更通知
     *
     * @param userId     接收用户ID
     * @param activityId 活动ID
     * @param activityTitle 活动标题
     * @param oldStatus  旧状态
     * @param newStatus  新状态
     * @return 通知对象
     */
    Notification sendStatusChange(Long userId, Long activityId, String activityTitle, String oldStatus, String newStatus);

    /**
     * 发送活动提醒（即将开始）- 参与者
     *
     * @param userId     接收用户ID
     * @param activityId 活动ID
     * @param activityTitle 活动标题
     * @param startTime  开始时间
     * @return 通知对象
     */
    Notification sendActivityReminder(Long userId, Long activityId, String activityTitle, String startTime);

    /**
     * 发送活动提醒（即将开始）- 组织者
     *
     * @param userId     接收用户ID
     * @param activityId 活动ID
     * @param activityTitle 活动标题
     * @param startTime  开始时间
     * @return 通知对象
     */
    Notification sendOrganizerActivityReminder(Long userId, Long activityId, String activityTitle, String startTime);

    /**
     * 是否已发过某类活动提醒（type=2，标题包含 marker，用于去重）
     */
    boolean hasActivityReminderSent(Long userId, Long activityId, String titleMarker);

    /**
     * 活动开始前第 3 个日历日提醒（东八区日期），type=2
     */
    Notification sendActivityThreeDaysCalendarReminder(Long userId, Long activityId, String activityTitle,
                                                       LocalDateTime startTime, boolean isOrganizer);

    /**
     * 活动开始前第 1 个日历日（明天）提醒，type=2
     */
    Notification sendActivityOneDayCalendarReminder(Long userId, Long activityId, String activityTitle,
                                                    LocalDateTime startTime, boolean isOrganizer);

    /**
     * 活动已开始（进行中）即时提醒，type=2
     */
    Notification sendActivityJustStartedReminder(Long userId, Long activityId, String activityTitle, boolean isOrganizer);

    /**
     * 发送评论回复通知
     *
     * @param userId      接收用户ID（被回复者）
     * @param activityId  活动ID
     * @param activityTitle 活动标题
     * @param replyUserName 回复者名称
     * @param content     回复内容
     * @return 通知对象
     */
    Notification sendCommentReply(Long userId, Long activityId, String activityTitle, String replyUserName, String content);

    /**
     * 发送成果展示贴发布通知
     *
     * @param userId        接收用户ID
     * @param activityId    活动ID
     * @param activityTitle 活动标题
     * @param creatorName   创建者名称
     * @return 通知对象
     */
    Notification sendResultPublish(Long userId, Long activityId, String activityTitle, String creatorName);

    /**
     * 发送活动管理者变更通知（添加/移除）
     *
     * @param userId        接收用户ID（被添加/移除的管理者）
     * @param activityId    活动ID
     * @param activityTitle 活动标题
     * @param organizerName 操作者名称（活动组织者）
     * @param isAdded       true-被添加为管理者，false-被移除管理者身份
     * @return 通知对象
     */
    Notification sendOrganizerChange(Long userId, Long activityId, String activityTitle, String organizerName, boolean isAdded);

    /**
     * 获取用户的通知列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    Page<Notification> getUserNotifications(Long userId, Integer role, boolean admin, Integer page, Integer size);

    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    Long getUnreadCount(Long userId, Integer role, boolean admin);

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long notificationId, Long userId);

    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     * @return 标记数量
     */
    int markAllAsRead(Long userId);

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     * @return 是否成功
     */
    boolean deleteNotification(Long notificationId, Long userId);
}
