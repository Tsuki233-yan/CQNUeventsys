package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Notification;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 管理员-公告管理控制器
 * 提供系统公告的创建、更新、撤回、列表查询等功能（管理员权限）
 *
 * 与 NotificationController 的职责划分：
 * - NotificationController (/notifications)：用户通知查看、标记已读、删除
 * - AdminNotificationController (/admin/notifications)：管理员公告管理（CRUD + 撤回）
 *
 * 公告发布范围说明：
 * - ALL：所有用户可见
 * - USER：仅普通用户可见
 * - ADMIN：仅管理员可见
 *
 * 公告分类说明：
 * - SYSTEM：系统公告
 * - ACTIVITY：活动公告
 * - URGENT：紧急公告
 */
@Tag(name = "管理员-公告管理", description = "管理员使用的系统公告管理接口，包含创建、更新、撤回、列表查询等功能。需要ADMIN角色权限\n\n" +
        "与用户通知接口的区别：\n" +
        "- /notifications (NotificationController): 用户查看/标记已读/删除通知\n" +
        "- /admin/notifications (本接口): 管理员CRUD管理公告\n\n" +
        "公告发布范围：ALL(所有用户) / USER(仅普通用户) / ADMIN(仅管理员)\n" +
        "公告分类：SYSTEM(系统公告) / ACTIVITY(活动公告) / URGENT(紧急公告)\n" +
        "公告状态：0-草稿 / 1-已发布 / 2-已撤回")
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 创建系统公告
     */
    @Operation(summary = "创建公告",
               description = "管理员创建系统公告。\n\n" +
                           "参数说明：\n" +
                           "- title: 公告标题（必填）\n" +
                           "- content: 公告内容（必填）\n" +
                           "- publishScope: 发布范围，ALL/USER/ADMIN（默认ALL）\n" +
                           "- isPinned: 是否置顶（默认false）\n" +
                           "- publishTime: 定时发布时间，格式yyyy-MM-dd HH:mm:ss（可选，为空则立即发布）\n" +
                           "- status: 状态，0-草稿/1-已发布（默认1）\n" +
                           "- announceCategory: 公告分类，SYSTEM/ACTIVITY/URGENT（默认SYSTEM）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/announcement")
    public Result<Notification> createAnnouncement(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "公告标题", required = true) @RequestParam String title,
            @Parameter(description = "公告内容", required = true) @RequestParam String content,
            @Parameter(description = "发布范围：ALL/USER/ADMIN") @RequestParam(required = false, defaultValue = "ALL") String publishScope,
            @Parameter(description = "是否置顶") @RequestParam(required = false, defaultValue = "false") boolean isPinned,
            @Parameter(description = "发布时间 yyyy-MM-dd HH:mm:ss，可选，为空则立即发布") @RequestParam(required = false) String publishTime,
            @Parameter(description = "状态：0-草稿 1-已发布") @RequestParam(required = false, defaultValue = "1") Integer status,
            @Parameter(description = "公告分类：SYSTEM/ACTIVITY/URGENT") @RequestParam(required = false, defaultValue = "SYSTEM") String announceCategory) {

        User user = getCurrentUser(authentication);
        Long adminId = user != null ? user.getId() : null;
        LocalDateTime pt = parsePublishTime(publishTime);
        Notification notification = notificationService.createAnnouncement(
                adminId, title, content, publishScope, isPinned, pt, status, announceCategory);
        return Result.success("保存成功", notification);
    }

    /**
     * 获取公告列表（管理员）
     */
    @Operation(summary = "公告列表", description = "分页获取所有系统公告列表，按时间倒序排列。包含草稿和已撤回的公告")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/announcements")
    public Result<Page<Notification>> listAnnouncements(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认20") @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(notificationService.pageAdminAnnouncements(page, size));
    }

    /**
     * 获取公告详情（管理员）
     */
    @Operation(summary = "公告详情", description = "根据ID获取系统公告详情。可查看草稿和已撤回的公告")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/announcement/{id}")
    public Result<Notification> getAnnouncement(
            @Parameter(description = "公告ID", required = true) @PathVariable Long id) {
        Notification n = notificationService.getAdminAnnouncement(id);
        if (n == null) {
            return Result.error("公告不存在");
        }
        return Result.success(n);
    }

    /**
     * 更新公告
     */
    @Operation(summary = "更新公告", description = "更新系统公告的标题、内容、发布范围等信息。所有参数均为可选，仅更新传入的字段")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/announcement/{id}")
    public Result<Void> updateAnnouncement(
            @Parameter(description = "公告ID", required = true) @PathVariable Long id,
            @Parameter(description = "公告标题", required = true) @RequestParam String title,
            @Parameter(description = "公告内容", required = true) @RequestParam String content,
            @Parameter(description = "发布范围：ALL/USER/ADMIN") @RequestParam(required = false, defaultValue = "ALL") String publishScope,
            @Parameter(description = "是否置顶") @RequestParam(required = false, defaultValue = "false") boolean isPinned,
            @Parameter(description = "发布时间 yyyy-MM-dd HH:mm:ss，可选") @RequestParam(required = false) String publishTime,
            @Parameter(description = "状态：0-草稿 1-已发布") @RequestParam(required = false) Integer status,
            @Parameter(description = "公告分类：SYSTEM/ACTIVITY/URGENT") @RequestParam(required = false, defaultValue = "SYSTEM") String announceCategory) {
        try {
            notificationService.updateAnnouncement(id, title, content, publishScope, isPinned,
                    parsePublishTime(publishTime), status, announceCategory);
            return Result.success("更新成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 撤回公告
     */
    @Operation(summary = "撤回公告", description = "撤回已发布的系统公告。撤回后用户将无法查看该公告，但管理员仍可在公告列表中查看")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "撤回成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/announcement/{id}/recall")
    public Result<Void> recallAnnouncement(
            @Parameter(description = "公告ID", required = true) @PathVariable Long id) {
        try {
            notificationService.recallAnnouncement(id);
            return Result.success("已撤回", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private static LocalDateTime parsePublishTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.endsWith("Z")) {
            t = t.substring(0, t.length() - 1);
        }
        t = t.replace('T', ' ');
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

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return userService.getByUsername(username);
    }
}
