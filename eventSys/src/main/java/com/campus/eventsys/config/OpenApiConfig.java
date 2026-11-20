package com.campus.eventsys.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * 配置API文档的基本信息和安全认证
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI文档基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 定义安全方案名称
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // API基本信息
                .info(new Info()
                        .title("校园活动综合管理系统 API")
                        .description("校园活动综合管理系统的后端接口文档，包含用户认证、活动管理、报名管理等功能模块")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("耿莉莉")
                                .email("dev@lntu.edu.cn"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // 配置JWT安全认证
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
