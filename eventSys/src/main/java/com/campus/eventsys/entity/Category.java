package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动分类/标签实体类
 */
@Data
@TableName("category")
@Schema(description = "活动分类信息")
public class Category {

    @TableId(type = IdType.INPUT)
    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "文艺活动")
    private String name;

    @Schema(description = "分类描述", example = "音乐、舞蹈、戏剧等文艺类活动")
    private String description;

    @Schema(description = "分类图标", example = "icon-art")
    private String icon;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "活动数量", example = "10")
    private Integer activityCount;

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
