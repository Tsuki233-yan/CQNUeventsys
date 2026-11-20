package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("user")
@Schema(description = "用户信息")
public class User {

    @TableId(type = IdType.INPUT)
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名/学号", example = "2021001")
    private String username;

    @Schema(description = "密码（返回时会被隐藏）", example = "***")
    private String password;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像URL", example = "/avatars/user1.jpg")
    private String avatar;

    @Schema(description = "邮箱", example = "zhangsan@campus.edu.cn")
    private String email;

    @Schema(description = "手机号", example = "13800138001")
    private String phone;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "用户身份：0-学生，1-教师（NULL/缺失视为未绑定身份的老账号，需在个人中心补填）", example = "0")
    private Integer userType;

    @Schema(description = "学号：学生为10位数字（前4位为入学年份2020~2026），教师为NULL", example = "2023011234")
    private String studentId;

    @Schema(description = "工号：教师为6位数字，学生为NULL", example = "200101")
    private String teacherId;

    @Schema(description = "学院", example = "计算机学院")
    private String college;

    @Schema(description = "专业", example = "软件工程")
    private String major;

    @Schema(description = "年级", example = "2021")
    private String grade;

    @Schema(description = "性别：0-女，1-男", example = "1")
    private Integer gender;

    @Schema(description = "个人简介", example = "热爱编程和音乐")
    private String bio;

    @Schema(description = "角色：0-普通用户，1-管理员", example = "0")
    private Integer role;

    @Schema(description = "状态：0-禁用，1-正常", example = "1")
    private Integer status;

    @Schema(description = "举办活动数", example = "2")
    private Integer organizedCount;

    @Schema(description = "参与活动数", example = "5")
    private Integer participatedCount;

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
