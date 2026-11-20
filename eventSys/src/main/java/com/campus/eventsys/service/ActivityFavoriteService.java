package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.ActivityFavorite;

import java.util.List;

/**
 * 活动收藏服务接口
 */
public interface ActivityFavoriteService extends IService<ActivityFavorite> {

    /**
     * 收藏活动
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-收藏成功，false-已收藏或失败
     */
    boolean favorite(Long activityId, Long userId);

    /**
     * 取消收藏
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-取消成功，false-未收藏或失败
     */
    boolean unfavorite(Long activityId, Long userId);

    /**
     * 检查是否已收藏
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-已收藏，false-未收藏
     */
    boolean isFavorited(Long activityId, Long userId);

    /**
     * 获取用户的收藏列表
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<ActivityFavorite> getUserFavorites(Long userId);

    /**
     * 获取活动的收藏数量
     *
     * @param activityId 活动ID
     * @return 收藏数量
     */
    long getFavoriteCount(Long activityId);
}
