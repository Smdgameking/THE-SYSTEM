package com.thesystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("THE SYSTEM API")
                                .description("AI-powered personal operating system")
                                .version("v0.1.0")
                                .contact(
                                        new Contact()
                                                .name("THE SYSTEM Team")
                                                .url("https://thesystem.example.com")
                                )
                                .license(
                                        new License()
                                                .name("Proprietary")
                                                .url("https://thesystem.example.com/license")
                                )
                );
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}
