package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动评价实体类
 * 记录用户对已结束活动的评分和文字评价
 * 每个用户对同一活动仅能评价一次
 */
@Data
@TableName("activity_review")
@Schema(description = "活动评价信息")
public class ActivityReview {

    @TableId(type = IdType.INPUT)
    @Schema(description = "评价ID", example = "1")
    private Long id;

    @Schema(description = "活动ID", example = "1")
    private Long activityId;

    @Schema(description = "评价用户ID", example = "2")
    private Long userId;

    @Schema(description = "评分：1-5星", example = "5")
    private Integer rating;

    @Schema(description = "评价文字内容，最多500字", example = "活动内容很丰富，组织得很好！")
    private String content;

    @Schema(description = "组织者回复内容", example = "感谢参与，欢迎下次再来！")
    private String reply;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-04-28 10:00:00")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2026-04-28 12:00:00")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "逻辑删除标志：0-未删除，1-已删除", example = "0")
    private Integer deleted;

    // 非持久化字段，用于展示
    @TableField(exist = false)
    @Schema(description = "评价用户信息（非持久化）")
    private User user;
}
