package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.ActivityResult;
import com.campus.eventsys.entity.Comment;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CommentService;
import com.campus.eventsys.service.NotificationService;
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
 * 评论控制器
 * 提供评论和回复功能
 * 支持对活动(activity)和活动成果展示贴(result)进行评论
 */
@Tag(name = "评论管理", description = "评论和回复相关接口，支持对活动和成果展示贴进行评论\n\n" +
        "目标类型说明：\n" +
        "- targetType=1: 活动评论\n" +
        "- targetType=2: 成果展示贴评论\n\n" +
        "评论结构：顶级评论 + 回复（子评论）\n" +
        "获取列表和数量接口为公开接口，发表/回复/删除需要登录")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    private final ActivityResultService activityResultService;
    private final ActivityPermissionService permissionService;
    private final NotificationService notificationService;
    private final ActivityService activityService;

    /**
     * 获取目标的评论列表（公开接口）
     */
    @Operation(summary = "获取评论列表", description = "获取指定目标的评论列表（包含回复）。公开接口，无需登录。\n\n" +
                "返回数据为树状结构：顶级评论包含 children 字段（回复列表）\n" +
                "每条评论包含用户信息（id、nickname、avatar）和回复对象信息（如有）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public Result<List<Comment>> getComments(
            @Parameter(description = "目标ID", required = true) @RequestParam Long targetId,
            @Parameter(description = "目标类型：1-活动，2-成果展示贴", required = true) @RequestParam Integer targetType) {
        List<Comment> comments = commentService.getAllCommentsWithReplies(targetId, targetType);

        // 加载用户信息
        for (Comment comment : comments) {
            loadUserInfo(comment);
            if (comment.getChildren() != null) {
                for (Comment reply : comment.getChildren()) {
                    loadUserInfo(reply);
                }
            }
        }

        return Result.success(comments);
    }

    /**
     * 获取评论数量（公开接口）
     */
    @Operation(summary = "获取评论数量", description = "获取指定目标的评论数量（包含回复）。公开接口，无需登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/count")
    public Result<Long> getCommentCount(
            @Parameter(description = "目标ID", required = true) @RequestParam Long targetId,
            @Parameter(description = "目标类型：1-活动，2-成果展示贴", required = true) @RequestParam Integer targetType) {
        long count = commentService.getCommentCount(targetId, targetType);
        return Result.success(count);
    }

    /**
     * 发表评论（顶级评论）
     */
    @Operation(summary = "发表评论", description = "对指定目标发表顶级评论。\n\n" +
                "参数说明：\n" +
                "- targetId: 目标ID（活动ID或成果展示贴ID）\n" +
                "- targetType: 目标类型（1-活动，2-成果展示贴）\n" +
                "- content: 评论内容（必填，不能为空）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发表成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/post")
    public Result<Void> postComment(
            @Parameter(description = "目标ID", required = true) @RequestParam Long targetId,
            @Parameter(description = "目标类型：1-活动，2-成果展示贴", required = true) @RequestParam Integer targetType,
            @Parameter(description = "评论内容", required = true) @RequestParam String content,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查内容是否为空
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        // 创建评论
        Comment comment = new Comment();
        comment.setTargetId(targetId);
        comment.setTargetType(targetType);
        comment.setUserId(user.getId());
        comment.setContent(content.trim());
        comment.setParentId(0L);
        comment.setReplyToUserId(0L);

        boolean success = commentService.addComment(comment);
        if (!success) {
            return Result.error("发表评论失败");
        }

        return Result.success("发表成功", null);
    }

    /**
     * 回复评论
     */
    @Operation(summary = "回复评论", description = "回复指定评论。\n\n" +
                "参数说明：\n" +
                "- parentId: 父评论ID（路径参数）\n" +
                "- content: 回复内容（必填，不能为空）\n" +
                "- replyToUserId: 回复给哪个用户（可选，默认回复给父评论作者）\n\n" +
                "回复成功后会自动发送评论回复通知给被回复用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "回复成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "父评论不存在"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/reply/{parentId}")
    public Result<Void> replyComment(
            @Parameter(description = "父评论ID", required = true) @PathVariable Long parentId,
            @Parameter(description = "回复内容", required = true) @RequestParam String content,
            @Parameter(description = "回复给哪个用户（可选，默认回复给父评论作者）") @RequestParam(required = false) Long replyToUserId,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查父评论是否存在
        Comment parentComment = commentService.getById(parentId);
        if (parentComment == null) {
            return Result.error("父评论不存在");
        }

        // 检查内容是否为空
        if (content == null || content.trim().isEmpty()) {
            return Result.error("回复内容不能为空");
        }

        // 如果没有指定回复用户，默认回复给父评论作者
        if (replyToUserId == null) {
            replyToUserId = parentComment.getUserId();
        }

        // 创建回复
        Comment reply = new Comment();
        reply.setTargetId(parentComment.getTargetId());
        reply.setTargetType(parentComment.getTargetType());
        reply.setUserId(user.getId());
        reply.setContent(content.trim());
        reply.setParentId(parentId);
        reply.setReplyToUserId(replyToUserId);

        boolean success = commentService.addComment(reply);
        if (!success) {
            return Result.error("回复失败");
        }

        // 发送评论回复通知（不通知自己）
        if (!replyToUserId.equals(user.getId())) {
            String activityTitle = getTargetTitle(parentComment.getTargetId(), parentComment.getTargetType());
            notificationService.sendCommentReply(replyToUserId, parentComment.getTargetId(), 
                    activityTitle, user.getNickname(), content.trim());
        }

        return Result.success("回复成功", null);
    }

    /**
     * 删除评论
     * 评论作者、目标所有者（组织者/创建者）或管理员可以删除
     */
    @Operation(summary = "删除评论", description = "删除评论及其所有回复。\n\n" +
                "删除权限：\n" +
                "1. 评论作者可以删除自己的评论\n" +
                "2. 目标所有者（活动组织者/成果展示贴创建者）可以删除目标下的评论\n" +
                "3. 管理员可以删除任何评论")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "评论不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteComment(
            @Parameter(description = "评论ID", required = true) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        // 获取当前用户
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查评论是否存在
        Comment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }

        // 检查权限
        boolean hasPermission = checkDeletePermission(comment, user);
        if (!hasPermission) {
            return Result.error("无权限：只有评论作者、目标所有者或管理员可以删除评论");
        }

        boolean success = commentService.deleteComment(id);
        if (!success) {
            return Result.error("删除失败");
        }

        return Result.success("删除成功", null);
    }

    /**
     * 检查用户是否有权限删除评论
     */
    private boolean checkDeletePermission(Comment comment, User user) {
        // 评论作者可以删除
        if (comment.getUserId().equals(user.getId())) {
            return true;
        }

        // 管理员可以删除
        if (user.getRole() != null && user.getRole() == 1) {
            return true;
        }

        // 检查是否是目标的所有者
        if (comment.getTargetType() == 1) {
            // 活动评论：检查是否是组织者
            return permissionService.isOrganizer(comment.getTargetId(), user.getId());
        } else if (comment.getTargetType() == 2) {
            // 成果展示贴评论：检查是否是创建者
            ActivityResult result = activityResultService.getById(comment.getTargetId());
            if (result != null && result.getCreatorId().equals(user.getId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取目标标题
     */
    private String getTargetTitle(Long targetId, Integer targetType) {
        if (targetType == 1) {
            // 活动
            var activity = activityService.getById(targetId);
            return activity != null ? activity.getTitle() : "活动";
        } else if (targetType == 2) {
            // 成果展示贴
            var result = activityResultService.getById(targetId);
            return result != null ? result.getTitle() : "成果展示";
        }
        return "活动";
    }

    /**
     * 加载评论的用户信息
     */
    private void loadUserInfo(Comment comment) {
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            // 只保留必要信息
            User simpleUser = new User();
            simpleUser.setId(user.getId());
            simpleUser.setNickname(user.getNickname());
            simpleUser.setAvatar(user.getAvatar());
            comment.setUser(simpleUser);
        }

        // 加载回复对象信息
        if (comment.getReplyToUserId() != null && comment.getReplyToUserId() > 0) {
            User replyToUser = userService.getById(comment.getReplyToUserId());
            if (replyToUser != null) {
                User simpleReplyToUser = new User();
                simpleReplyToUser.setId(replyToUser.getId());
                simpleReplyToUser.setNickname(replyToUser.getNickname());
                comment.setReplyToUser(simpleReplyToUser);
            }
        }
    }
}
