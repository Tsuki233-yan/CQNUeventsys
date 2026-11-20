package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.enums.ActivityRole;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.mapper.ActivityRegistrationMapper;
import com.campus.eventsys.service.ActivityPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import com.campus.eventsys.config.TimeZoneConfig;
import java.util.List;

/**
 * 活动权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityPermissionServiceImpl implements ActivityPermissionService {

    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;

    @Override
    public boolean isOrganizer(Long activityId, Long userId) {
        ActivityRegistration registration = getUserRole(activityId, userId);
        return registration != null
                && registration.getRole() != null
                && registration.getRole() == ActivityRole.ORGANIZER.getCode();
    }

    @Override
    public boolean isParticipant(Long activityId, Long userId) {
        ActivityRegistration registration = getUserRole(activityId, userId);
        // 参与者：角色为PARTICIPANT且状态为已通过(1)或已签到(3)
        // 签到后报名状态会更新为3，已签到的用户同样是活动的正式参与者（否则已签到用户将无法评价活动）
        return registration != null
                && registration.getRole() != null
                && registration.getRole() == ActivityRole.PARTICIPANT.getCode()
                && registration.getStatus() != null
                && (registration.getStatus() == 1 || registration.getStatus() == 3);
    }

    @Override
    public boolean canModifyActivity(Long activityId, Long userId) {
        // 检查是否是组织者
        if (!isOrganizer(activityId, userId)) {
            return false;
        }

        ActivityRegistration registration = getUserRole(activityId, userId);
        // 组织者必须已通过报名才能修改活动
        return registration != null
                && registration.getStatus() != null
                && registration.getStatus() == 1;
    }

    @Override
    public boolean canCheckIn(Long activityId, Long userId) {
        // 1. 检查是否是参与者（已通过报名）
        ActivityRegistration registration = getUserRole(activityId, userId);
        if (registration == null || registration.getStatus() == null || registration.getStatus() != 1) {
            return false;
        }

        // 2. 检查活动状态是否为进行中(4)
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() == null || activity.getStatus() != 4) {
            return false;
        }

        // 3. 检查活动是否已开始（当前时间 >= 活动开始时间）
        LocalDateTime now = ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime();
        return now.isAfter(activity.getStartTime())
                || now.isEqual(activity.getStartTime());
    }

    @Override
    public ActivityRegistration getUserRole(Long activityId, Long userId) {
        return registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getDeleted, 0)
        );
    }

    @Override
    public boolean setUserAsOrganizer(Long activityId, Long userId) {
        try {
            // 检查是否已有报名记录
            ActivityRegistration existing = getUserRole(activityId, userId);
            if (existing != null) {
                // 更新为组织者角色
                existing.setRole(ActivityRole.ORGANIZER.getCode());
                existing.setStatus(1); // 已通过
                registrationMapper.updateById(existing);
            } else {
                // 创建新的报名记录，设置为组织者
                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivityId(activityId);
                registration.setUserId(userId);
                registration.setRole(ActivityRole.ORGANIZER.getCode());
                registration.setStatus(1); // 已通过
                registrationMapper.insert(registration);
            }
            return true;
        } catch (Exception e) {
            log.error("设置用户为组织者失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    public boolean addParticipant(Long activityId, Long userId, ActivityRole role) {
        return addParticipantWithImages(activityId, userId, role, null, null);
    }

    @Override
    public boolean addParticipantWithImages(Long activityId, Long userId, ActivityRole role, String remark, String images) {
        // 默认报名即通过（无需审核的活动）
        return addParticipantWithStatus(activityId, userId, role, remark, images, 1);
    }

    @Override
    public boolean addParticipantWithStatus(Long activityId, Long userId, ActivityRole role, String remark, String images, Integer status) {
        try {
            // 检查是否已报名（未删除的）
            ActivityRegistration existing = getUserRole(activityId, userId);
            if (existing != null) {
                log.warn("用户已报名该活动: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 检查是否存在已删除的报名记录（取消报名后再报名的场景）
            // 使用自定义SQL查询包含已删除的记录，绕过MyBatis Plus的逻辑删除过滤器
            ActivityRegistration deletedRegistration = registrationMapper.selectDeletedRegistration(activityId, userId);

            if (deletedRegistration != null) {
                // 恢复已删除的报名记录，使用自定义SQL绕过MyBatis Plus逻辑删除机制
                // 状态由调用方指定：0-待审核（活动需审核报名），1-已通过（报名即通过）
                int updated = registrationMapper.restoreRegistrationWithStatus(deletedRegistration.getId(), role.getCode(), status);
                if (updated > 0) {
                    log.info("恢复已删除的报名记录: activityId={}, userId={}, registrationId={}", activityId, userId, deletedRegistration.getId());
                    // 更新备注和图片
                    deletedRegistration.setRemark(remark);
                    deletedRegistration.setImages(images);
                    deletedRegistration.setStatus(status);
                    registrationMapper.updateById(deletedRegistration);
                } else {
                    log.warn("恢复报名记录失败: activityId={}, userId={}, registrationId={}", activityId, userId, deletedRegistration.getId());
                    return false;
                }
            } else {
                // 创建新的报名记录
                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivityId(activityId);
                registration.setUserId(userId);
                registration.setRole(role.getCode());
                registration.setStatus(status); // 0-待审核，1-已通过
                registration.setRemark(remark);
                registration.setImages(images);
                registrationMapper.insert(registration);
            }

            return true;
        } catch (Exception e) {
            log.error("添加参与者失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    public List<ActivityRegistration> getActivityOrganizers(Long activityId) {
        return registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getRole, ActivityRole.ORGANIZER.getCode())
                        .eq(ActivityRegistration::getDeleted, 0)
                        .orderByDesc(ActivityRegistration::getCreateTime)
        );
    }

    @Override
    @Transactional
    public boolean addOrganizer(Long activityId, Long userId, Long operatorId) {
        try {
            // 1. 检查操作人是否是组织者
            if (!isOrganizer(activityId, operatorId)) {
                log.warn("操作人不是组织者，无权添加组织者: activityId={}, operatorId={}", activityId, operatorId);
                return false;
            }

            // 2. 检查目标用户是否已经是组织者
            if (isOrganizer(activityId, userId)) {
                log.warn("用户已经是组织者: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 3. 检查目标用户是否已经是参与者
            ActivityRegistration existing = getUserRole(activityId, userId);
            if (existing != null) {
                // 更新为组织者角色
                existing.setRole(ActivityRole.ORGANIZER.getCode());
                existing.setStatus(1); // 已通过
                registrationMapper.updateById(existing);
                log.info("将参与者提升为组织者: activityId={}, userId={}", activityId, userId);
            } else {
                // 创建新的组织者记录
                ActivityRegistration registration = new ActivityRegistration();
                registration.setActivityId(activityId);
                registration.setUserId(userId);
                registration.setRole(ActivityRole.ORGANIZER.getCode());
                registration.setStatus(1); // 已通过
                registrationMapper.insert(registration);
                log.info("添加新组织者: activityId={}, userId={}", activityId, userId);
            }

            return true;
        } catch (Exception e) {
            log.error("添加组织者失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeOrganizer(Long activityId, Long userId, Long operatorId) {
        try {
            // 1. 检查操作人是否是组织者
            if (!isOrganizer(activityId, operatorId)) {
                log.warn("操作人不是组织者，无权移除组织者: activityId={}, operatorId={}", activityId, operatorId);
                return false;
            }

            // 2. 不能移除自己
            if (userId.equals(operatorId)) {
                log.warn("组织者不能移除自己: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 3. 检查目标用户是否是组织者
            if (!isOrganizer(activityId, userId)) {
                log.warn("用户不是组织者，无法移除: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 4. 获取组织者记录
            ActivityRegistration registration = getUserRole(activityId, userId);
            if (registration == null) {
                log.warn("找不到组织者记录: activityId={}, userId={}", activityId, userId);
                return false;
            }

            // 5. 逻辑删除组织者记录
            registrationMapper.deleteById(registration.getId());
            log.info("移除组织者: activityId={}, userId={}", activityId, userId);

            return true;
        } catch (Exception e) {
            log.error("移除组织者失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }
}
