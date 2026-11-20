package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动公开查询控制器
 * 提供活动的公开查询接口，无需登录即可访问
 * 
 * 活动状态说明：
 * -2: 已下架（管理员强制下架）
 * -1: 已拒绝（审核未通过）
 * 0: 草稿（预留）
 * 1: 待审核（创建后默认状态）
 * 2: 报名中（审核通过，未到开始时间）
 * 3: 即将开始（开始前30分钟内）
 * 4: 进行中（已开始但未结束）
 * 5: 已结束（超过结束时间）
 */
@Tag(name = "活动查询", description = "活动的公开查询接口（无需登录）。只返回已通过审核的活动（状态 >= 2）\n\n" +
        "活动状态码：\n" +
        "- 2: 报名中\n" +
        "- 3: 即将开始\n" +
        "- 4: 进行中\n" +
        "- 5: 已结束")
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final com.campus.eventsys.service.ActivityService activityService;

    /**
     * 获取活动列表（分页）
     * 公开接口，无需登录
     */
    @Operation(summary = "获取活动列表",
               description = "分页查询活动列表，支持关键词搜索、状态筛选和分类筛选。\n" +
                           "只返回已通过审核的活动（状态 >= 2）。按创建时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public Result<Page<Activity>> list(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词（活动标题）") @RequestParam(required = false) String keyword,
            @Parameter(description = "活动状态：2-报名中, 3-即将开始, 4-进行中, 5-已结束") @RequestParam(required = false) Integer status,
            @Parameter(description = "活动分类ID") @RequestParam(required = false) Long categoryId) {

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        // 默认查询已审核通过且未删除的活动
        // 状态说明：-2-已下架, -1-已拒绝, 0-草稿, 1-待审核, 2-已通过/报名中, 3-即将开始, 4-进行中, 5-已结束
        // 公开查询只显示已通过审核的活动（状态 >= 2）
        wrapper.ge(Activity::getStatus, 2)  // 只显示已通过审核的活动
               .orderByDesc(Activity::getCreateTime);

        // 关键词搜索（活动标题）
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Activity::getTitle, keyword);
        }

        // 按状态筛选
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }

        // 按分类筛选
        if (categoryId != null) {
            wrapper.eq(Activity::getCategoryId, categoryId);
        }

        Page<Activity> pageResult = activityService.page(new Page<>(page, size), wrapper);
        return Result.success(pageResult);
    }

    /**
     * 获取活动详情
     * 公开接口，无需登录
     */
    @Operation(summary = "获取活动详情", description = "根据活动ID获取详细信息，同时浏览次数+1。公开接口，无需登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @GetMapping("/detail/{id}")
    public Result<Activity> detail(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        // 浏览次数+1（历史数据浏览数可能为空，做空值防御）
        activity.setViewCount(activity.getViewCount() == null ? 1 : activity.getViewCount() + 1);
        activityService.updateById(activity);
        return Result.success(activity);
    }
}
