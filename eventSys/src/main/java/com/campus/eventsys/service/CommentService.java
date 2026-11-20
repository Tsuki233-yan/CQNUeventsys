package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.Comment;

import java.util.List;

/**
 * 评论服务接口
 * 支持对活动和成果展示贴进行评论
 */
public interface CommentService extends IService<Comment> {

    /**
     * 获取目标的评论列表（顶级评论）
     *
     * @param targetId   目标ID
     * @param targetType 目标类型：1-活动，2-成果展示贴
     * @return 评论列表
     */
    List<Comment> getTopComments(Long targetId, Integer targetType);

    /**
     * 获取评论的回复列表
     *
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<Comment> getReplies(Long parentId);

    /**
     * 获取目标的所有评论（包含回复）
     *
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 评论列表（树形结构）
     */
    List<Comment> getAllCommentsWithReplies(Long targetId, Integer targetType);

    /**
     * 添加评论
     *
     * @param comment 评论信息
     * @return 是否添加成功
     */
    boolean addComment(Comment comment);

    /**
     * 删除评论（逻辑删除）
     * 同时删除所有回复
     *
     * @param commentId 评论ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId);

    /**
     * 获取目标的评论数量
     *
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 评论数量
     */
    long getCommentCount(Long targetId, Integer targetType);
}
