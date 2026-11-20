package com.campus.eventsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.eventsys.entity.ActivityFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityFavoriteMapper extends BaseMapper<ActivityFavorite> {

    /**
     * 查询指定活动ID和用户ID的收藏记录（包含已删除的）
     * 用于处理取消收藏后再收藏的场景
     */
    @Select("SELECT * FROM activity_favorite WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    ActivityFavorite selectDeletedFavorite(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /**
     * 恢复已删除的收藏记录
     * 直接使用SQL更新，绕过MyBatis Plus的逻辑删除机制
     */
    @Update("UPDATE activity_favorite SET deleted = 0, create_time = datetime('now') WHERE id = #{id}")
    int restoreFavorite(@Param("id") Long id);
}
