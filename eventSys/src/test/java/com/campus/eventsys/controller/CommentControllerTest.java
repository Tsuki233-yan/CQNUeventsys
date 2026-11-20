package com.campus.eventsys.controller;

import com.campus.eventsys.common.Result;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityResult;
import com.campus.eventsys.entity.Comment;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityResultService;
import com.campus.eventsys.service.ActivityService;
import com.campus.eventsys.service.CommentService;
import com.campus.eventsys.service.NotificationService;
import com.campus.eventsys.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 评论控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityResultService activityResultService;

    @Mock
    private ActivityPermissionService permissionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityService activityService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentController commentController;

    private User testUser;
    private User replyToUser;
    private Comment testComment;
    private Comment replyComment;
    private Activity testActivity;
    private ActivityResult testResult;
    private static final Long TARGET_ID = 1L;
    private static final Long COMMENT_ID = 1L;
    private static final Long REPLY_ID = 2L;
    private static final Long USER_ID = 1L;
    private static final Long REPLY_USER_ID = 2L;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setNickname("测试用户");
        testUser.setRole(0);

        replyToUser = new User();
        replyToUser.setId(REPLY_USER_ID);
        replyToUser.setNickname("被回复用户");

        testComment = new Comment();
        testComment.setId(COMMENT_ID);
        testComment.setTargetId(TARGET_ID);
        testComment.setTargetType(1);
        testComment.setUserId(REPLY_USER_ID);
        testComment.setContent("测试评论");
        testComment.setParentId(0L);

        replyComment = new Comment();
        replyComment.setId(REPLY_ID);
        replyComment.setTargetId(TARGET_ID);
        replyComment.setTargetType(1);
        replyComment.setUserId(USER_ID);
        replyComment.setContent("回复内容");
        replyComment.setParentId(COMMENT_ID);
        replyComment.setReplyToUserId(REPLY_USER_ID);

        testActivity = new Activity();
        testActivity.setId(TARGET_ID);
        testActivity.setTitle("测试活动");

        testResult = new ActivityResult();
        testResult.setId(TARGET_ID);
        testResult.setTitle("测试成果");
        testResult.setCreatorId(REPLY_USER_ID);
    }

    @Test
    void getComments_Success() {
        // 准备
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setParentId(COMMENT_ID);
        reply.setUserId(REPLY_USER_ID);
        testComment.setChildren(Collections.singletonList(reply));

        List<Comment> comments = Collections.singletonList(testComment);
        when(commentService.getAllCommentsWithReplies(TARGET_ID, 1)).thenReturn(comments);
        when(userService.getById(REPLY_USER_ID)).thenReturn(replyToUser);

        // 执行
        Result<List<Comment>> result = commentController.getComments(TARGET_ID, 1);

        // 验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getCommentCount_Success() {
        // 准备
        when(commentService.getCommentCount(TARGET_ID, 1)).thenReturn(10L);

        // 执行
        Result<Long> result = commentController.getCommentCount(TARGET_ID, 1);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals(10L, result.getData());
    }

    @Test
    void postComment_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.addComment(any(Comment.class))).thenReturn(true);

        // 执行
        Result<Void> result = commentController.postComment(TARGET_ID, 1, "评论内容", authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("发表成功", result.getMessage());
    }

    @Test
    void postComment_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = commentController.postComment(TARGET_ID, 1, "评论内容", authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void postComment_EmptyContent() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<Void> result = commentController.postComment(TARGET_ID, 1, "   ", authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("评论内容不能为空", result.getMessage());
    }

    @Test
    void postComment_NullContent() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);

        // 执行
        Result<Void> result = commentController.postComment(TARGET_ID, 1, null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("评论内容不能为空", result.getMessage());
    }

    @Test
    void replyComment_Success() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(testComment);
        when(commentService.addComment(any(Comment.class))).thenReturn(true);
        when(activityService.getById(TARGET_ID)).thenReturn(testActivity);
        when(notificationService.sendCommentReply(any(), any(), any(), any(), any())).thenReturn(null);

        // 执行
        Result<Void> result = commentController.replyComment(COMMENT_ID, "回复内容", null, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("回复成功", result.getMessage());
    }

    @Test
    void replyComment_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = commentController.replyComment(COMMENT_ID, "回复内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void replyComment_ParentNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(null);

        // 执行
        Result<Void> result = commentController.replyComment(COMMENT_ID, "回复内容", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("父评论不存在", result.getMessage());
    }

    @Test
    void replyComment_EmptyContent() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(testComment);

        // 执行
        Result<Void> result = commentController.replyComment(COMMENT_ID, "   ", null, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("回复内容不能为空", result.getMessage());
    }

    @Test
    void replyComment_NotNotifySelf() {
        // 准备 - 回复给自己，不发送通知
        testComment.setUserId(USER_ID);
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(testComment);
        when(commentService.addComment(any(Comment.class))).thenReturn(true);

        // 执行
        Result<Void> result = commentController.replyComment(COMMENT_ID, "回复内容", USER_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        verify(notificationService, never()).sendCommentReply(any(), any(), any(), any(), any());
    }

    @Test
    void deleteComment_Success() {
        // 准备 - 评论作者是当前用户
        Comment myComment = new Comment();
        myComment.setId(COMMENT_ID);
        myComment.setTargetId(TARGET_ID);
        myComment.setTargetType(1);
        myComment.setUserId(USER_ID); // 当前用户是评论作者
        
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(myComment);
        when(commentService.deleteComment(COMMENT_ID)).thenReturn(true);

        // 执行
        Result<Void> result = commentController.deleteComment(COMMENT_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
        assertEquals("删除成功", result.getMessage());
    }

    @Test
    void deleteComment_UserNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(null);

        // 执行
        Result<Void> result = commentController.deleteComment(COMMENT_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void deleteComment_CommentNotFound() {
        // 准备
        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(null);

        // 执行
        Result<Void> result = commentController.deleteComment(COMMENT_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("评论不存在", result.getMessage());
    }

    @Test
    void deleteComment_NoPermission() {
        // 准备 - 其他用户的评论，无权限删除
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setRole(0);

        Comment otherComment = new Comment();
        otherComment.setId(COMMENT_ID);
        otherComment.setUserId(3L);
        otherComment.setTargetId(TARGET_ID);
        otherComment.setTargetType(1);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(testUser);
        when(commentService.getById(COMMENT_ID)).thenReturn(otherComment);
        when(permissionService.isOrganizer(TARGET_ID, USER_ID)).thenReturn(false);

        // 执行
        Result<Void> result = commentController.deleteComment(COMMENT_ID, authentication);

        // 验证
        assertEquals(400, result.getCode());
        assertEquals("无权限：只有评论作者、目标所有者或管理员可以删除评论", result.getMessage());
    }

    @Test
    void deleteComment_AdminCanDelete() {
        // 准备 - 管理员可以删除任何评论
        User admin = new User();
        admin.setId(USER_ID);
        admin.setRole(1);

        when(authentication.getName()).thenReturn(USERNAME);
        when(userService.getByUsername(USERNAME)).thenReturn(admin);
        when(commentService.getById(COMMENT_ID)).thenReturn(testComment);
        when(commentService.deleteComment(COMMENT_ID)).thenReturn(true);

        // 执行
        Result<Void> result = commentController.deleteComment(COMMENT_ID, authentication);

        // 验证
        assertEquals(200, result.getCode());
    }
}
