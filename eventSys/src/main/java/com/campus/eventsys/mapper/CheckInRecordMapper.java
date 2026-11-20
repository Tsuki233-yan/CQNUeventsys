package com.campus.eventsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.eventsys.entity.CheckInRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CheckInRecordMapper extends BaseMapper<CheckInRecord> {
    @Select("""
            SELECT c.*,
                   u.username AS username,
                   u.nickname AS nickname
            FROM check_in_record c
            LEFT JOIN user u ON c.user_id = u.id
            WHERE c.activity_id = #{activityId}
              AND c.deleted = 0
            ORDER BY c.check_in_time DESC
            """)
    List<CheckInRecord> selectActivityCheckInRecordsWithUser(@Param("activityId") Long activityId);
}
