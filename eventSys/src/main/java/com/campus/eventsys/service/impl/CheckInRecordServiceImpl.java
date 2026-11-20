package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.ActivityRegistration;
import com.campus.eventsys.entity.CheckInRecord;
import com.campus.eventsys.mapper.ActivityRegistrationMapper;
import com.campus.eventsys.mapper.CheckInRecordMapper;
import com.campus.eventsys.service.CheckInRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 签到记录服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInRecordServiceImpl extends ServiceImpl<CheckInRecordMapper, CheckInRecord>
        implements CheckInRecordService {

    private final ActivityRegistrationMapper registrationMapper;

    @Override
    @Transactional
    public boolean checkIn(Long activityId, Long userId) {
        // 检查是否已签到
        if (hasCheckedIn(activityId, userId)) {
            log.warn("用户已签到: activityId={}, userId={}", activityId, userId);
            return false;
        }

        // 获取报名信息
        ActivityRegistration registration = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getDeleted, 0)
        );

        if (registration == null) {
            log.warn("用户未报名该活动: activityId={}, userId={}", activityId, userId);
            return false;
        }

        try {
            // 创建签到记录
            CheckInRecord record = new CheckInRecord();
            record.setActivityId(activityId);
            record.setUserId(userId);
            record.setRegistrationId(registration.getId());
            record.setCheckInTime(LocalDateTime.now());
            record.setCheckInType(2); // 2-手动签到
            record.setLocation(""); // 可以扩展获取位置信息
            record.setRemark("用户手动签到");

            this.save(record);

            // 更新报名状态为已签到(3)
            registration.setStatus(3);
            registrationMapper.updateById(registration);

            log.info("签到成功: activityId={}, userId={}", activityId, userId);
            return true;
        } catch (Exception e) {
            log.error("签到失败: activityId={}, userId={}", activityId, userId, e);
            return false;
        }
    }

    @Override
    public boolean hasCheckedIn(Long activityId, Long userId) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getActivityId, activityId)
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getDeleted, 0)
        );
        return count != null && count > 0;
    }

    @Override
    public List<CheckInRecord> getActivityCheckInRecords(Long activityId) {
        return baseMapper.selectActivityCheckInRecordsWithUser(activityId);
    }

    @Override
    public List<CheckInRecord> getUserCheckInRecords(Long userId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getDeleted, 0)
                        .orderByDesc(CheckInRecord::getCheckInTime)
        );
    }
}
