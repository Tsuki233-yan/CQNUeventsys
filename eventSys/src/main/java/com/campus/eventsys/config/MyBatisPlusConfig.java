package com.campus.eventsys.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * MyBatis Plus 配置类
 * 包含分页插件配置和自动填充处理器
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 分页插件配置
     * SQLite数据库需要指定DbType.SQLITE
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // SQLite分页插件配置
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.SQLITE);
        paginationInterceptor.setOverflow(false);
        paginationInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 插入时自动填充字段
     * 使用 GMT+8 时区
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    /**
     * 更新时自动填充字段
     * 使用 GMT+8 时区
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = ZonedDateTime.now(TimeZoneConfig.getDefaultZoneId()).toLocalDateTime();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
    }
}
