package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Notification;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 通知控制器
 * 提供用户通知查看、标记已读、删除等功能
 *
 * 管理员公告管理接口已迁至 AdminNotificationController (/admin/notifications)
 */
@Tag(name = "通知管理", description = "用户通知查看、标记已读、删除接口。管理员公告管理请使用 /admin/notifications 接口\n\n" +
        "【通知类型说明】\n" +
        "- type=1: 系统公告（管理员发送的全局公告，user_id=0表示全体可见）\n" +
        "- type=2: 活动提醒（活动即将开始时自动提醒参与者）\n" +
        "- type=3: 审核结果（活动审核通过/拒绝、报名审核通过/拒绝）\n" +
        "- type=4: 活动状态变更（活动即将开始、已结束、成果展示贴发布）\n" +
        "- type=5: 评论回复（有人回复了你的评论）")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 获取当前用户的通知列表
     */
    @Operation(summary = "获取通知列表", 
               description = "获取当前登录用户的通知列表，包含个人通知和系统公告。按时间倒序排列。\n\n" +
                           "【返回数据结构】\n" +
                           "- id: 通知ID\n" +
                           "- userId: 接收用户ID（0表示系统公告）\n" +
                           "- title: 通知标题\n" +
                           "- content: 通知内容\n" +
                           "- type: 通知类型（1-系统公告, 2-活动提醒, 3-审核结果, 4-状态变更, 5-评论回复）\n" +
                           "- activityId: 关联活动ID（可选）\n" +
                           "- isRead: 是否已读（0-未读, 1-已读）\n" +
                           "- readTime: 阅读时间\n" +
                           "- createTime: 创建时间")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/list")
    public Result<Page<Notification>> list(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") Integer size) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean admin = user.getRole() != null && user.getRole() == 1;
        Integer role = user.getRole() != null ? user.getRole() : 0;
        Page<Notification> notifications = notificationService.getUserNotifications(user.getId(), role, admin, page, size);
        return Result.success(notifications);
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "未读数量", 
               description = "获取当前用户的未读通知数量，用于消息角标显示。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount(@Parameter(hidden = true) Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean admin = user.getRole() != null && user.getRole() == 1;
        Integer role = user.getRole() != null ? user.getRole() : 0;
        Long count = notificationService.getUnreadCount(user.getId(), role, admin);
        return Result.success(new UnreadCountVO(count));
    }

    /**
     * 标记通知为已读
     */
    @Operation(summary = "标记已读",
               description = "将指定通知标记为已读状态。只能标记属于自己的通知。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "通知不存在或无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "通知ID", required = true) @PathVariable Long id) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean success = notificationService.markAsRead(id, user.getId());
        if (!success) {
            return Result.error("通知不存在或无权限");
        }

        return Result.success("标记已读成功", null);
    }

    /**
     * 标记所有通知为已读
     */
    @Operation(summary = "全部已读",
               description = "将当前用户的所有未读通知标记为已读。\n\n" +
                           "返回数据：markedCount - 本次标记已读的通知数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/read-all")
    public Result<ReadAllVO> markAllAsRead(@Parameter(hidden = true) Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return Result.error("用户不存在");
        }

        int count = notificationService.markAllAsRead(user.getId());
        return Result.success(new ReadAllVO(count));
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "删除指定通知。只能删除属于自己的通知，逻辑删除。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "通知不存在或无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "通知ID", required = true) @PathVariable Long id) {

        User user = getCurrentUser(authentication);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean success = notificationService.deleteNotification(id, user.getId());
        if (!success) {
            return Result.error("通知不存在或无权限");
        }

        return Result.success("删除成功", null);
    }

    /**
     * 发送系统公告（管理员）
     * 注意：HTTP 接口已迁至 AdminNotificationController (/admin/notifications/announcement)，
     * 此方法保留给内部调用/单元测试
     */
    public Result<Notification> sendAnnouncement(
            Authentication authentication,
            String title,
            String content,
            String publishScope,
            boolean isPinned,
            String publishTime,
            Integer status,
            String announceCategory) {

        User user = getCurrentUser(authentication);
        Long adminId = user != null ? user.getId() : null;
        Notification notification = notificationService.createAnnouncement(
                adminId, title, content, publishScope, isPinned, parsePublishTime(publishTime), status, announceCategory);
        return Result.success("保存成功", notification);
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return userService.getByUsername(username);
    }

    /**
     * 解析发布时间字符串（yyyy-MM-dd HH:mm:ss 或 ISO 格式），为空返回 null
     */
    private static LocalDateTime parsePublishTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String t = raw.trim().replace('T', ' ');
        if (t.length() >= 19) {
            t = t.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(raw.trim().replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 未读数量响应VO
     */
    @Schema(description = "未读通知数量")
    public static class UnreadCountVO {
        @Schema(description = "未读数量")
        private Long count;

        public UnreadCountVO(Long count) {
            this.count = count;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    /**
     * 全部已读响应VO
     */
    @Schema(description = "全部已读结果")
    public static class ReadAllVO {
        @Schema(description = "标记已读的数量")
        private Integer markedCount;

        public ReadAllVO(Integer markedCount) {
            this.markedCount = markedCount;
        }

        public Integer getMarkedCount() {
            return markedCount;
        }

        public void setMarkedCount(Integer markedCount) {
            this.markedCount = markedCount;
        }
    }
}
