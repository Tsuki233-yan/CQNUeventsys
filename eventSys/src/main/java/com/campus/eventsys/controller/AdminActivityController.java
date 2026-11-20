package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.ActivityStatusService;
import com.campus.eventsys.service.CategoryService;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员活动管理控制器
 * 提供活动审核、管理功能
 * 
 * 活动状态码说明：
 * -2: 已下架（管理员强制下架）
 * -1: 已拒绝（审核未通过）
 * 0: 草稿（预留）
 * 1: 待审核（用户创建后等待审核）
 * 2: 报名中（审核通过，接受报名）
 * 3: 即将开始（开始前30分钟内）
 * 4: 进行中（活动已开始）
 * 5: 已结束（超过结束时间）
 */
@Tag(name = "管理员-活动管理", description = "管理员使用的活动审核和管理接口。需要ADMIN角色权限\n\n" +
        "核心功能：\n" +
        "- 审核活动（通过/拒绝）\n" +
        "- 管理活动（删除/强制下架）\n" +
        "- 查看活动列表（含各状态筛选）")
@RestController
@RequestMapping("/admin/activities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityStatusService statusService;
    private final NotificationService notificationService;
    private final CategoryService categoryService;
    private final ActivityRegistrationService registrationService;
    private final ActivityResultService activityResultService;

    /**
     * 获取待审核活动列表
     */
    @Operation(summary = "待审核活动",
               description = "获取所有待审核的活动列表，包含组织者信息。\n\n" +
                           "状态码 1: 待审核（用户创建后等待管理员审核）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/pending")
    public Result<Page<ActivityWithOrganizerVO>> getPendingActivities(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 1) // 1-待审核
               .orderByDesc(Activity::getCreateTime);

        Page<Activity> pageResult = activityService.page(new Page<>(page, size), wrapper);
        List<ActivityWithOrganizerVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 创建新的Page对象，保留分页信息
        Page<ActivityWithOrganizerVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(voList);
        voPage.setPages(pageResult.getPages());
        
        return Result.success(voPage);
    }

    /**
     * 获取所有活动列表（管理员视角）
     */
    @Operation(summary = "所有活动", description = "获取所有活动列表（包含各种状态），支持状态筛选和关键词搜索，包含组织者信息。\n\n" +
                "返回数据包含组织者详细信息（organizer字段：id、username、nickname、realName）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/list")
    public Result<Page<ActivityWithOrganizerVO>> list(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "活动状态筛选：-2-已下架, -1-已拒绝, 0-草稿, 1-待审核, 2-报名中, 3-即将开始, 4-进行中, 5-已结束") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键词（活动标题）") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Activity::getTitle, keyword);
        }

        wrapper.orderByDesc(Activity::getCreateTime);

        Page<Activity> pageResult = activityService.page(new Page<>(page, size), wrapper);
        List<ActivityWithOrganizerVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 创建新的Page对象，保留分页信息
        Page<ActivityWithOrganizerVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(voList);
        voPage.setPages(pageResult.getPages());
        
        return Result.success(voPage);
    }

    /**
     * 获取活动详情（管理员视角，包含组织者信息）
     */
    @Operation(summary = "活动详情（管理员）", description = "根据活动ID获取活动详情（包含组织者信息）。管理员专用，不会修改浏览次数等统计数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/detail/{id}")
    public Result<ActivityWithOrganizerVO> detail(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        return Result.success(convertToVO(activity));
    }

    /**
     * 审核通过活动
     */
    @Operation(summary = "审核通过",
               description = "审核通过指定活动。将活动状态从 1(待审核) 更新为 2(报名中)。\n" +
                           "审核通过后会自动发送通知给活动组织者")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "审核成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/approve/{id}")
    public Result<Void> approve(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 获取当前管理员
        String username = authentication.getName();
        User admin = userService.getByUsername(username);

        // 更新状态为已通过(2)
        activity.setStatus(2);
        activityService.updateById(activity);

        // 发送审核通过通知
        notificationService.sendAuditResult(activity.getOrganizerId(), activity.getId(), 
                activity.getTitle(), true, null);

        return Result.success("审核通过", null);
    }

    /**
     * 拒绝活动
     */
    @Operation(summary = "审核拒绝",
               description = "拒绝指定活动。将活动状态更新为 -1(已拒绝)，活动不会在前端展示。\n" +
                           "拒绝后自动发送通知给活动组织者，通知中包含拒绝原因（如有）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "拒绝成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/reject/{id}")
    public Result<Void> reject(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String reason) {

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 更新状态为已拒绝(-1)
        activity.setStatus(-1);
        activityService.updateById(activity);

        // 发送审核拒绝通知
        notificationService.sendAuditResult(activity.getOrganizerId(), activity.getId(), 
                activity.getTitle(), false, reason);

        return Result.success("已拒绝" + (reason != null ? "：" + reason : ""), null);
    }

    /**
     * 管理员删除活动（物理删除或逻辑删除）
     */
    @Operation(summary = "删除活动", description = "管理员删除活动（逻辑删除）。\n\n" +
                "删除操作会同时清理：报名记录、成果展示贴，并更新相关统计数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 获取活动信息用于后续更新统计
        Long categoryId = activity.getCategoryId();
        Long organizerId = activity.getOrganizerId();

        activityService.removeById(id);

        // 减少分类活动数量统计
        if (categoryId != null) {
            categoryService.decrementActivityCount(categoryId);
        }

        // 减少用户创建活动统计
        User organizer = userService.getById(organizerId);
        if (organizer != null && organizer.getOrganizedCount() > 0) {
            organizer.setOrganizedCount(organizer.getOrganizedCount() - 1);
            userService.updateById(organizer);
        }

        // 减少所有参与者的参与活动统计
        List<ActivityRegistration> registrations = registrationService.getActivityParticipants(id);
        for (ActivityRegistration reg : registrations) {
            User participant = userService.getById(reg.getUserId());
            if (participant != null && participant.getParticipatedCount() > 0) {
                participant.setParticipatedCount(participant.getParticipatedCount() - 1);
                userService.updateById(participant);
            }
        }

        // 删除活动的所有报名记录（包括组织者和参与者）
        registrationService.removeAllRegistrationsByActivity(id);

        // 删除活动的成果展示贴
        activityResultService.removeByActivityId(id);

        return Result.success("删除成功", null);
    }

    /**
     * 强制下架活动
     */
    @Operation(summary = "强制下架",
               description = "强制下架已发布的活动。将活动状态更新为 -2(已下架)，活动将从前端隐藏。\n" +
                           "下架操作会同时：减少分类统计、减少用户创建/参与统计、删除报名记录、下架成果展示贴")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "下架成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/take-down/{id}")
    public Result<Void> takeDown(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id,
            @Parameter(description = "下架原因") @RequestParam(required = false) String reason) {

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 获取活动信息用于后续更新统计
        Long categoryId = activity.getCategoryId();
        Long organizerId = activity.getOrganizerId();

        // 更新状态为已下架(-2)
        activity.setStatus(-2);
        activityService.updateById(activity);

        // 减少分类活动数量统计
        if (categoryId != null) {
            categoryService.decrementActivityCount(categoryId);
        }

        // 减少用户创建活动统计
        User organizer = userService.getById(organizerId);
        if (organizer != null && organizer.getOrganizedCount() > 0) {
            organizer.setOrganizedCount(organizer.getOrganizedCount() - 1);
            userService.updateById(organizer);
        }

        // 减少所有参与者的参与活动统计
        List<ActivityRegistration> registrations = registrationService.getActivityParticipants(id);
        for (ActivityRegistration reg : registrations) {
            User participant = userService.getById(reg.getUserId());
            if (participant != null && participant.getParticipatedCount() > 0) {
                participant.setParticipatedCount(participant.getParticipatedCount() - 1);
                userService.updateById(participant);
            }
        }

        // 删除活动的所有报名记录（包括组织者和参与者）
        registrationService.removeAllRegistrationsByActivity(id);

        // 下架活动的成果展示贴
        activityResultService.takeDownByActivityId(id);

        return Result.success("已强制下架" + (reason != null ? "：" + reason : ""), null);
    }

    /**
     * 批量刷新活动状态
     */
    @Operation(summary = "批量刷新状态", description = "管理员强制检查并更新所有活动状态。通常由定时任务自动调用，也可手动触发")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/refresh-status")
    public Result<Void> refreshStatus() {
        statusService.checkAndUpdateAllActivities();
        return Result.success("批量刷新完成", null);
    }

    /**
     * 将Activity转换为包含组织者信息的VO
     */
    private ActivityWithOrganizerVO convertToVO(Activity activity) {
        ActivityWithOrganizerVO vo = new ActivityWithOrganizerVO();
        // 复制活动基本信息
        vo.setId(activity.getId());
        vo.setTitle(activity.getTitle());
        vo.setDescription(activity.getDescription());
        vo.setCoverImage(activity.getCoverImage());
        vo.setOrganizerId(activity.getOrganizerId());
        vo.setLocation(activity.getLocation());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setRegistrationStartTime(activity.getRegistrationStartTime());
        vo.setRegistrationEndTime(activity.getRegistrationEndTime());
        vo.setMaxParticipants(activity.getMaxParticipants());
        vo.setCurrentParticipants(activity.getCurrentParticipants());
        vo.setTags(activity.getTags());
        vo.setStatus(activity.getStatus());
        vo.setCategoryId(activity.getCategoryId());
        vo.setNeedReview(activity.getNeedReview());
        vo.setViewCount(activity.getViewCount());
        vo.setFavoriteCount(activity.getFavoriteCount());
        vo.setCommentCount(activity.getCommentCount());
        vo.setCreateTime(activity.getCreateTime());
        vo.setUpdateTime(activity.getUpdateTime());

        // 查询并设置组织者信息
        if (activity.getOrganizerId() != null) {
            User organizer = userService.getById(activity.getOrganizerId());
            if (organizer != null) {
                OrganizerInfo organizerInfo = new OrganizerInfo();
                organizerInfo.setId(organizer.getId());
                organizerInfo.setUsername(organizer.getUsername());
                organizerInfo.setNickname(organizer.getNickname());
                organizerInfo.setRealName(organizer.getRealName());
                vo.setOrganizer(organizerInfo);
            }
        }

        return vo;
    }

    /**
     * 活动详情（包含组织者信息）
     */
    @Data
    @Schema(description = "活动详情（包含组织者信息）")
    public static class ActivityWithOrganizerVO {
        @Schema(description = "活动ID")
        private Long id;
        @Schema(description = "活动标题")
        private String title;
        @Schema(description = "活动描述")
        private String description;
        @Schema(description = "封面图片URL")
        private String coverImage;
        @Schema(description = "组织者ID")
        private Long organizerId;
        @Schema(description = "组织者信息")
        private OrganizerInfo organizer;
        @Schema(description = "活动地点")
        private String location;
        @Schema(description = "活动开始时间")
        private java.time.LocalDateTime startTime;
        @Schema(description = "活动结束时间")
        private java.time.LocalDateTime endTime;
        @Schema(description = "报名开始时间")
        private java.time.LocalDateTime registrationStartTime;
        @Schema(description = "报名结束时间")
        private java.time.LocalDateTime registrationEndTime;
        @Schema(description = "最大参与人数")
        private Integer maxParticipants;
        @Schema(description = "当前报名人数")
        private Integer currentParticipants;
        @Schema(description = "活动标签")
        private String tags;
        @Schema(description = "活动状态")
        private Integer status;
        @Schema(description = "活动分类ID")
        private Long categoryId;
        @Schema(description = "是否需要审核报名")
        private Boolean needReview;
        @Schema(description = "浏览次数")
        private Integer viewCount;
        @Schema(description = "收藏次数")
        private Integer favoriteCount;
        @Schema(description = "评论数量")
        private Integer commentCount;
        @Schema(description = "创建时间")
        private java.time.LocalDateTime createTime;
        @Schema(description = "更新时间")
        private java.time.LocalDateTime updateTime;
    }

    /**
     * 组织者信息
     */
    @Data
    @Schema(description = "组织者信息")
    public static class OrganizerInfo {
        @Schema(description = "用户ID")
        private Long id;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "昵称")
        private String nickname;
        @Schema(description = "真实姓名")
        private String realName;
    }
}
