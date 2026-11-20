package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动收藏/关注实体类
 */
@Data
@TableName("activity_favorite")
public class ActivityFavorite {

    // SQLite 不支持 getGeneratedKeys，使用 INPUT 模式
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 用户ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
