package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.CheckInRecord;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CheckInRecordService;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动签到控制器
 * 提供活动签到相关接口
 */
@Tag(name = "活动管理-签到", description = "活动签到相关接口\n\n" +
        "签到前提条件：\n" +
        "- 用户必须是活动的参与者（报名已通过）\n" +
        "- 活动必须处于进行中(4)状态\n\n" +
        "签到记录包含：活动ID、用户ID、签到时间等")
@RestController
@RequestMapping("/activities/checkin")
@RequiredArgsConstructor
public class ActivityCheckInController {

    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityPermissionService permissionService;
    private final CheckInRecordService checkInRecordService;

    /**
     * 活动签到
     */
    @Operation(summary = "活动签到", description = "活动参与者进行签到。签到前提：用户已报名且审核通过，活动处于进行中(4)状态。\n\n" +
                "重复签到会返回失败提示")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "签到成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是参与者或活动未开始）"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "已签到")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{activityId}")
    public Result<Void> checkIn(
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

        // 检查是否可以签到
        if (!permissionService.canCheckIn(activityId, user.getId())) {
            return Result.error("无法签到：您不是该活动的参与者，或活动尚未开始");
        }

        // 执行签到
        boolean success = checkInRecordService.checkIn(activityId, user.getId());
        if (!success) {
            return Result.error("签到失败：可能已签到");
        }

        return Result.success("签到成功", null);
    }

    /**
     * 检查是否已签到
     */
    @Operation(summary = "检查签到状态", description = "检查当前用户是否已在指定活动签到。返回true/false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/status/{activityId}")
    public Result<Boolean> checkInStatus(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean hasCheckedIn = checkInRecordService.hasCheckedIn(activityId, user.getId());
        return Result.success(hasCheckedIn);
    }

    /**
     * 获取活动的签到记录列表
     * 组织者可以查看
     */
    @Operation(summary = "活动签到列表", description = "获取指定活动的所有签到记录，只有组织者可以查看。\n\n" +
                "返回数据包含每条签到记录：id、activityId、userId、checkInTime等")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/list/{activityId}")
    public Result<List<CheckInRecord>> getActivityCheckInList(
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

        // 检查是否是组织者
        if (!permissionService.isOrganizer(activityId, user.getId())) {
            return Result.error("无权限：只有活动组织者可以查看签到列表");
        }

        List<CheckInRecord> records = checkInRecordService.getActivityCheckInRecords(activityId);
        return Result.success(records);
    }

    /**
     * 获取我的签到记录
     */
    @Operation(summary = "我的签到记录", description = "获取当前用户的所有签到记录列表，按签到时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my-records")
    public Result<List<CheckInRecord>> getMyCheckInRecords(
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<CheckInRecord> records = checkInRecordService.getUserCheckInRecords(user.getId());
        return Result.success(records);
    }
}
