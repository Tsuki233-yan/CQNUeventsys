package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.ActivityReview;
import com.campus.eventsys.entity.ReviewStatisticsVO;

/**
 * 活动评价服务接口
 * 提供评价的创建、查询、回复、删除及统计功能
 */
public interface ActivityReviewService extends IService<ActivityReview> {

    /**
     * 提交活动评价
     * 业务规则：活动已结束、用户已报名且已签到、未重复评价
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param rating     评分1-5
     * @param content    评价内容
     * @return 评价记录
     */
    ActivityReview createReview(Long activityId, Long userId, Integer rating, String content);

    /**
     * 获取某活动的评价列表（分页）
     *
     * @param activityId 活动ID
     * @param page      页码
     * @param size      每页大小
     * @return 分页结果
     */
    Page<ActivityReview> getActivityReviews(Long activityId, int page, int size);

    /**
     * 获取我的评价列表（分页）
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    Page<ActivityReview> getMyReviews(Long userId, int page, int size);

    /**
     * 组织者查看自己活动的评价（分页）
     *
     * @param organizerId 组织者用户ID
     * @param activityId  活动ID（可选，不传则查所有自己创建的活动评价）
     * @param page        页码
     * @param size        每页大小
     * @return 分页结果
     */
    Page<ActivityReview> getOrganizerReviews(Long organizerId, Long activityId, int page, int size);

    /**
     * 组织者回复评价
     * 只有活动创建者或组织者可回复，重复调用覆盖回复内容
     *
     * @param reviewId   评价ID
     * @param organizerId 回复者用户ID
     * @param reply      回复内容
     */
    void replyReview(Long reviewId, Long organizerId, String reply);

    /**
     * 管理员删除评价
     * 删除后重新计算该活动的 avg_rating 和 review_count
     *
     * @param reviewId 评价ID
     */
    void deleteReview(Long reviewId);

    /**
     * 获取活动评价统计
     *
     * @param activityId 活动ID
     * @return 统计数据（avgRating, totalCount, ratingDistribution）
     */
    ReviewStatisticsVO getReviewStatistics(Long activityId);

    /**
     * 重新计算活动的平均评分和评价数量
     *
     * @param activityId 活动ID
     */
    void recalculateActivityRating(Long activityId);
}
