package com.campus.eventsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.eventsys.entity.ActivityRegistration;

import java.util.List;

/**
 * 活动报名服务接口
 */
public interface ActivityRegistrationService extends IService<ActivityRegistration> {

    /**
     * 获取用户的所有报名记录
     *
     * @param userId 用户ID
     * @return 报名记录列表
     */
    List<ActivityRegistration> getUserRegistrations(Long userId);

    /**
     * 获取用户的参与者报名记录（不包括组织者/管理者身份）
     * 用于"我参与的活动"列表，过滤掉用户作为组织者的活动
     *
     * @param userId 用户ID
     * @return 报名记录列表
     */
    List<ActivityRegistration> getUserParticipantRegistrations(Long userId);

    /**
     * 获取活动的所有报名记录
     *
     * @param activityId 活动ID
     * @return 报名记录列表
     */
    List<ActivityRegistration> getActivityRegistrations(Long activityId);

    /**
     * 获取活动的参与者报名记录（不包括组织者）
     *
     * @param activityId 活动ID
     * @return 报名记录列表
     */
    List<ActivityRegistration> getActivityParticipants(Long activityId);

    /**
     * 审核报名申请
     *
     * @param registrationId 报名记录ID
     * @param status         审核状态：1-通过，2-拒绝
     * @param reviewerId     审核人ID
     * @param reviewRemark   审核备注
     * @return 是否审核成功
     */
    boolean reviewRegistration(Long registrationId, Integer status, Long reviewerId, String reviewRemark);

    /**
     * 移除活动参与者（逻辑删除）
     *
     * @param registrationId 报名记录ID
     * @return 是否移除成功
     */
    boolean removeParticipant(Long registrationId);

    /**
     * 批量逻辑删除活动的所有报名记录（包括组织者和参与者）
     * 用于活动删除或下架时的清理操作
     *
     * @param activityId 活动ID
     * @return 删除的记录数量
     */
    int removeAllRegistrationsByActivity(Long activityId);
}
