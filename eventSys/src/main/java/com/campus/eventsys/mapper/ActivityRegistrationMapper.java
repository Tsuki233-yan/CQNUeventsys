package com.campus.eventsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.eventsys.entity.ActivityRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityRegistrationMapper extends BaseMapper<ActivityRegistration> {

    /**
     * 查询指定活动ID和用户ID的报名记录（包含已删除的）
     * 用于处理取消报名后再报名的场景
     */
    @Select("SELECT * FROM activity_registration WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    ActivityRegistration selectDeletedRegistration(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /**
     * 恢复已删除的报名记录
     * 直接使用SQL更新，绕过MyBatis Plus的逻辑删除机制
     */
    @Update("UPDATE activity_registration SET deleted = 0, role = #{role}, status = 0, remark = NULL, reviewer_id = NULL, review_time = NULL, review_remark = NULL, create_time = datetime('now') WHERE id = #{id}")
    int restoreRegistration(@Param("id") Long id, @Param("role") Integer role);

    /**
     * 恢复已删除的报名记录并设置状态
     * 直接使用SQL更新，绕过MyBatis Plus的逻辑删除机制
     * 用于报名不需要审核的场景
     */
    @Update("UPDATE activity_registration SET deleted = 0, role = #{role}, status = #{status}, remark = NULL, reviewer_id = NULL, review_time = NULL, review_remark = NULL, create_time = datetime('now') WHERE id = #{id}")
    int restoreRegistrationWithStatus(@Param("id") Long id, @Param("role") Integer role, @Param("status") Integer status);
}
