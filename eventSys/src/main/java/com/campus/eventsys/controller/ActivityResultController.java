package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.ActivityResult;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 活动成果展示贴控制器
 * 提供成果展示贴的创建、查询、更新、删除等功能
 * 支持Markdown内容和图片上传
 */
@Tag(name = "活动成果展示", description = "活动成果展示贴相关接口，支持Markdown编辑和图片\n\n" +
        "权限说明：\n" +
        "- 创建：活动组织者或管理员\n" +
        "- 更新/删除：创建者或管理员\n" +
        "- 查询/检查：公开接口，无需登录")
@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
public class ActivityResultController {

    private final ActivityResultService resultService;
    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityPermissionService permissionService;
    private final NotificationService notificationService;
    private final ActivityRegistrationService registrationService;

    /**
     * 获取成果展示贴详情
     * 公开接口，无需登录
     */
    @Operation(summary = "获取成果展示贴", description = "根据活动ID获取对应的成果展示贴详情，浏览次数+1。公开接口，无需登录。\n\n" +
                "返回数据包含：成果展示贴信息 + 创建者信息(creator) + 活动信息(activity)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "成果展示贴不存在")
    })
    @GetMapping("/activity/{activityId}")
    public Result<ActivityResult> getByActivityId(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        ActivityResult result = resultService.getByActivityId(activityId);
        if (result == null) {
            return Result.error("该活动暂无成果展示贴");
        }

        // 浏览次数+1
        resultService.incrementViewCount(result.getId());

        // 加载创建者信息
        User creator = userService.getById(result.getCreatorId());
        result.setCreator(creator);

        // 加载活动信息
        Activity activity = activityService.getById(activityId);
        result.setActivity(activity);

        return Result.success(result);
    }

    /**
     * 检查活动是否有成果展示贴
     * 公开接口，无需登录
     */
    @Operation(summary = "检查成果展示贴", description = "检查指定活动是否已有成果展示贴。公开接口，无需登录。返回true/false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/exists/{activityId}")
    public Result<Boolean> existsByActivityId(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId) {
        boolean exists = resultService.existsByActivityId(activityId);
        return Result.success(exists);
    }

    /**
     * 创建成果展示贴
     * 只有活动组织者或管理员可以创建
     */
    @Operation(summary = "创建成果展示贴", description = "为活动创建成果展示贴，只有组织者或管理员可以创建。\n\n" +
                "参数说明：\n" +
                "- activityId: 活动ID（路径参数，必填）\n" +
                "- title: 帖子标题（必填）\n" +
                "- content: 帖子内容，Markdown格式（可选）\n" +
                "- images: 图片URL列表，JSON数组格式（可选）\n\n" +
                "每个活动只能有一个成果展示贴。创建成功后会自动通知所有已通过审核的参与者")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限（不是组织者或管理员）"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "400", description = "已存在成果展示贴")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/create/{activityId}")
    public Result<Void> create(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "帖子标题", required = true) @RequestParam String title,
            @Parameter(description = "帖子内容（Markdown格式）") @RequestParam(required = false) String content,
            @Parameter(description = "图片URL列表，JSON数组格式") @RequestParam(required = false) String images,
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

        // 检查权限（必须是组织者或管理员）
        boolean isOrganizer = permissionService.isOrganizer(activityId, user.getId());
        boolean isAdmin = user.getRole() != null && user.getRole() == 1;
        if (!isOrganizer && !isAdmin) {
            return Result.error("无权限：只有活动组织者或管理员可以创建成果展示贴");
        }

        // 检查是否已有未删除的成果展示贴
        if (resultService.existsByActivityId(activityId)) {
            return Result.error("该活动已有成果展示贴，请使用更新接口");
        }

        // 创建成果展示贴（支持删除后重新创建的场景）
        ActivityResult result = new ActivityResult();
        result.setActivityId(activityId);
        result.setTitle(title);
        result.setContent(content);
        result.setImages(images);
        result.setCreatorId(user.getId());
        result.setViewCount(0);
        result.setCommentCount(0);
        result.setStatus(1); // 1-已发布

        boolean success = resultService.createOrRestore(result);
        if (!success) {
            return Result.error("创建失败");
        }
        
        // 发送成果展示贴发布通知给所有已通过审核的参与者
        sendResultPublishNotification(activity, user.getNickname());
        
        return Result.success("创建成功", null);
    }

    /**
     * 更新成果展示贴
     * 只有创建者或管理员可以更新
     */
    @Operation(summary = "更新成果展示贴", description = "更新成果展示贴内容，只有创建者或管理员可以更新。所有字段均为可选，仅更新传入的字段")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "成果展示贴不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @Parameter(description = "成果展示贴ID", required = true) @PathVariable Long id,
            @Parameter(description = "帖子标题") @RequestParam(required = false) String title,
            @Parameter(description = "帖子内容（Markdown格式）") @RequestParam(required = false) String content,
            @Parameter(description = "图片URL列表，JSON数组格式") @RequestParam(required = false) String images,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查成果展示贴是否存在
        ActivityResult result = resultService.getById(id);
        if (result == null) {
            return Result.error("成果展示贴不存在");
        }

        // 检查权限（必须是创建者或管理员）
        boolean isCreator = result.getCreatorId().equals(user.getId());
        boolean isAdmin = user.getRole() != null && user.getRole() == 1;
        if (!isCreator && !isAdmin) {
            return Result.error("无权限：只有创建者或管理员可以更新成果展示贴");
        }

        // 更新内容
        if (title != null) {
            result.setTitle(title);
        }
        if (content != null) {
            result.setContent(content);
        }
        if (images != null) {
            result.setImages(images);
        }

        resultService.updateById(result);
        return Result.success("更新成功", null);
    }

    /**
     * 删除成果展示贴
     * 只有创建者或管理员可以删除
     */
    @Operation(summary = "删除成果展示贴", description = "删除成果展示贴（逻辑删除），只有创建者或管理员可以删除")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "成果展示贴不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @Parameter(description = "成果展示贴ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查成果展示贴是否存在
        ActivityResult result = resultService.getById(id);
        if (result == null) {
            return Result.error("成果展示贴不存在");
        }

        // 检查权限（必须是创建者或管理员）
        boolean isCreator = result.getCreatorId().equals(user.getId());
        boolean isAdmin = user.getRole() != null && user.getRole() == 1;
        if (!isCreator && !isAdmin) {
            return Result.error("无权限：只有创建者或管理员可以删除成果展示贴");
        }

        resultService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 上传图片（支持多图上传）
     * 用于成果展示贴的图片上传，图片按活动ID分目录存储
     * 
     * 【图片存储规范】
     * - 所有活动相关图片（含报名帖与展示贴）统一存储至活动展示贴指定的图片路径
     * - 共用同一套存储路径结构与后端接口逻辑
     * - 存储路径：uploads/images/activity_{活动ID}/
     * - 文件命名：UUID随机生成，确保唯一性
     * - 前端按文档规范直接消费，通过 /uploads/images/activity_{活动ID}/文件名 访问
     */
    @Operation(summary = "上传图片（成果展示贴）",
                description = "上传成果展示贴的图片，支持多张图片同时上传。\n\n" +
                           "图片存储规范：\n" +
                           "- 存储位置：uploads/images/activity_{活动ID}/\n" +
                           "- 文件命名：UUID随机生成\n" +
                           "- 访问URL：/uploads/images/activity_{活动ID}/文件名\n\n" +
                           "限制条件：单次最多9张，单张不超过10MB，仅支持图片格式\n\n" +
                           "返回数据：图片URL字符串数组")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功，data字段返回图片URL字符串数组，格式：[\"/uploads/images/activity_1/xxx.jpg\"]"),
            @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
            @ApiResponse(responseCode = "400", description = "上传失败：文件为空/超过9张/非图片格式/超过10MB/其他业务错误"),
            @ApiResponse(responseCode = "404", description = "活动不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/upload-images/{activityId}", consumes = "multipart/form-data")
    public Result<List<String>> uploadImages(
            @Parameter(description = "活动ID", required = true) @PathVariable Long activityId,
            @Parameter(description = "图片文件列表，支持jpg/png/gif等格式，form-data格式上传", required = true)
            @RequestParam("files") List<MultipartFile> files) {

        // 检查活动是否存在
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        if (files == null || files.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        // 限制单次上传数量
        if (files.size() > 9) {
            return Result.error("单次最多上传9张图片");
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件: " + file.getOriginalFilename());
            }

            // 检查文件大小（限制10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error("图片大小不能超过10MB: " + file.getOriginalFilename());
            }

            try {
                // 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

                // 保存文件到上传目录（按活动ID分目录）
                String imageUrl = resultService.saveImage(file, newFilename, activityId);
                imageUrls.add(imageUrl);

            } catch (Exception e) {
                return Result.error("上传失败: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }

        return Result.success("上传成功", imageUrls);
    }

    /**
     * 发送成果展示贴发布通知
     * @param activity 活动
     * @param creatorName 创建者名称
     */
    private void sendResultPublishNotification(Activity activity, String creatorName) {
        try {
            // 获取活动的所有已通过审核的参与者
            List<ActivityRegistration> registrations = registrationService.getActivityRegistrations(activity.getId());
            
            for (ActivityRegistration registration : registrations) {
                // 通知所有已通过审核的参与者（不包括创建者自己）
                if (registration.getStatus() == 1) {
                    notificationService.sendResultPublish(
                            registration.getUserId(),
                            activity.getId(),
                            activity.getTitle(),
                            creatorName
                    );
                }
            }
        } catch (Exception e) {
            // 通知发送失败不影响主流程
            // log.error("发送成果展示贴发布通知失败: activityId={}", activity.getId(), e);
        }
    }
}
