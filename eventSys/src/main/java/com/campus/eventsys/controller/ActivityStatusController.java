package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.ActivityStatusService;
import com.campus.eventsys.service.UserService;
import com.campus.eventsys.utils.ActivityStatusUtil;
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

/**
 * 活动状态管理控制器
 * 提供活动状态查看和手动开始活动等功能
 * 
 * 活动状态码说明：
 * -2: 已下架（管理员强制下架）
 * -1: 已拒绝（审核未通过）
 * 0: 草稿（预留）
 * 1: 待审核（创建后默认状态）
 * 2: 报名中（审核通过，未到开始时间）
 * 3: 即将开始（开始前30分钟内）
 * 4: 进行中（已开始但未结束）
 * 5: 已结束（超过结束时间）
 * 
 * 自动状态流转规则：
 * - 开始前30分钟：2(报名中) → 3(即将开始)
 * - 到达开始时间：2/3 → 4(进行中)
 * - 到达结束时间：2/3/4 → 5(已结束)
 */
@Tag(name = "活动状态管理", description = "活动状态查看和手动开始活动接口\n\n" +
        "活动状态码说明：\n" +
        "- -2: 已下架（管理员强制下架）\n" +
        "- -1: 已拒绝（审核未通过）\n" +
        "- 0: 草稿（预留）\n" +
        "- 1: 待审核（创建后默认状态）\n" +
        "- 2: 报名中（审核通过，未到开始时间）\n" +
        "- 3: 即将开始（开始前30分钟内）\n" +
        "- 4: 进行中（已开始但未结束）\n" +
        "- 5: 已结束（超过结束时间）\n\n" +
        "自动状态流转规则：\n" +
        "- 开始前30分钟：2(报名中) → 3(即将开始)\n" +
        "- 到达开始时间：2/3 → 4(进行中)\n" +
        "- 到达结束时间：2/3/4 → 5(已结束)")
@RestController
@RequestMapping("/activities/status")
@RequiredArgsConstructor
public class ActivityStatusController {

    private final ActivityService activityService;
    private final ActivityStatusService statusService;
    private final UserService userService;

    /**
     * 获取活动当前状态信息
     */
    @Operation(summary = "获取活动状态",
               description = "获取活动的当前状态和预期状态信息。\n\n" +
                           "返回字段说明：\n" +
                           "- currentStatus: 当前状态码\n" +
                           "- currentStatusName: 当前状态名称\n" +
                           "- expectedStatus: 根据当前时间计算的预期状态码\n" +
                           "- expectedStatusName: 预期状态名称\n" +
                           "- needUpdate: 当前状态是否与预期状态不一致（true时需调用刷新接口）\n" +
                           "- startTime: 活动开始时间\n" +
                           "- endTime: 活动结束时间")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @GetMapping("/info/{activityId}")
    public Result<StatusInfo> getStatusInfo(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        int expectedStatus = statusService.getExpectedStatus(activity);
        
        StatusInfo info = new StatusInfo();
        info.setActivityId(activityId);
        info.setCurrentStatus(activity.getStatus());
        info.setCurrentStatusName(getStatusName(activity.getStatus()));
        info.setExpectedStatus(expectedStatus);
        info.setExpectedStatusName(getStatusName(expectedStatus));
        info.setStartTime(activity.getStartTime());
        info.setEndTime(activity.getEndTime());
        info.setNeedUpdate(activity.getStatus() != expectedStatus);

        return Result.success(info);
    }

    /**
     * 手动开始活动
     * 组织者或管理员可以手动开始活动
     */
    @Operation(summary = "开始活动",
               description = "手动开始活动，只有组织者或管理员可以操作。\n\n" +
                           "状态变更：2(报名中) 或 3(即将开始) → 4(进行中)\n" +
                           "仅当活动处于报名中或即将开始状态时才可手动开始")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "开始成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者或管理员）"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "活动状态不允许开始")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/start/{activityId}")
    public Result<Void> startActivity(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查活动是否存在
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 检查当前状态是否允许开始
        if (activity.getStatus() != 2 && activity.getStatus() != 3) {
            return Result.error("活动状态不允许开始，当前状态：" + getStatusName(activity.getStatus()));
        }

        boolean success = statusService.startActivity(activityId, user.getId());
        if (!success) {
            return Result.error("开始活动失败，您可能没有权限");
        }

        return Result.success("活动已开始", null);
    }

    /**
     * 刷新活动状态（自动根据时间更新）
     */
    @Operation(summary = "刷新活动状态",
               description = "根据当前时间自动更新活动状态，返回刷新后的状态信息。\n\n" +
                           "自动流转规则：\n" +
                           "- 开始前30分钟：2(报名中) → 3(即将开始)\n" +
                           "- 到达开始时间：2/3 → 4(进行中)\n" +
                           "- 到达结束时间：2/3/4 → 5(已结束)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @GetMapping("/refresh/{activityId}")
    public Result<StatusInfo> refreshStatus(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        Activity activity = statusService.autoUpdateStatus(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        int expectedStatus = statusService.getExpectedStatus(activity);
        
        StatusInfo info = new StatusInfo();
        info.setActivityId(activityId);
        info.setCurrentStatus(activity.getStatus());
        info.setCurrentStatusName(getStatusName(activity.getStatus()));
        info.setExpectedStatus(expectedStatus);
        info.setExpectedStatusName(getStatusName(expectedStatus));
        info.setStartTime(activity.getStartTime());
        info.setEndTime(activity.getEndTime());
        info.setNeedUpdate(false);

        return Result.success(info);
    }

    /**
     * 管理员强制更新所有活动状态
     */
    @Operation(summary = "批量刷新状态", description = "管理员强制检查并更新所有活动状态。通常由定时任务自动调用，也可手动触发。\n\n" +
                "适用场景：定时任务未运行时，管理员可手动触发全量状态刷新")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refresh-all")
    public Result<Void> refreshAllStatus() {
        statusService.checkAndUpdateAllActivities();
        return Result.success("批量刷新完成", null);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        return ActivityStatusUtil.getActivityStatusName(status);
    }

    /**
     * 状态信息DTO
     */
    @Data
    @Schema(description = "活动状态信息")
    public static class StatusInfo {
        @Schema(description = "活动ID")
        private Long activityId;
        @Schema(description = "当前状态码")
        private Integer currentStatus;
        @Schema(description = "当前状态名称")
        private String currentStatusName;
        @Schema(description = "预期状态码（根据时间计算）")
        private Integer expectedStatus;
        @Schema(description = "预期状态名称")
        private String expectedStatusName;
        @Schema(description = "活动开始时间")
        private java.time.LocalDateTime startTime;
        @Schema(description = "活动结束时间")
        private java.time.LocalDateTime endTime;
        @Schema(description = "是否需要更新状态")
        private Boolean needUpdate;
    }
}
