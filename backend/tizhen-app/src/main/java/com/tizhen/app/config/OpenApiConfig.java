package com.tizhen.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开发期接口文档配置。生产环境可通过 springdoc.api-docs.enabled=false 关闭。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tizhenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("题阵 API")
                        .version("v1")
                        .description("选手端 /api/v1，运营后台 /api/v1/admin"));
    }
}
