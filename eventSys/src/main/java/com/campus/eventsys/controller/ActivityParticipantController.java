package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.config.TimeZoneConfig;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 活动参与者控制器
 * 提供活动参与者使用的接口：报名、查看角色等
 */
@Tag(name = "活动管理-参与者", description = "活动参与者使用的接口\n\n" +
        "报名状态说明：\n" +
        "- 0: 待审核\n" +
        "- 1: 已通过\n" +
        "- 2: 已拒绝\n" +
        "- 3: 已签到\n" +
        "- 4: 已取消\n\n" +
        "角色说明：1-组织者，2-参与者")
@RestController
@RequestMapping("/activities/participant")
@RequiredArgsConstructor
public class ActivityParticipantController {

    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityPermissionService permissionService;
    private final ActivityRegistrationService registrationService;
    private final com.campus.eventsys.service.ActivityResultService activityResultService;

    /**
     * 报名参加活动
     * 支持上传报名相关图片（如作品、资质证明等）
     * 
     * 【图片存储规范】
     * - 所有活动相关图片（含报名帖与展示贴）统一存储至活动展示贴指定的图片路径
     * - 共用同一套存储路径结构与后端接口逻辑
     * - 存储路径：uploads/images/activity_{活动ID}/
     * - 文件命名：UUID随机生成，确保唯一性
     * - 前端按文档规范直接消费，通过 /uploads/images/activity_{活动ID}/文件名 访问
     */
    @Operation(summary = "报名活动（支持图片上传）",
               description = "当前用户报名参加活动，支持上传报名相关图片（如作品、资质证明等）。\n\n" +
                           "报名校验规则：\n" +
                           "- 仅报名中(2)的活动可以报名\n" +
                           "- 报名时间需在报名开始时间与报名结束时间之间（未设置时视为不限制）\n" +
                           "- 报名人数不超过名额上限（maxParticipants=0 表示不限制）\n" +
                           "- needReview=true 的活动报名进入待审核(0)，由组织者审核；否则报名即通过(1)\n\n" +
                           "图片统一存储规范：\n" +
                           "- 存储位置：uploads/images/activity_{活动ID}/\n" +
                           "- 访问URL：/uploads/images/activity_{活动ID}/文件名\n\n" +
                           "请求格式：multipart/form-data\n" +
                           "- activityId: 活动ID（路径参数，必填）\n" +
                           "- remark: 报名备注/留言（可选）\n" +
                           "- images: 图片文件列表（可选，最多3张）\n\n" +
                           "图片限制：单张不超过10MB，仅支持图片格式")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "报名成功（无需审核活动message为\"报名成功\"，需审核活动message为\"报名成功，等待审核\"），data为null"),
            @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "业务错误：已报名/活动状态不可报名/报名未开始/报名已截止/名额已满/图片超过3张/非图片格式/图片超过10MB/图片上传失败")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/join/{activityId}", consumes = "multipart/form-data")
    public Result<Void> joinActivity(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "报名备注/留言") @RequestParam(required = false) String remark,
            @Parameter(description = "报名图片文件列表，支持jpg/png/gif等格式，最多3张")
            @RequestParam(required = false) List<MultipartFile> images,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查身份绑定：未绑定学号/工号的用户须先完善身份认证才能报名活动（管理员豁免，规则见 UserServiceImpl.isIdentityBound）
        if ((user.getRole() == null || user.getRole() != 1) && !userService.isIdentityBound(user)) {
            return Result.error("请先完善身份认证（绑定学号/工号）后再报名活动");
        }

        // 检查活动是否存在
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 检查是否已报名
        if (permissionService.getUserRole(activityId, user.getId()) != null) {
            return Result.error("您已报名该活动");
        }

        // 检查活动状态：仅报名中(2)的活动可以报名
        if (activity.getStatus() == null || activity.getStatus() != 2) {
            return Result.error("当前活动状态不可报名，仅报名中的活动可以报名");
        }

        // 检查报名时段（未设置报名开始/结束时间时视为不限制）
        LocalDateTime now = LocalDateTime.now(TimeZoneConfig.getDefaultZoneId());
        if (activity.getRegistrationStartTime() != null && now.isBefore(activity.getRegistrationStartTime())) {
            return Result.error("报名尚未开始");
        }
        if (activity.getRegistrationEndTime() != null && now.isAfter(activity.getRegistrationEndTime())) {
            return Result.error("报名已截止");
        }

        // 检查名额上限（maxParticipants 为 0 或 null 表示不限制）
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0
                && activity.getCurrentParticipants() != null
                && activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            return Result.error("报名名额已满");
        }

        // 处理图片上传
        String imagesJson = null;
        if (images != null && !images.isEmpty()) {
            // 限制图片数量
            if (images.size() > 3) {
                return Result.error("报名图片最多上传3张");
            }

            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                // 检查文件类型
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Result.error("只能上传图片文件: " + image.getOriginalFilename());
                }
                // 检查文件大小（限制10MB）
                if (image.getSize() > 10 * 1024 * 1024) {
                    return Result.error("图片大小不能超过10MB: " + image.getOriginalFilename());
                }

                try {
                    // 生成唯一文件名
                    String originalFilename = image.getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

                    // 保存文件到上传目录（与成果展示贴共用同一活动目录）
                    String imageUrl = activityResultService.saveImage(image, newFilename, activityId);
                    imageUrls.add(imageUrl);
                } catch (Exception e) {
                    return Result.error("图片上传失败: " + image.getOriginalFilename());
                }
            }
            // 转换为JSON数组格式
            imagesJson = "[" + String.join(",", imageUrls.stream().map(url -> "\"" + url + "\"").toArray(String[]::new)) + "]";
        }

        // 添加为参与者（带图片）
        // 根据活动配置决定报名状态：需要审核的活动进入待审核(0)，由组织者审核；无需审核的报名即通过(1)
        boolean needReview = Boolean.TRUE.equals(activity.getNeedReview());
        boolean success;
        if (needReview) {
            success = permissionService.addParticipantWithStatus(
                    activityId, user.getId(), ActivityRole.PARTICIPANT, remark, imagesJson, 0);
        } else {
            success = permissionService.addParticipantWithImages(
                    activityId, user.getId(), ActivityRole.PARTICIPANT, remark, imagesJson);
        }
        if (!success) {
            return Result.error("报名失败");
        }

        if (needReview) {
            // 待审核报名不占用名额，人数与参与统计由组织者审核通过后再更新
            return Result.success("报名成功，等待审核", null);
        }

        // 报名即通过，更新活动当前参与人数
        activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        activityService.updateById(activity);

        // 更新用户参与活动统计
        user.setParticipatedCount(user.getParticipatedCount() + 1);
        userService.updateById(user);

        return Result.success("报名成功", null);
    }

    /**
     * 取消报名
     */
    @Operation(summary = "取消报名", description = "取消当前用户对活动的报名。\n\n" +
                "校验规则：\n" +
                "- 仅报名中(2)的活动可以取消报名\n" +
                "- 已签到(3)的报名不可取消\n\n" +
                "名额回退规则：仅已通过(1)的报名取消时回退活动参与人数与用户参与统计（待审核报名未计入）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "未报名")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/cancel/{activityId}")
    public Result<Void> cancelJoin(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查是否已报名
        var registration = permissionService.getUserRole(activityId, user.getId());
        if (registration == null) {
            return Result.error("您未报名该活动");
        }

        // 已签到的报名不可取消（已实际参与活动）
        if (registration.getStatus() != null && registration.getStatus() == 3) {
            return Result.error("您已签到，无法取消报名");
        }

        // 获取活动信息用于状态与统计校验
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 仅报名中(2)的活动可以取消报名（与前端"活动开始后不能取消"的展示逻辑一致）
        if (activity.getStatus() == null || activity.getStatus() != 2) {
            return Result.error("当前活动状态不可取消报名");
        }

        // 取消报名（逻辑删除）
        registrationService.removeById(registration.getId());

        // 仅已通过(1)的报名占用名额：取消时回退活动人数与参与统计
        // 待审核(0)的报名从未计入统计，取消时无需回退
        if (registration.getStatus() != null && registration.getStatus() == 1) {
            // 减少活动当前参与人数
            if (activity.getCurrentParticipants() != null && activity.getCurrentParticipants() > 0) {
                activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
                activityService.updateById(activity);
            }

            // 减少用户参与活动统计
            if (user.getParticipatedCount() != null && user.getParticipatedCount() > 0) {
                user.setParticipatedCount(user.getParticipatedCount() - 1);
                userService.updateById(user);
            }
        }

        return Result.success("取消报名成功", null);
    }

    /**
     * 获取当前用户在活动中的角色
     */
    @Operation(summary = "获取活动角色",
               description = "获取当前用户在指定活动中的角色信息，包含报名备注和图片。活动创建者自动识别为组织者。\n\n" +
                           "返回数据说明：\n" +
                           "- role: 角色代码（1-组织者，2-参与者，null-未报名）\n" +
                           "- roleName: 角色名称（组织者/参与者/未报名）\n" +
                           "- status: 报名状态（0-待审核，1-已通过，3-已签到）\n" +
                           "- remark: 报名备注/留言\n" +
                           "- images: 报名图片列表（JSON数组格式）\n" +
                           "- canModify: 是否可以修改活动\n" +
                           "- canCheckIn: 是否可以签到")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功，返回ActivityRoleInfo对象"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/role/{activityId}")
    public Result<ActivityRoleInfo> getActivityRole(
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

        // 检查活动是否已删除或已下架
        boolean isActivityValid = activity.getDeleted() != null && activity.getDeleted() == 0
                && activity.getStatus() != null && activity.getStatus() != -2;

        // 获取用户角色
        var registration = permissionService.getUserRole(activityId, user.getId());

        ActivityRoleInfo roleInfo = new ActivityRoleInfo();
        roleInfo.setActivityId(activityId);
        roleInfo.setUserId(user.getId());

        if (registration == null || !isActivityValid) {
            // 检查用户是否是活动创建者（创建者也是组织者）
            // 如果活动已删除或已下架，也显示为未报名
            if (isActivityValid && activity.getOrganizerId() != null && activity.getOrganizerId().equals(user.getId())) {
                roleInfo.setRole(ActivityRole.ORGANIZER.getCode());
                roleInfo.setRoleName(ActivityRole.ORGANIZER.getDesc());
                roleInfo.setStatus(1); // 已通过
                roleInfo.setCanModify(true);
                roleInfo.setCanCheckIn(false);
            } else {
                roleInfo.setRole(null);
                roleInfo.setRoleName("未报名");
                roleInfo.setCanModify(false);
                roleInfo.setCanCheckIn(false);
            }
        } else {
            ActivityRole role = ActivityRole.getByCode(registration.getRole());
            roleInfo.setRole(registration.getRole());
            roleInfo.setRoleName(role.getDesc());
            roleInfo.setStatus(registration.getStatus());
            roleInfo.setRemark(registration.getRemark());
            roleInfo.setImages(registration.getImages());
            roleInfo.setCanModify(permissionService.canModifyActivity(activityId, user.getId()));
            roleInfo.setCanCheckIn(permissionService.canCheckIn(activityId, user.getId()));
        }

        return Result.success(roleInfo);
    }

    /**
     * 获取我参与的活动列表（不包括作为组织者/管理者创建的活动）
     */
    @Operation(summary = "我的活动", description = "获取当前用户作为参与者报名的所有活动详情列表（不包括作为组织者创建的活动）。\n\n" +
                "过滤规则：排除已删除和已下架的活动，按报名时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/my-activities")
    public Result<List<Activity>> getMyActivities(
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 获取用户的参与者报名记录（不包括组织者/管理者身份）
        // 避免用户自己创建的活动也显示在"我参与的活动"列表中
        List<ActivityRegistration> registrations = registrationService.getUserParticipantRegistrations(user.getId());
        
        // 提取活动ID并查询活动详情
        List<Long> activityIds = registrations.stream()
                .map(ActivityRegistration::getActivityId)
                .collect(java.util.stream.Collectors.toList());
        
        // 如果没有参与任何活动，返回空列表
        if (activityIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<Activity> activities = activityService.listByIds(activityIds);
        
        // 过滤掉已删除或已下架的活动
        activities = activities.stream()
                .filter(a -> a.getDeleted() != null && a.getDeleted() == 0)  // 未删除
                .filter(a -> a.getStatus() != null && a.getStatus() != -2)   // 未下架
                .collect(java.util.stream.Collectors.toList());
        
        // 按报名时间倒序排序（最新的在前面）
        activities.sort((a1, a2) -> {
            ActivityRegistration r1 = registrations.stream()
                    .filter(r -> r.getActivityId().equals(a1.getId()))
                    .findFirst().orElse(null);
            ActivityRegistration r2 = registrations.stream()
                    .filter(r -> r.getActivityId().equals(a2.getId()))
                    .findFirst().orElse(null);
            if (r1 == null || r2 == null) return 0;
            return r2.getCreateTime().compareTo(r1.getCreateTime());
        });
        
        return Result.success(activities);
    }

    /**
     * 活动角色信息
     */
    @Data
    @Schema(description = "活动角色信息")
    public static class ActivityRoleInfo {
        @Schema(description = "活动ID")
        private Long activityId;
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "角色代码：1-组织者，2-参与者")
        private Integer role;
        @Schema(description = "角色名称")
        private String roleName;
        @Schema(description = "报名状态：0-待审核，1-已通过，3-已签到")
        private Integer status;
        @Schema(description = "报名备注/留言")
        private String remark;
        @Schema(description = "报名图片列表，JSON数组格式")
        private String images;
        @Schema(description = "是否可以修改活动")
        private Boolean canModify;
        @Schema(description = "是否可以签到")
        private Boolean canCheckIn;
    }
}
