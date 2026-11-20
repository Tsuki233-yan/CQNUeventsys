package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.common.BusinessException;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityReview;
import com.campus.eventsys.entity.CheckInRecord;
import com.campus.eventsys.entity.ReviewStatisticsVO;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.mapper.ActivityReviewMapper;
import com.campus.eventsys.service.ActivityPermissionService;
import com.campus.eventsys.service.ActivityReviewService;
import com.campus.eventsys.service.CheckInRecordService;
import com.campus.eventsys.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动评价服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityReviewServiceImpl extends ServiceImpl<ActivityReviewMapper, ActivityReview>
        implements ActivityReviewService {

    private final ActivityMapper activityMapper;
    private final ActivityPermissionService permissionService;
    private final CheckInRecordService checkInRecordService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityReview createReview(Long activityId, Long userId, Integer rating, String content) {
        // 1. 检查活动是否存在且未被删除
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getDeleted() != null && activity.getDeleted() == 1) {
            throw new BusinessException(404, "活动不存在");
        }

        // 2. 检查活动状态是否为已结束(5)
        if (activity.getStatus() == null || activity.getStatus() != 5) {
            throw new BusinessException(403, "活动尚未结束，无法评价");
        }

        // 3. 检查用户是否为活动参与者（组织者不可评价）
        boolean isParticipant = permissionService.isParticipant(activityId, userId);
        if (!isParticipant) {
            throw new BusinessException(403, "仅活动参与者可以评价");
        }

        // 4. 检查用户是否已签到
        boolean hasCheckedIn = checkInRecordService.hasCheckedIn(activityId, userId);
        if (!hasCheckedIn) {
            throw new BusinessException(403, "您未完成签到，无法评价");
        }

        // 5. 检查是否已评价
        ActivityReview existing = getOne(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getUserId, userId)
                        .eq(ActivityReview::getDeleted, 0)
        );
        if (existing != null) {
            throw new BusinessException(400, "您已评价过该活动");
        }

        // 6. 校验评分范围
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(400, "评分必须为1-5的整数");
        }

        // 7. 校验评价内容
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "评价内容不能为空");
        }
        if (content.trim().length() > 500) {
            throw new BusinessException(400, "评价内容不能超过500字");
        }

        // 8. 创建评价记录
        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content.trim());
        baseMapper.insert(review);

        // SQLite不支持getGeneratedKeys，通过唯一约束查回记录获取自增ID
        ActivityReview saved = baseMapper.selectOne(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getUserId, userId)
                        .eq(ActivityReview::getDeleted, 0)
        );

        // 9. 重新计算活动的平均评分和评价数量
        recalculateActivityRating(activityId);

        return saved;
    }

    @Override
    public Page<ActivityReview> getActivityReviews(Long activityId, int page, int size) {
        Page<ActivityReview> pageParam = new Page<>(page, size);
        Page<ActivityReview> result = baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getDeleted, 0)
                        .orderByDesc(ActivityReview::getCreateTime)
        );

        // 加载用户信息
        loadUserInfo(result.getRecords());
        return result;
    }

    @Override
    public Page<ActivityReview> getMyReviews(Long userId, int page, int size) {
        Page<ActivityReview> pageParam = new Page<>(page, size);
        Page<ActivityReview> result = baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getUserId, userId)
                        .eq(ActivityReview::getDeleted, 0)
                        .orderByDesc(ActivityReview::getCreateTime)
        );

        // 加载用户信息
        loadUserInfo(result.getRecords());
        return result;
    }

    @Override
    public Page<ActivityReview> getOrganizerReviews(Long organizerId, Long activityId, int page, int size) {
        Page<ActivityReview> pageParam = new Page<>(page, size);

        if (activityId != null) {
            // 查指定活动的评价
            Page<ActivityReview> result = baseMapper.selectPage(pageParam,
                    new LambdaQueryWrapper<ActivityReview>()
                            .eq(ActivityReview::getActivityId, activityId)
                            .eq(ActivityReview::getDeleted, 0)
                            .orderByDesc(ActivityReview::getCreateTime)
            );
            loadUserInfo(result.getRecords());
            return result;
        }

        // 查所有自己创建的活动的评价
        // 先获取该用户创建的所有活动ID
        List<Activity> myActivities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getOrganizerId, organizerId)
                        .eq(Activity::getDeleted, 0)
                        .select(Activity::getId)
        );

        if (myActivities.isEmpty()) {
            return pageParam;
        }

        List<Long> activityIds = myActivities.stream()
                .map(Activity::getId)
                .toList();

        Page<ActivityReview> result = baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ActivityReview>()
                        .in(ActivityReview::getActivityId, activityIds)
                        .eq(ActivityReview::getDeleted, 0)
                        .orderByDesc(ActivityReview::getCreateTime)
        );
        loadUserInfo(result.getRecords());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long reviewId, Long organizerId, String reply) {
        // 1. 检查评价是否存在
        ActivityReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }

        // 2. 校验回复内容
        if (reply == null || reply.trim().isEmpty()) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        if (reply.trim().length() > 500) {
            throw new BusinessException(400, "回复内容不能超过500字");
        }

        // 3. 检查是否是该活动的组织者
        boolean isOrganizer = permissionService.isOrganizer(review.getActivityId(), organizerId);
        if (!isOrganizer) {
            throw new BusinessException(403, "您无权回复该评价");
        }

        // 4. 更新回复内容（覆盖）
        review.setReply(reply.trim());
        baseMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId) {
        // 1. 检查评价是否存在
        ActivityReview review = getById(reviewId);
        if (review == null) {
            throw new BusinessException(404, "评价不存在");
        }

        // 2. 逻辑删除
        removeById(reviewId);

        // 3. 重新计算活动的平均评分和评价数量
        recalculateActivityRating(review.getActivityId());
    }

    @Override
    public ReviewStatisticsVO getReviewStatistics(Long activityId) {
        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }

        // 获取所有未删除的评价
        List<ActivityReview> reviews = baseMapper.selectList(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getDeleted, 0)
        );

        ReviewStatisticsVO vo = new ReviewStatisticsVO();
        vo.setActivityId(activityId);

        // 总评价数
        int totalCount = reviews.size();
        vo.setTotalCount(totalCount);

        // 平均评分
        double avgRating = 0.0;
        if (totalCount > 0) {
            int sum = reviews.stream().mapToInt(ActivityReview::getRating).sum();
            avgRating = Math.round(sum * 10.0 / totalCount) / 10.0;
        }
        vo.setAvgRating(avgRating);

        // 各星级分布
        Map<String, Integer> ratingDistribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            final int star = i;
            int count = (int) reviews.stream().filter(r -> r.getRating() == star).count();
            ratingDistribution.put(String.valueOf(star), count);
        }
        vo.setRatingDistribution(ratingDistribution);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateActivityRating(Long activityId) {
        List<ActivityReview> reviews = baseMapper.selectList(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getDeleted, 0)
        );

        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return;
        }

        int reviewCount = reviews.size();
        activity.setReviewCount(reviewCount);

        if (reviewCount > 0) {
            int sum = reviews.stream().mapToInt(ActivityReview::getRating).sum();
            double avgRating = Math.round(sum * 10.0 / reviewCount) / 10.0;
            activity.setAvgRating(avgRating);
        } else {
            activity.setAvgRating(null);
        }

        activityMapper.updateById(activity);
    }

    /**
     * 为评价列表加载用户信息
     */
    private void loadUserInfo(List<ActivityReview> reviews) {
        for (ActivityReview review : reviews) {
            User user = userService.getById(review.getUserId());
            if (user != null) {
                User simpleUser = new User();
                simpleUser.setId(user.getId());
                simpleUser.setNickname(user.getNickname());
                simpleUser.setAvatar(user.getAvatar());
                review.setUser(simpleUser);
            }
        }
    }
}
