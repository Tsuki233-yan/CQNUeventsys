package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动成果展示贴实体
 * 活动结束后由组织者或管理者创建
 * 支持Markdown内容和图片
 */
@Data
@TableName("activity_result")
@Schema(description = "活动成果展示贴信息")
public class ActivityResult {

    @TableId(type = IdType.INPUT)
    @Schema(description = "成果展示贴ID", example = "1")
    private Long id;

    @Schema(description = "关联的活动ID", example = "1")
    private Long activityId;

    @Schema(description = "帖子标题", example = "校园歌手大赛精彩回顾")
    private String title;

    @Schema(description = "帖子内容（Markdown格式）", example = "# 活动回顾\\n\\n本次活动圆满成功...")
    private String content;

    @Schema(description = "图片列表，JSON数组格式", example = "[\"/images/result1.jpg\", \"/images/result2.jpg\"]")
    private String images;

    @Schema(description = "创建者ID（组织者或管理者）", example = "2")
    private Long creatorId;

    @Schema(description = "浏览次数", example = "100")
    private Integer viewCount;

    @Schema(description = "评论数量", example = "10")
    private Integer commentCount;

    @Schema(description = "状态：-2-已下架, 0-草稿, 1-已发布", example = "1")
    private Integer status;

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
    @Schema(description = "创建者信息（非持久化）")
    private User creator;

    @TableField(exist = false)
    @Schema(description = "活动信息（非持久化）")
    private Activity activity;
}
