package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论实体
 * 支持对活动(activity)和活动成果展示贴(result)进行评论
 */
@Data
@TableName("comment")
@Schema(description = "评论信息")
public class Comment {

    @TableId(type = IdType.INPUT)
    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "目标ID（活动ID或成果展示贴ID）", example = "1")
    private Long targetId;

    @Schema(description = "目标类型：1-活动，2-成果展示贴", example = "1")
    private Integer targetType;

    @Schema(description = "用户ID", example = "2")
    private Long userId;

    @Schema(description = "评论内容", example = "非常期待这次活动！")
    private String content;

    @Schema(description = "父评论ID，0表示顶级评论", example = "0")
    private Long parentId;

    @Schema(description = "回复给哪个用户的ID", example = "0")
    private Long replyToUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-02-28 10:00:00")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2026-02-28 12:00:00")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "逻辑删除标志：0-未删除，1-已删除", example = "0")
    private Integer deleted;

    // 非持久化字段，用于展示
    @TableField(exist = false)
    @Schema(description = "评论用户信息（非持久化）")
    private User user;

    @TableField(exist = false)
    @Schema(description = "回复目标用户信息（非持久化）")
    private User replyToUser;

    @TableField(exist = false)
    @Schema(description = "子评论（回复）列表（非持久化）")
    private List<Comment> children;
}
