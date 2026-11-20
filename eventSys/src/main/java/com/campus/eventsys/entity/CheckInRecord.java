package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动签到记录实体类
 */
@Data
@TableName("check_in_record")
@Schema(description = "活动签到记录信息")
public class CheckInRecord {

    @TableId(type = IdType.INPUT)
    @Schema(description = "签到记录ID", example = "1")
    private Long id;

    @Schema(description = "活动ID", example = "1")
    private Long activityId;

    @Schema(description = "用户ID", example = "2")
    private Long userId;

    @Schema(description = "报名信息ID", example = "1")
    private Long registrationId;

    @Schema(description = "签到时间", example = "2026-03-07 14:05:00")
    private LocalDateTime checkInTime;

    @Schema(description = "签到方式：1-扫码，2-手动，3-自动", example = "2")
    private Integer checkInType;

    @Schema(description = "签到地点", example = "学校大礼堂入口")
    private String location;

    @Schema(description = "备注", example = "用户手动签到")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-03-07 14:05:00")
    private LocalDateTime createTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "逻辑删除标志：0-未删除，1-已删除", example = "0")
    private Integer deleted;

    @TableField(exist = false)
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @TableField(exist = false)
    @Schema(description = "昵称", example = "张三")
    private String nickname;
}
