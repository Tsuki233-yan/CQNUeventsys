package com.campus.eventsys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.eventsys.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    IPage<Notification> selectUserNotificationPage(Page<Notification> page,
                                                    @Param("userId") Long userId,
                                                    @Param("role") Integer role,
                                                    @Param("admin") boolean admin,
                                                    @Param("now") LocalDateTime now);

    Long countUnreadUserNotifications(@Param("userId") Long userId,
                                      @Param("role") Integer role,
                                      @Param("admin") boolean admin,
                                      @Param("now") LocalDateTime now);
}
