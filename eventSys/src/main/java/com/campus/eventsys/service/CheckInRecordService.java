package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.CheckInRecord;

import java.util.List;

/**
 * 签到记录服务接口
 */
public interface CheckInRecordService extends IService<CheckInRecord> {

    /**
     * 用户签到
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-签到成功，false-签到失败（已签到或其他原因）
     */
    boolean checkIn(Long activityId, Long userId);

    /**
     * 检查用户是否已签到
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-已签到，false-未签到
     */
    boolean hasCheckedIn(Long activityId, Long userId);

    /**
     * 获取活动的签到记录列表
     *
     * @param activityId 活动ID
     * @return 签到记录列表
     */
    List<CheckInRecord> getActivityCheckInRecords(Long activityId);

    /**
     * 获取用户的签到记录列表
     *
     * @param userId 用户ID
     * @return 签到记录列表
     */
    List<CheckInRecord> getUserCheckInRecords(Long userId);
}
