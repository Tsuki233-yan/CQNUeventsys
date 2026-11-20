package com.campus.eventsys.service;

import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.enums.ActivityRole;

import java.util.List;

/**
 * 活动权限服务接口
 * 检查用户在活动中的权限
 */
public interface ActivityPermissionService {

    /**
     * 检查用户是否是活动的组织者/管理者
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-是组织者，false-不是
     */
    boolean isOrganizer(Long activityId, Long userId);

    /**
     * 检查用户是否是活动的参与者（已通过报名）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-是参与者，false-不是
     */
    boolean isParticipant(Long activityId, Long userId);

    /**
     * 检查用户是否可以修改活动信息
     * 条件：是活动组织者且报名状态为已通过
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-可以修改，false-不可以
     */
    boolean canModifyActivity(Long activityId, Long userId);

    /**
     * 检查用户是否可以签到
     * 条件：是活动参与者、报名已通过、活动已开始
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return true-可以签到，false-不可以
     */
    boolean canCheckIn(Long activityId, Long userId);

    /**
     * 获取用户在活动中的角色
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 角色信息，未报名返回null
     */
    ActivityRegistration getUserRole(Long activityId, Long userId);

    /**
     * 设置用户为活动组织者
     * 通常在创建活动时调用
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 是否设置成功
     */
    boolean setUserAsOrganizer(Long activityId, Long userId);

    /**
     * 添加活动参与者
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param role       角色
     * @return 是否添加成功
     */
    boolean addParticipant(Long activityId, Long userId, ActivityRole role);

    /**
     * 添加活动参与者（带图片）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param role       角色
     * @param remark     备注
     * @param images     图片URL列表（JSON格式）
     * @return 是否添加成功
     */
    boolean addParticipantWithImages(Long activityId, Long userId, ActivityRole role, String remark, String images);

    /**
     * 添加活动参与者（带图片，可指定报名状态）
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param role       角色
     * @param remark     备注
     * @param images     图片URL列表（JSON格式）
     * @param status     报名状态：0-待审核，1-已通过
     * @return 是否添加成功
     */
    boolean addParticipantWithStatus(Long activityId, Long userId, ActivityRole role, String remark, String images, Integer status);

    /**
     * 添加活动组织者
     * 由现有组织者添加其他用户为组织者
     *
     * @param activityId 活动ID
     * @param userId     要添加的用户ID
     * @param operatorId 操作人ID（必须是现有组织者）
     * @return 是否添加成功
     */
    boolean addOrganizer(Long activityId, Long userId, Long operatorId);

    /**
     * 移除活动组织者
     * 组织者可以移除其他组织者，但不能移除自己
     *
     * @param activityId 活动ID
     * @param userId     要移除的用户ID
     * @param operatorId 操作人ID
     * @return 是否移除成功
     */
    boolean removeOrganizer(Long activityId, Long userId, Long operatorId);

    /**
     * 获取活动的所有组织者
     *
     * @param activityId 活动ID
     * @return 组织者报名记录列表
     */
    List<ActivityRegistration> getActivityOrganizers(Long activityId);
}
