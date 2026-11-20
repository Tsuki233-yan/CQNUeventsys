package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Comment;
import com.campus.eventsys.mapper.CommentMapper;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    private final ActivityResultService activityResultService;
    private final ActivityMapper activityMapper;

    @Override
    public List<Comment> getTopComments(Long targetId, Integer targetType) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getParentId, 0L)
                        .eq(Comment::getDeleted, 0)
                        .orderByDesc(Comment::getCreateTime)
        );
    }

    @Override
    public List<Comment> getReplies(Long parentId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, parentId)
                        .eq(Comment::getDeleted, 0)
                        .orderByAsc(Comment::getCreateTime)
        );
    }

    @Override
    public List<Comment> getAllCommentsWithReplies(Long targetId, Integer targetType) {
        // 获取所有评论
        List<Comment> allComments = baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getDeleted, 0)
                        .orderByDesc(Comment::getCreateTime)
        );

        // 分离顶级评论和回复
        Map<Long, List<Comment>> replyMap = allComments.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<Comment> topComments = allComments.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        // 将回复设置到对应的顶级评论
        for (Comment topComment : topComments) {
            List<Comment> replies = replyMap.getOrDefault(topComment.getId(), new ArrayList<>());
            topComment.setChildren(replies);
        }

        return topComments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addComment(Comment comment) {
        try {
            // 设置默认值
            if (comment.getParentId() == null) {
                comment.setParentId(0L);
            }
            if (comment.getReplyToUserId() == null) {
                comment.setReplyToUserId(0L);
            }

            baseMapper.insert(comment);

            // 增加评论计数
            if (comment.getTargetType() != null) {
                if (comment.getTargetType() == 2) {
                    // 评论成果展示贴
                    activityResultService.incrementCommentCount(comment.getTargetId());
                } else if (comment.getTargetType() == 1) {
                    // 评论活动
                    Activity activity = activityMapper.selectById(comment.getTargetId());
                    if (activity != null) {
                        activity.setCommentCount(activity.getCommentCount() + 1);
                        activityMapper.updateById(activity);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(Long commentId) {
        try {
            Comment comment = getById(commentId);
            if (comment == null) {
                return false;
            }

            // 逻辑删除主评论
            removeById(commentId);

            // 逻辑删除所有回复
            List<Comment> replies = getReplies(commentId);
            for (Comment reply : replies) {
                removeById(reply.getId());
            }

            // 减少评论计数
            if (comment.getTargetType() != null) {
                int deletedCount = 1 + replies.size();
                if (comment.getTargetType() == 2) {
                    // 评论成果展示贴
                    for (int i = 0; i < deletedCount; i++) {
                        activityResultService.decrementCommentCount(comment.getTargetId());
                    }
                } else if (comment.getTargetType() == 1) {
                    // 评论活动
                    Activity activity = activityMapper.selectById(comment.getTargetId());
                    if (activity != null && activity.getCommentCount() >= deletedCount) {
                        activity.setCommentCount(activity.getCommentCount() - deletedCount);
                        activityMapper.updateById(activity);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return false;
        }
    }

    @Override
    public long getCommentCount(Long targetId, Integer targetType) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getDeleted, 0)
        );
    }
}
