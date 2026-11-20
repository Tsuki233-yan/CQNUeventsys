package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动实体类
 */
@Data
@TableName("activity")
@Schema(description = "活动信息")
public class Activity {

    // SQLite 不支持 getGeneratedKeys，使用 INPUT 模式
    @TableId(type = IdType.INPUT)
    @Schema(description = "活动ID", example = "1")
    private Long id;

    @Schema(description = "活动标题", example = "校园歌手大赛")
    private String title;

    @Schema(description = "活动描述", example = "展示你的歌喉，赢取丰厚奖品！")
    private String description;

    @Schema(description = "封面图片URL", example = "/images/activity1.jpg")
    private String coverImage;

    @Schema(description = "组织者ID", example = "2")
    private Long organizerId;

    @Schema(description = "活动地点", example = "学校大礼堂")
    private String location;

    @Schema(description = "活动开始时间", example = "2026-03-07 14:00:00")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间", example = "2026-03-07 18:00:00")
    private LocalDateTime endTime;

    @Schema(description = "报名开始时间", example = "2026-02-27 00:00:00")
    private LocalDateTime registrationStartTime;

    @Schema(description = "报名结束时间", example = "2026-03-05 23:59:59")
    private LocalDateTime registrationEndTime;

    @Schema(description = "最大参与人数，0表示不限制", example = "50")
    private Integer maxParticipants;

    @Schema(description = "当前报名人数", example = "12")
    private Integer currentParticipants;

    @Schema(description = "活动标签，逗号分隔", example = "音乐,文艺,比赛,歌手")
    private String tags;

    @Schema(description = "活动状态：-2-已下架, -1-已拒绝, 0-草稿, 1-待审核, 2-报名中, 3-即将开始, 4-进行中, 5-已结束", example = "2")
    private Integer status;

    @Schema(description = "活动分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "是否需要审核报名", example = "true")
    private Boolean needReview;

    @Schema(description = "浏览次数", example = "156")
    private Integer viewCount;

    @Schema(description = "收藏次数", example = "23")
    private Integer favoriteCount;

    @Schema(description = "评论数量", example = "8")
    private Integer commentCount;

    @Schema(description = "平均评分，保留1位小数", example = "4.5")
    private Double avgRating;

    @Schema(description = "评价数量", example = "20")
    private Integer reviewCount;

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
}
