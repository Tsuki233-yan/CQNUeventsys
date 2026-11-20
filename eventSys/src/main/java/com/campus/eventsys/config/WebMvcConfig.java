package com.campus.eventsys.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置类
 * 配置静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传的图片目录
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadAbsolutePath = uploadDir.toUri().toString();

        // 主路径：/uploads/images/**
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(uploadAbsolutePath)
                .setCachePeriod(3600);
        
        // 兼容数据库中的路径：/images/**
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadAbsolutePath)
                .setCachePeriod(3600);
    }
}
