package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityFavorite;
import com.campus.eventsys.mapper.ActivityFavoriteMapper;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.service.ActivityFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 活动收藏服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityFavoriteServiceImpl extends ServiceImpl<ActivityFavoriteMapper, ActivityFavorite>
        implements ActivityFavoriteService {

    private final ActivityMapper activityMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean favorite(Long activityId, Long userId) {
        try {
            // 检查是否已收藏（未删除的）
            if (isFavorited(activityId, userId)) {
                log.warn("用户已收藏该活动: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 检查是否存在已删除的收藏记录（取消收藏后再收藏的场景）
            // 使用自定义SQL查询包含已删除的记录，绕过MyBatis Plus的逻辑删除过滤器
            ActivityFavorite existingFavorite = baseMapper.selectDeletedFavorite(activityId, userId);

            if (existingFavorite != null) {
                // 恢复已删除的收藏记录，使用自定义SQL绕过MyBatis Plus逻辑删除机制
                int updated = baseMapper.restoreFavorite(existingFavorite.getId());
                if (updated > 0) {
                    log.info("恢复已删除的收藏记录: activityId={}, userId={}, favoriteId={}", activityId, userId, existingFavorite.getId());
                } else {
                    log.warn("恢复收藏记录失败: activityId={}, userId={}, favoriteId={}", activityId, userId, existingFavorite.getId());
                    return false;
                }
            } else {
                // 创建新的收藏记录
                ActivityFavorite favorite = new ActivityFavorite();
                favorite.setActivityId(activityId);
                favorite.setUserId(userId);
                baseMapper.insert(favorite);
            }

            // 增加活动收藏计数
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                activity.setFavoriteCount(activity.getFavoriteCount() + 1);
                activityMapper.updateById(activity);
            }

            return true;
        } catch (Exception e) {
            log.error("收藏活动失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfavorite(Long activityId, Long userId) {
        try {
            // 查找收藏记录
            ActivityFavorite favorite = baseMapper.selectOne(
                    new LambdaQueryWrapper<ActivityFavorite>()
                            .eq(ActivityFavorite::getActivityId, activityId)
                            .eq(ActivityFavorite::getUserId, userId)
                            .eq(ActivityFavorite::getDeleted, 0)
            );

            if (favorite == null) {
                log.warn("用户未收藏该活动: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 逻辑删除收藏记录
            baseMapper.deleteById(favorite.getId());

            // 减少活动收藏计数
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null && activity.getFavoriteCount() > 0) {
                activity.setFavoriteCount(activity.getFavoriteCount() - 1);
                activityMapper.updateById(activity);
            }

            return true;
        } catch (Exception e) {
            log.error("取消收藏失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    public boolean isFavorited(Long activityId, Long userId) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<ActivityFavorite>()
                        .eq(ActivityFavorite::getActivityId, activityId)
                        .eq(ActivityFavorite::getUserId, userId)
                        .eq(ActivityFavorite::getDeleted, 0)
        );
        return count > 0;
    }

    @Override
    public List<ActivityFavorite> getUserFavorites(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ActivityFavorite>()
                        .eq(ActivityFavorite::getUserId, userId)
                        .eq(ActivityFavorite::getDeleted, 0)
                        .orderByDesc(ActivityFavorite::getCreateTime)
        );
    }

    @Override
    public long getFavoriteCount(Long activityId) {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<ActivityFavorite>()
                        .eq(ActivityFavorite::getActivityId, activityId)
                        .eq(ActivityFavorite::getDeleted, 0)
        );
    }
}
