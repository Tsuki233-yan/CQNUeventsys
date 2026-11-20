package com.campus.eventsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.eventsys.entity.ActivityResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityResultMapper extends BaseMapper<ActivityResult> {

    /**
     * 查询指定活动ID的成果展示贴（包含已删除的）
     * 用于处理删除成果后再创建的场景
     */
    @Select("SELECT * FROM activity_result WHERE activity_id = #{activityId} LIMIT 1")
    ActivityResult selectDeletedResult(@Param("activityId") Long activityId);
}
