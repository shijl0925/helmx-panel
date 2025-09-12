package com.helmx.tutorial.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI配置类
 * 用于配置Swagger API文档的相关信息和安全策略
 */
@Configuration
public class OpenAPIConfig {

    /**
     * 创建OpenAPI配置Bean
     * 配置API文档的基本信息、联系人、许可证、安全策略等
     *
     * @return OpenAPI对象，包含完整的API文档配置信息
     */
    @Bean
    public OpenAPI myOpenAPI() {

        // 配置API联系人信息
        Contact contact = new Contact();
        contact.setEmail("kevin09254930sjl@gmail.com");
        contact.setName("Kevin Shi");
        contact.setUrl("https://github.com/shijl0925");

        // 配置API许可证信息
        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        // 配置API基本信息
        Info info = new Info()
                .title("Vben Management API")
                .version("1.0")
                .contact(contact)
                .description("This API exposes endpoints to manage Vben API.")
                .termsOfService("https://github.com/shijl0925/helmx-admin")
                .license(mitLicense);

        // 配置JWT安全策略
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // 添加安全需求
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        // 构建并返回OpenAPI配置对象
        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme));
    }

}
