package com.campus.eventsys.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 时区配置类
 * 统一设置应用时区为 GMT+8（北京时间）
 */
@Slf4j
@Configuration
public class TimeZoneConfig {

    private static final String DEFAULT_TIME_ZONE = "UTC";

    @PostConstruct
    public void setTimeZone() {
        // 设置应用默认时区
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
        log.info("应用时区已设置为: {}", DEFAULT_TIME_ZONE);
    }

    /**
     * 获取应用默认时区ID
     */
    public static ZoneId getDefaultZoneId() {
        return ZoneId.of(DEFAULT_TIME_ZONE);
    }
}
