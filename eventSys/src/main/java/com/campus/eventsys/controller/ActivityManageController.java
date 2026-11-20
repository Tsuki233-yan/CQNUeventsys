package com.campus.eventsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CategoryService;
import com.campus.eventsys.service.NotificationService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 活动管理控制器
 * 提供活动组织者使用的管理接口：创建、修改、删除活动
 * 
 * 活动状态流转：
 * 1. 创建活动 → 状态变为 1(待审核)
 * 2. 管理员审核通过 → 状态变为 2(报名中)
 * 3. 到达开始时间前30分钟 → 自动变为 3(即将开始)
 * 4. 组织者/管理员手动开始 或 到达开始时间 → 变为 4(进行中)
 * 5. 到达结束时间 → 自动变为 5(已结束)
 * 
 * 状态码说明：
 * -2: 已下架, -1: 已拒绝, 0: 草稿, 1: 待审核, 2: 报名中, 3: 即将开始, 4: 进行中, 5: 已结束
 */
@Tag(name = "活动管理-组织者", description = "活动组织者使用的管理接口。创建后需要管理员审核通过才能展示\n\n" +
        "活动状态码说明：\n" +
        "- -2: 已下架（管理员强制下架）\n" +
        "- -1: 已拒绝（审核未通过）\n" +
        "- 0: 草稿（预留）\n" +
        "- 1: 待审核（创建后默认状态）\n" +
        "- 2: 报名中（审核通过，接受报名）\n" +
        "- 3: 即将开始（开始前30分钟内）\n" +
        "- 4: 进行中（活动已开始）\n" +
        "- 5: 已结束（超过结束时间）")
@RestController
@RequestMapping("/activities/manage")
@RequiredArgsConstructor
public class ActivityManageController {

    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityPermissionService permissionService;
    private final ActivityRegistrationService registrationService;
    private final ActivityResultService activityResultService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    /**
     * 创建活动
     * 创建后状态为待审核(1)，需要管理员审核通过后才能展示
     */
    @Operation(summary = "创建活动",
               description = "创建新活动，创建者自动成为组织者。\n\n" +
                           "创建后活动状态为 1(待审核)，需要管理员审核通过后才能展示。\n" +
                           "返回data为创建的活动ID，用于后续上传封面图片。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功，data返回活动ID"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/create")
    public Result<Long> create(
            @Parameter(description = "活动信息", required = true) @RequestBody Activity activity,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前登录用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 设置组织者ID
        activity.setOrganizerId(user.getId());

        // 新创建的活动状态为待审核(1)，需要管理员审核通过后才能展示
        activity.setStatus(1);

        // 初始化统计数据
        activity.setViewCount(0);
        activity.setFavoriteCount(0);
        activity.setCommentCount(0);
        activity.setCurrentParticipants(0);

        activityService.save(activity);

        // 查询获取刚创建的活动ID（SQLite不支持getGeneratedKeys）
        Activity createdActivity = activityService.getOne(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getOrganizerId, user.getId())
                        .eq(Activity::getTitle, activity.getTitle())
                        .orderByDesc(Activity::getCreateTime)
                        .last("LIMIT 1")
        );
        Long activityId = createdActivity != null ? createdActivity.getId() : null;

        // 将创建者设置为活动组织者
        permissionService.setUserAsOrganizer(activityId, user.getId());

        // 更新用户创建活动统计
        user.setOrganizedCount(user.getOrganizedCount() + 1);
        userService.updateById(user);

        // 更新分类活动数量统计
        if (activity.getCategoryId() != null) {
            categoryService.incrementActivityCount(activity.getCategoryId());
        }

        // 返回创建的活动ID，用于后续上传封面图片
        return Result.success("创建成功，等待管理员审核", activityId);
    }

    /**
     * 更新活动信息
     */
    @Operation(summary = "更新活动", description = "更新活动信息，只有活动组织者可以修改。\n\n" +
                "不允许修改 organizerId（组织者）。若需重新提交审核，可修改后联系管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id,
            @Parameter(description = "活动信息", required = true) @RequestBody Activity activity,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Activity existActivity = activityService.getById(id);
        if (existActivity == null) {
            return Result.error("活动不存在");
        }

        // 检查是否有权限修改（必须是组织者）
        if (!permissionService.canModifyActivity(id, user.getId())) {
            return Result.error("无权限：只有活动组织者可以修改活动信息");
        }

        activity.setId(id);
        // 不允许修改组织者
        activity.setOrganizerId(null);
        // 不允许通过更新接口篡改审核状态与统计数据：
        // 审核状态只能由管理员流转（防止组织者绕过审核自行把活动改为报名中），统计字段由系统维护
        activity.setStatus(null);
        activity.setCurrentParticipants(null);
        activity.setViewCount(null);
        activity.setFavoriteCount(null);
        activity.setCommentCount(null);
        activity.setAvgRating(null);
        activity.setReviewCount(null);
        activity.setCreateTime(null);
        activity.setDeleted(null);

        activityService.updateById(activity);
        return Result.success("更新成功", null);
    }

    /**
     * 删除活动（逻辑删除）
     */
    @Operation(summary = "删除活动", description = "删除活动（逻辑删除），只有活动组织者可以删除。\n\n" +
                "删除操作会同时清理：报名记录、成果展示贴，并更新相关统计数据（分类活动数、用户创建/参与数）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 检查是否有权限删除（必须是组织者）
        if (!permissionService.isOrganizer(id, user.getId())) {
            return Result.error("无权限：只有活动组织者可以删除活动");
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
     * 获取我创建的活动列表
     */
    @Operation(summary = "我创建的活动", description = "获取当前用户作为组织者创建的所有活动（包括所有状态：待审核、报名中、进行中、已结束等）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my-created")
    public Result<List<Activity>> getMyCreatedActivities(
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 查询用户创建的活动（包括所有状态）
        List<Activity> activities = activityService.list(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getOrganizerId, user.getId())
                        .eq(Activity::getDeleted, 0)
                        .orderByDesc(Activity::getCreateTime)
        );

        return Result.success(activities);
    }

    // ==================== 报名人员管理接口 ====================

    /**
     * 获取活动的所有报名人员（包括组织者和参与者）
     */
    @Operation(summary = "获取活动报名列表", description = "获取活动的所有报名人员列表，包括组织者和参与者，只有组织者可以查看。\n\n" +
                "返回数据中每条记录包含：id、activityId、userId、username、realName、studentId、role(1-组织者/2-参与者)、roleName、status(0-待审核/1-已通过/2-已拒绝/3-已签到)、statusName、remark、images、reviewRemark、createTime")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{activityId}/registrations")
    public Result<List<RegistrationDetail>> getActivityRegistrations(
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

        // 检查是否有权限查看（必须是组织者）
        if (!permissionService.isOrganizer(activityId, user.getId())) {
            return Result.error("无权限：只有活动组织者可以查看报名列表");
        }

        // 获取所有报名记录
        List<ActivityRegistration> registrations = registrationService.getActivityRegistrations(activityId);
        
        // 转换为详细信息
        List<RegistrationDetail> details = registrations.stream().map(reg -> {
            RegistrationDetail detail = new RegistrationDetail();
            detail.setId(reg.getId());
            detail.setActivityId(reg.getActivityId());
            detail.setUserId(reg.getUserId());
            detail.setRole(reg.getRole());
            detail.setRoleName(reg.getRole() == 1 ? "组织者" : "参与者");
            detail.setStatus(reg.getStatus());
            detail.setStatusName(getStatusName(reg.getStatus()));
            detail.setRemark(reg.getRemark());
            detail.setImages(reg.getImages());
            detail.setReviewRemark(reg.getReviewRemark());
            detail.setCreateTime(reg.getCreateTime());
            
            // 查询用户信息
            User regUser = userService.getById(reg.getUserId());
            if (regUser != null) {
                detail.setUsername(regUser.getUsername());
                detail.setRealName(regUser.getRealName());
                detail.setStudentId(regUser.getStudentId());
            }
            
            return detail;
        }).collect(Collectors.toList());

        return Result.success(details);
    }

    /**
     * 获取活动的参与者列表（不包括组织者）
     */
    @Operation(summary = "获取活动参与者列表", description = "获取活动的参与者列表（不包括组织者），只有组织者可以查看。\n\n" +
                "与获取报名列表接口的区别：此接口仅返回 role=2(参与者) 的记录，不包含组织者")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{activityId}/participants")
    public Result<List<RegistrationDetail>> getActivityParticipants(
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

        // 检查是否有权限查看（必须是组织者）
        if (!permissionService.isOrganizer(activityId, user.getId())) {
            return Result.error("无权限：只有活动组织者可以查看参与者列表");
        }

        // 获取参与者报名记录
        List<ActivityRegistration> registrations = registrationService.getActivityParticipants(activityId);
        
        // 转换为详细信息
        List<RegistrationDetail> details = registrations.stream().map(reg -> {
            RegistrationDetail detail = new RegistrationDetail();
            detail.setId(reg.getId());
            detail.setActivityId(reg.getActivityId());
            detail.setUserId(reg.getUserId());
            detail.setRole(reg.getRole());
            detail.setRoleName("参与者");
            detail.setStatus(reg.getStatus());
            detail.setStatusName(getStatusName(reg.getStatus()));
            detail.setRemark(reg.getRemark());
            detail.setImages(reg.getImages());
            detail.setReviewRemark(reg.getReviewRemark());
            detail.setCreateTime(reg.getCreateTime());
            
            // 查询用户信息
            User regUser = userService.getById(reg.getUserId());
            if (regUser != null) {
                detail.setUsername(regUser.getUsername());
                detail.setRealName(regUser.getRealName());
                detail.setStudentId(regUser.getStudentId());
            }
            
            return detail;
        }).collect(Collectors.toList());

        return Result.success(details);
    }

    /**
     * 审核报名申请
     */
    @Operation(summary = "审核报名", description = "审核活动报名申请，只有组织者可以审核。\n\n" +
                "请求体参数：\n" +
                "- status: 审核状态，1-通过，2-拒绝（必填）\n" +
                "- remark: 审核备注（可选）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "审核成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "报名记录不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/registrations/{registrationId}/review")
    public Result<Void> reviewRegistration(
            @Parameter(description = "报名记录ID", required = true) @PathVariable Long registrationId,
            @Parameter(description = "审核请求", required = true) @RequestBody ReviewRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 获取报名记录
        ActivityRegistration registration = registrationService.getById(registrationId);
        if (registration == null || registration.getDeleted() == 1) {
            return Result.error("报名记录不存在");
        }

        // 检查是否有权限审核（必须是该活动的组织者）
        if (!permissionService.isOrganizer(registration.getActivityId(), user.getId())) {
            return Result.error("无权限：只有活动组织者可以审核报名");
        }

        // 校验审核状态：仅允许 1-通过 或 2-拒绝（防止传入其他状态值篡改报名记录）
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("审核状态不合法：仅允许 1-通过 或 2-拒绝");
        }

        // 执行审核
        boolean success = registrationService.reviewRegistration(
                registrationId, request.getStatus(), user.getId(), request.getRemark());
        
        if (!success) {
            return Result.error("审核失败，可能报名记录状态不正确");
        }

        String message = request.getStatus() == 1 ? "审核通过" : "审核拒绝";
        return Result.success(message, null);
    }

    /**
     * 移除活动参与者
     */
    @Operation(summary = "移除参与者", description = "移除活动参与者，只有组织者可以移除。不能移除组织者角色，组织者需通过移除管理者接口处理。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）或不能移除组织者"),
            @ApiResponse(responseCode = "404", description = "报名记录不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/registrations/{registrationId}/remove")
    public Result<Void> removeParticipant(
            @Parameter(description = "报名记录ID", required = true) @PathVariable Long registrationId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 获取报名记录
        ActivityRegistration registration = registrationService.getById(registrationId);
        if (registration == null || registration.getDeleted() == 1) {
            return Result.error("报名记录不存在");
        }

        // 检查是否有权限移除（必须是该活动的组织者）
        if (!permissionService.isOrganizer(registration.getActivityId(), user.getId())) {
            return Result.error("无权限：只有活动组织者可以移除参与者");
        }

        // 不能移除组织者
        if (registration.getRole() == 1) {
            return Result.error("不能移除组织者，请使用移除组织者接口");
        }

        // 执行移除
        boolean success = registrationService.removeParticipant(registrationId);
        
        if (!success) {
            return Result.error("移除失败");
        }

        return Result.success("移除成功", null);
    }

    // ==================== 组织者/管理者管理接口 ====================

    /**
     * 获取活动的所有管理者（包括组织者和被添加的管理者）
     */
    @Operation(summary = "获取活动管理者列表", description = "获取活动的所有管理者列表，包括活动创建者（组织者）和被添加的管理者，只有组织者可以查看\n\n" +
            "【角色说明】\n" +
            "- roleType=1: 组织者（活动创建者）\n" +
            "- roleType=2: 管理者（被组织者添加的协助管理人员）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{activityId}/organizers")
    public Result<List<OrganizerDetail>> getActivityOrganizers(
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

        // 检查是否有权限查看（必须是组织者）
        if (!permissionService.isOrganizer(activityId, user.getId())) {
            return Result.error("无权限：只有活动组织者可以查看管理者列表");
        }

        // 获取所有组织者记录
        List<ActivityRegistration> organizers = permissionService.getActivityOrganizers(activityId);
        
        // 转换为详细信息
        List<OrganizerDetail> details = organizers.stream().map(reg -> {
            OrganizerDetail detail = new OrganizerDetail();
            detail.setId(reg.getId());
            detail.setActivityId(reg.getActivityId());
            detail.setUserId(reg.getUserId());
            detail.setCreateTime(reg.getCreateTime());
            
            // 查询用户信息
            User orgUser = userService.getById(reg.getUserId());
            if (orgUser != null) {
                detail.setUsername(orgUser.getUsername());
                detail.setRealName(orgUser.getRealName());
                detail.setStudentId(orgUser.getStudentId());
            }
            
            // 标记是否是自己
            detail.setIsSelf(reg.getUserId().equals(user.getId()));
            
            // 标记是否是活动创建者（组织者）还是被添加的管理者
            boolean isCreator = activity.getOrganizerId() != null && 
                               activity.getOrganizerId().equals(reg.getUserId());
            detail.setRoleType(isCreator ? 1 : 2);
            detail.setRoleName(isCreator ? "组织者" : "管理者");
            
            return detail;
        }).collect(Collectors.toList());

        return Result.success(details);
    }

    /**
     * 添加活动管理者（协助管理活动的用户，区别于活动创建者-组织者）
     */
    @Operation(summary = "添加管理者", description = "添加用户为活动管理者，只有活动组织者可以添加其他管理者。\n\n" +
            "【角色说明】\n" +
            "- 组织者：活动创建者，拥有最高权限\n" +
            "- 管理者：被组织者添加的协助管理人员\n\n" +
            "支持通过用户ID、用户名或学号查找用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动或用户不存在"),
            @ApiResponse(responseCode = "400", description = "用户已经是管理者")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{activityId}/organizers")
    public Result<Void> addOrganizer(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "添加管理者请求", required = true) @RequestBody AddOrganizerRequest request,
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

        // 检查目标用户是否存在（支持通过ID、用户名或学号查找）
        User targetUser = null;
        String userIdStr = String.valueOf(request.getUserId());
        
        // 首先尝试作为ID查询
        targetUser = userService.getById(request.getUserId());
        
        // 如果未找到，尝试作为用户名（学号）查询
        if (targetUser == null) {
            targetUser = userService.getByUsername(userIdStr);
        }
        
        // 如果仍未找到，尝试作为学号查询
        if (targetUser == null) {
            targetUser = userService.getByStudentId(userIdStr);
        }
        
        if (targetUser == null) {
            return Result.error("要添加的用户不存在");
        }

        // 执行添加
        boolean success = permissionService.addOrganizer(activityId, targetUser.getId(), user.getId());
        
        if (!success) {
            return Result.error("添加失败，可能您不是组织者或用户已经是管理者");
        }

        // 发送通知给被添加的管理者
        String organizerName = user.getNickname() != null ? user.getNickname() : user.getUsername();
        notificationService.sendOrganizerChange(
                targetUser.getId(), 
                activityId, 
                activity.getTitle(), 
                organizerName, 
                true
        );

        return Result.success("添加管理者成功", null);
    }

    /**
     * 移除活动管理者
     */
    @Operation(summary = "移除管理者", description = "移除活动管理者，只有活动组织者可以移除其他管理者，不能移除自己\n\n" +
            "【注意】此接口只能移除被添加的管理者，不能移除活动创建者（组织者）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）或不能移除自己"),
            @ApiResponse(responseCode = "404", description = "用户不是管理者")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{activityId}/organizers/{targetUserId}/remove")
    public Result<Void> removeOrganizer(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "要移除的用户ID", required = true) @PathVariable Long targetUserId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 执行移除
        boolean success = permissionService.removeOrganizer(activityId, targetUserId, user.getId());
        
        if (!success) {
            return Result.error("移除失败，可能您不是组织者、不能移除自己或目标用户不是管理者");
        }

        // 发送通知给被移除的管理者
        User targetUser = userService.getById(targetUserId);
        if (targetUser != null) {
            String organizerName = user.getNickname() != null ? user.getNickname() : user.getUsername();
            Activity activity = activityService.getById(activityId);
            if (activity != null) {
                notificationService.sendOrganizerChange(
                        targetUserId, 
                        activityId, 
                        activity.getTitle(), 
                        organizerName, 
                        false
                );
            }
        }

        return Result.success("移除管理者成功", null);
    }

    // ==================== 辅助方法和DTO ====================

    private String getStatusName(Integer status) {
        return ActivityStatusUtil.getRegistrationStatusName(status);
    }

    @Data
    @Schema(description = "报名详细信息")
    public static class RegistrationDetail {
        @Schema(description = "报名记录ID")
        private Long id;
        @Schema(description = "活动ID")
        private Long activityId;
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "真实姓名")
        private String realName;
        @Schema(description = "学号")
        private String studentId;
        @Schema(description = "角色：1-组织者，2-参与者")
        private Integer role;
        @Schema(description = "角色名称")
        private String roleName;
        @Schema(description = "状态：0-待审核，1-已通过，2-已拒绝，3-已签到")
        private Integer status;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "报名备注")
        private String remark;
        @Schema(description = "报名图片")
        private String images;
        @Schema(description = "审核备注")
        private String reviewRemark;
        @Schema(description = "报名时间")
        private java.time.LocalDateTime createTime;
    }

    @Data
    @Schema(description = "管理者详细信息")
    public static class OrganizerDetail {
        @Schema(description = "报名记录ID")
        private Long id;
        @Schema(description = "活动ID")
        private Long activityId;
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "真实姓名")
        private String realName;
        @Schema(description = "学号")
        private String studentId;
        @Schema(description = "是否是自己")
        private Boolean isSelf;
        @Schema(description = "角色类型：1-组织者（活动创建者），2-管理者（被添加的协助人员）")
        private Integer roleType;
        @Schema(description = "角色名称：组织者/管理者")
        private String roleName;
        @Schema(description = "成为管理者时间")
        private java.time.LocalDateTime createTime;
    }

    @Data
    @Schema(description = "审核请求")
    public static class ReviewRequest {
        @Schema(description = "审核状态：1-通过，2-拒绝", required = true)
        private Integer status;
        @Schema(description = "审核备注")
        private String remark;
    }

    @Data
    @Schema(description = "添加管理者请求")
    public static class AddOrganizerRequest {
        @Schema(description = "要添加的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
        private Long userId;
    }

    /**
     * 上传活动封面图片
     * 支持jpg/png/gif格式，大小不超过5MB
     */
    @Operation(summary = "上传活动封面", 
               description = "上传活动封面图片，支持jpg/png/gif格式，大小不超过5MB。\n\n" +
                           "图片存储路径：uploads/images/activity_{activityId}/cover/\n" +
                           "返回图片访问URL：/uploads/images/activity_{activityId}/cover/{filename}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功，data返回图片URL"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者）"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "上传失败：文件为空/非图片格式/超过5MB")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/upload-cover/{activityId}", consumes = "multipart/form-data")
    public Result<String> uploadCoverImage(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "封面图片文件，支持jpg/png/gif格式", required = true)
            @RequestParam("cover") MultipartFile coverFile,
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

        // 检查是否有权限（必须是组织者）
        if (!permissionService.canModifyActivity(activityId, user.getId())) {
            return Result.error("无权限：只有活动组织者可以上传封面图片");
        }

        // 检查文件
        if (coverFile == null || coverFile.isEmpty()) {
            return Result.error("请选择要上传的封面图片");
        }

        // 检查文件类型
        String contentType = coverFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 检查文件大小（限制10MB）
        if (coverFile.getSize() > 10 * 1024 * 1024) {
            return Result.error("图片大小不能超过10MB");
        }

        try {
            // 创建上传目录
            Path activityDir = Paths.get(uploadPath, "activity_" + activityId, "cover").toAbsolutePath().normalize();
            if (!Files.exists(activityDir)) {
                Files.createDirectories(activityDir);
            }

            // 生成唯一文件名
            String originalFilename = coverFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path targetPath = activityDir.resolve(newFilename);
            coverFile.transferTo(targetPath.toFile());

            // 生成访问URL（与数据库中其他图片路径格式保持一致）
            String imageUrl = "/images/activity_" + activityId + "/cover/" + newFilename;

            // 更新活动的封面图片URL
            activity.setCoverImage(imageUrl);
            activityService.updateById(activity);

            return Result.success("封面图片上传成功", imageUrl);
        } catch (IOException e) {
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
}
