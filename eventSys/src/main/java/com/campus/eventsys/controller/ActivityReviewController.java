package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.BusinessException;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.ActivityReview;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityReviewService;
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

import com.campus.eventsys.entity.ReviewStatisticsVO;

import java.util.Map;

/**
 * 活动评价控制器
 * 提供活动评价的提交、查看、回复、删除及统计功能
 */
@Tag(name = "活动评价", description = "活动评价与反馈相关接口\n\n" +
        "业务规则：\n" +
        "- 仅已结束活动（status=5）且已签到成功的参与者可评价\n" +
        "- 每个用户对同一活动仅限评价一次\n" +
        "- 评价提交后不允许修改\n" +
        "- 组织者回复可覆盖更新\n\n" +
        "公开接口（无需登录）：\n" +
        "- 查看活动评价列表\n" +
        "- 获取活动评价统计")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ActivityReviewController {

    private final ActivityReviewService reviewService;
    private final UserService userService;
    private final ActivityPermissionService permissionService;

    /**
     * UC76 - 提交活动评价
     */
    @Operation(summary = "提交活动评价", description = "对已结束的活动提交评价。\n\n" +
                "前提条件：\n" +
                "1. 活动状态为已结束（status=5）\n" +
                "2. 用户已报名该活动\n" +
                "3. 用户已完成该活动的签到\n" +
                "4. 每个用户对同一活动仅能评价一次\n\n" +
                "成功响应data结构：{ \"reviewId\": 1 }")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "评价成功，返回评价ID"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "活动未结束/未参与/未签到"),
            @ApiResponse(responseCode = "400", description = "重复评价/参数错误")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/create")
    public Result<Map<String, Object>> createReview(
            @RequestBody Map<String, Object> body,
            @Parameter(hidden = true) Authentication authentication) {
        User user = getCurrentUser(authentication);

        // 从JSON请求体中提取参数
        Long activityId = body.get("activityId") != null ? ((Number) body.get("activityId")).longValue() : null;
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        String content = body.get("content") != null ? body.get("content").toString() : null;

        try {
            ActivityReview review = reviewService.createReview(activityId, user.getId(), rating, content);
            Map<String, Object> data = Map.of("reviewId", review.getId());
            return Result.success("评价成功", data);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * UC77 - 获取某活动的评价列表（公开接口）
     */
    @Operation(summary = "获取活动评价列表", description = "获取指定活动的评价列表（分页）。公开接口，无需登录。\n\n" +
                "返回数据结构：\n" +
                "- data.records: 评价列表，每条包含 id/activityId/userId/rating/content/reply/createTime/updateTime/user\n" +
                "- data.user: 评价用户信息（id、nickname、avatar）\n" +
                "- data.total: 总记录数\n" +
                "- data.current: 当前页码\n" +
                "- data.size: 每页大小\n" +
                "- data.pages: 总页数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/activity/{activityId}")
    public Result<Page<ActivityReview>> getActivityReviews(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") int size) {
        Page<ActivityReview> result = reviewService.getActivityReviews(activityId, page, size);
        return Result.success(result);
    }

    /**
     * UC78 - 获取我的评价列表
     */
    @Operation(summary = "我的评价", description = "获取当前登录用户提交的所有评价列表（分页）。\n\n" +
                "返回数据结构同 GET /reviews/activity/{activityId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my")
    public Result<Page<ActivityReview>> getMyReviews(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        User user = getCurrentUser(authentication);
        Page<ActivityReview> result = reviewService.getMyReviews(user.getId(), page, size);
        return Result.success(result);
    }

    /**
     * UC79 - 组织者查看自己活动的评价
     */
    @Operation(summary = "组织者查看评价", description = "活动组织者查看自己创建的活动的评价列表（分页）。\n\n" +
                "参数说明：\n" +
                "- activityId: 可选，不传则查所有自己创建的活动评价\n" +
                "- 需要是活动的组织者才能查看\n\n" +
                "返回数据结构同 GET /reviews/activity/{activityId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "非组织者")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/organizer")
    public Result<Page<ActivityReview>> getOrganizerReviews(
            @Parameter(description = "活动ID（可选，不传查所有自己活动的评价）") @RequestParam(required = false) Long activityId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        User user = getCurrentUser(authentication);
        // 当指定活动ID时，校验当前用户是否为该活动的组织者
        if (activityId != null) {
            boolean isOrganizer = permissionService.isOrganizer(activityId, user.getId());
            if (!isOrganizer) {
                return Result.error(403, "您不是该活动的组织者，无权查看评价");
            }
        }
        Page<ActivityReview> result = reviewService.getOrganizerReviews(user.getId(), activityId, page, size);
        return Result.success(result);
    }

    /**
     * UC80 - 组织者回复评价
     */
    @Operation(summary = "回复评价", description = "组织者回复指定评价。\n\n" +
                "权限：仅该评价对应活动的组织者可回复\n" +
                "重复调用回复接口会覆盖之前的回复内容")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "回复成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "非组织者无权回复"),
            @ApiResponse(responseCode = "404", description = "评价不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/reply/{reviewId}")
    public Result<Void> replyReview(
            @Parameter(description = "评价ID", required = true) @PathVariable Long reviewId,
            @Parameter(description = "回复内容，1-500字", required = true) @RequestParam String reply,
            @Parameter(hidden = true) Authentication authentication) {
        User user = getCurrentUser(authentication);

        try {
            reviewService.replyReview(reviewId, user.getId(), reply);
            return Result.success("回复成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * UC81 - 管理员删除评价
     */
    @Operation(summary = "删除评价", description = "管理员删除指定评价。\n\n" +
                "删除后系统会自动重新计算该活动的平均评分和评价数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "非管理员"),
            @ApiResponse(responseCode = "404", description = "评价不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{reviewId}")
    public Result<Void> deleteReview(
            @Parameter(description = "评价ID", required = true) @PathVariable Long reviewId,
            @Parameter(hidden = true) Authentication authentication) {
        User user = getCurrentUser(authentication);

        // 权限校验：仅管理员
        if (user.getRole() == null || user.getRole() != 1) {
            return Result.error(403, "仅管理员可删除评价");
        }

        try {
            reviewService.deleteReview(reviewId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * 获取活动评价统计（公开接口）
     */
    @Operation(summary = "获取评价统计", description = "获取指定活动的评价统计数据。公开接口，无需登录。\n\n" +
                "返回数据结构：\n" +
                "- activityId: 活动ID\n" +
                "- avgRating: 平均评分（保留1位小数），无评价时为0.0\n" +
                "- totalCount: 总评价数\n" +
                "- ratingDistribution: 各星级分布，key为星级(\"5\"/\"4\"/\"3\"/\"2\"/\"1\")，value为该星级评价数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @GetMapping("/statistics/{activityId}")
    public Result<ReviewStatisticsVO> getReviewStatistics(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        try {
            ReviewStatisticsVO statistics = reviewService.getReviewStatistics(activityId);
            return Result.success(statistics);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }
}
