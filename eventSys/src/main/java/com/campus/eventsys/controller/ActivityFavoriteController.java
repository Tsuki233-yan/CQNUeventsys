package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityFavorite;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityFavoriteService;
import com.campus.eventsys.service.ActivityService;
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
import java.util.stream.Collectors;

/**
 * 活动收藏控制器
 * 提供活动收藏相关接口
 */
@Tag(name = "活动收藏", description = "活动收藏相关接口\n\n" +
        "收藏状态：收藏/未收藏\n" +
        "收藏数量统计接口为公开接口，无需登录")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class ActivityFavoriteController {

    private final ActivityFavoriteService favoriteService;
    private final ActivityService activityService;
    private final UserService userService;

    /**
     * 收藏活动
     */
    @Operation(summary = "收藏活动", description = "收藏指定活动。重复收藏会返回错误提示。\n\n" +
                "收藏后活动 favoriteCount 字段会 +1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "收藏成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "已收藏")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/add/{activityId}")
    public Result<Void> favorite(
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

        // 检查是否已收藏
        if (favoriteService.isFavorited(activityId, user.getId())) {
            return Result.error("您已收藏该活动");
        }

        boolean success = favoriteService.favorite(activityId, user.getId());
        if (!success) {
            return Result.error("收藏失败");
        }

        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏
     */
    @Operation(summary = "取消收藏", description = "取消对活动的收藏。未收藏时调用会返回错误提示。\n\n" +
                "取消后活动 favoriteCount 字段会 -1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "未收藏")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/remove/{activityId}")
    public Result<Void> unfavorite(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查是否已收藏
        if (!favoriteService.isFavorited(activityId, user.getId())) {
            return Result.error("您未收藏该活动");
        }

        boolean success = favoriteService.unfavorite(activityId, user.getId());
        if (!success) {
            return Result.error("取消收藏失败");
        }

        return Result.success("取消收藏成功", null);
    }

    /**
     * 检查是否已收藏
     */
    @Operation(summary = "检查收藏状态", description = "检查当前用户是否已收藏指定活动。返回true/false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/status/{activityId}")
    public Result<Boolean> isFavorited(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        boolean favorited = favoriteService.isFavorited(activityId, user.getId());
        return Result.success(favorited);
    }

    /**
     * 获取我的收藏列表
     */
    @Operation(summary = "我的收藏", description = "获取当前用户收藏的所有活动列表，按收藏时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my-favorites")
    public Result<List<Activity>> getMyFavorites(
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<ActivityFavorite> favorites = favoriteService.getUserFavorites(user.getId());

        // 加载活动详情
        List<Activity> activities = favorites.stream()
                .map(fav -> activityService.getById(fav.getActivityId()))
                .filter(activity -> activity != null)
                .collect(Collectors.toList());

        return Result.success(activities);
    }

    /**
     * 获取活动的收藏数量（公开接口）
     */
    @Operation(summary = "收藏数量", description = "获取指定活动的收藏数量。公开接口，无需登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/count/{activityId}")
    public Result<Long> getFavoriteCount(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        long count = favoriteService.getFavoriteCount(activityId);
        return Result.success(count);
    }
}
