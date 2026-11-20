package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.mapper.ActivityRegistrationMapper;
import com.campus.eventsys.mapper.UserMapper;
import com.campus.eventsys.service.ActivityRegistrationService;
import com.campus.eventsys.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动报名服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRegistrationServiceImpl extends ServiceImpl<ActivityRegistrationMapper, ActivityRegistration>
        implements ActivityRegistrationService {

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    public List<ActivityRegistration> getUserRegistrations(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getDeleted, 0)
                        .orderByDesc(ActivityRegistration::getCreateTime)
        );
    }

    @Override
    public List<ActivityRegistration> getUserParticipantRegistrations(Long userId) {
        // 只获取角色为参与者的报名记录（不包括组织者/管理者）
        // 用于"我参与的活动"列表，避免创建的活动也显示在参与列表中
        return baseMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getRole, ActivityRole.PARTICIPANT.getCode())
                        .eq(ActivityRegistration::getDeleted, 0)
                        .orderByDesc(ActivityRegistration::getCreateTime)
        );
    }

    @Override
    public List<ActivityRegistration> getActivityRegistrations(Long activityId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getDeleted, 0)
                        .orderByDesc(ActivityRegistration::getCreateTime)
        );
    }

    @Override
    public List<ActivityRegistration> getActivityParticipants(Long activityId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getRole, ActivityRole.PARTICIPANT.getCode())
                        .eq(ActivityRegistration::getDeleted, 0)
                        .orderByDesc(ActivityRegistration::getCreateTime)
        );
    }

    @Override
    @Transactional
    public boolean reviewRegistration(Long registrationId, Integer status, Long reviewerId, String reviewRemark) {
        try {
            ActivityRegistration registration = getById(registrationId);
            if (registration == null || registration.getDeleted() == 1) {
                log.warn("报名记录不存在或已删除: registrationId={}", registrationId);
                return false;
            }

            // 只能审核待审核状态的报名
            if (registration.getStatus() != 0) {
                log.warn("报名记录状态不是待审核: registrationId={}, status={}", registrationId, registration.getStatus());
                return false;
            }

            registration.setStatus(status);
            registration.setReviewerId(reviewerId);
            registration.setReviewTime(LocalDateTime.now());
            registration.setReviewRemark(reviewRemark);

            boolean success = updateById(registration);

            // 如果审核通过，增加活动当前参与人数与用户参与统计（与报名即通过路径保持一致）
            if (success && status == 1) {
                Activity activity = activityMapper.selectById(registration.getActivityId());
                if (activity != null) {
                    activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
                    activityMapper.updateById(activity);
                }

                // 同步增加用户参与活动统计
                User participant = userMapper.selectById(registration.getUserId());
                if (participant != null) {
                    participant.setParticipatedCount(
                            participant.getParticipatedCount() == null ? 1 : participant.getParticipatedCount() + 1);
                    userMapper.updateById(participant);
                }
            }

            // 发送报名审核结果通知
            if (success) {
                Activity activity = activityMapper.selectById(registration.getActivityId());
                if (activity != null) {
                    boolean approved = (status == 1);
                    notificationService.sendAuditResult(
                            registration.getUserId(),
                            activity.getId(),
                            activity.getTitle(),
                            approved,
                            approved ? null : reviewRemark
                    );
                    log.info("发送报名审核通知: userId={}, activityId={}, approved={}", 
                            registration.getUserId(), activity.getId(), approved);
                }
            }

            return success;
        } catch (Exception e) {
            log.error("审核报名失败: registrationId={}", registrationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeParticipant(Long registrationId) {
        try {
            ActivityRegistration registration = getById(registrationId);
            if (registration == null || registration.getDeleted() == 1) {
                log.warn("报名记录不存在或已删除: registrationId={}", registrationId);
                return false;
            }

            // 只能移除参与者，不能移除组织者
            if (registration.getRole() == ActivityRole.ORGANIZER.getCode()) {
                log.warn("不能通过此方法移除组织者: registrationId={}", registrationId);
                return false;
            }

            Long activityId = registration.getActivityId();

            // 逻辑删除报名记录
            boolean success = removeById(registrationId);

            // 减少活动当前参与人数（只有已通过审核的参与者才减少）
            if (success && registration.getStatus() == 1) {
                Activity activity = activityMapper.selectById(activityId);
                if (activity != null && activity.getCurrentParticipants() > 0) {
                    activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
                    activityMapper.updateById(activity);
                }
            }

            return success;
        } catch (Exception e) {
            log.error("移除参与者失败: registrationId={}", registrationId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int removeAllRegistrationsByActivity(Long activityId) {
        try {
            // 查询该活动下所有未删除的报名记录（包括组织者和参与者）
            List<ActivityRegistration> registrations = baseMapper.selectList(
                    new LambdaQueryWrapper<ActivityRegistration>()
                            .eq(ActivityRegistration::getActivityId, activityId)
                            .eq(ActivityRegistration::getDeleted, 0)
            );

            if (registrations.isEmpty()) {
                return 0;
            }

            int count = 0;
            for (ActivityRegistration registration : registrations) {
                // 逻辑删除报名记录
                boolean success = removeById(registration.getId());
                if (success) {
                    count++;
                }
            }

            log.info("批量删除活动报名记录: activityId={}, 删除数量={}", activityId, count);
            return count;
        } catch (Exception e) {
            log.error("批量删除活动报名记录失败: activityId={}", activityId, e);
            return 0;
        }
    }
}
