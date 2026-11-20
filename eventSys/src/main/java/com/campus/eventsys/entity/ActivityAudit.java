package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动审核记录实体类
 */
@Data
@TableName("activity_audit")
public class ActivityAudit {

    // SQLite 不支持 getGeneratedKeys，使用 INPUT 模式
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /**
     * 审核类型
     * 1-活动发布审核
     * 2-活动修改审核
     * 3-活动取消审核
     */
    private Integer auditType;

    /**
     * 审核状态
     * 0-待审核
     * 1-已通过
     * 2-已拒绝
     */
    private Integer status;

    /** 提交人ID */
    private Long submitterId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核人ID */
    private Long auditorId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditOpinion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
