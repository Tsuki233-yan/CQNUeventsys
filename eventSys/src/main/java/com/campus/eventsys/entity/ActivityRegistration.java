package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名实体类
 * 记录活动的参与者信息及其角色
 */
@Data
@TableName("activity_registration")
@Schema(description = "活动报名信息")
public class ActivityRegistration {

    @TableId(type = IdType.INPUT)
    @Schema(description = "报名记录ID", example = "1")
    private Long id;

    @Schema(description = "活动ID", example = "1")
    private Long activityId;

    @Schema(description = "用户ID", example = "2")
    private Long userId;

    @Schema(description = "参与角色：1-组织者，2-普通参与者", example = "2")
    private Integer role;

    @Schema(description = "报名状态：0-待审核，1-已通过，2-已拒绝，3-已签到，4-已取消", example = "1")
    private Integer status;

    @Schema(description = "报名备注/留言", example = "希望能有好的表现")
    private String remark;

    @Schema(description = "报名图片列表，JSON数组格式，如[\"/uploads/images/activity_1/xxx1.jpg\"]", example = "[\"/uploads/images/activity_1/xxx1.jpg\",\"/uploads/images/activity_1/xxx2.jpg\"]")
    private String images;

    @Schema(description = "审核人ID", example = "1")
    private Long reviewerId;

    @Schema(description = "审核时间", example = "2026-02-28 10:30:00")
    private LocalDateTime reviewTime;

    @Schema(description = "审核备注", example = "审核通过")
    private String reviewRemark;

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
