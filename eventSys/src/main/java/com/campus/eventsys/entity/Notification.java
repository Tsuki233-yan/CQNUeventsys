package com.campus.eventsys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知/消息实体类
 */
@Data
@TableName("notification")
public class Notification {

    // SQLite 不支持 getGeneratedKeys，使用 INPUT 模式
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 接收用户ID 0-全体用户（公告为单条广播） */
    private Long userId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /**
     * 类型
     * 1-系统公告（管理员发送）
     * 2-活动提醒
     * 3-审核结果
     * 4-活动状态变更
     * 5-评论回复
     */
    private Integer type;

    /** 关联活动ID */
    private Long activityId;

    /** 是否已读 0-未读 1-已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 公告发布范围：ALL / USER / ADMIN（仅 type=1 有效；个人通知可为空）
     */
    private String publishScope;

    /** 是否置顶 0/1（仅 type=1 参与排序） */
    private Integer isPinned;

    /** 计划/实际发布时间（个人通知可为创建时间） */
    private LocalDateTime publishTime;

    /**
     * 公告状态：0-草稿 1-已发布 2-已撤回（仅 type=1；个人通知恒为 1）
     */
    private Integer status;

    /** 公告创建人（管理员 user id） */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 公告展示子类：SYSTEM / ACTIVITY / URGENT（仅 type=1）
     */
    private String announceCategory;
}
