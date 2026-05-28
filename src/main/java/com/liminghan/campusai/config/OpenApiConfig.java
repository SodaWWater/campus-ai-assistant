package com.liminghan.campusai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Campus AI Assistant API")
                        .description("APIs for campus AI assistant.")
                        .version("v1"));
    }
}
