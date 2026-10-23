package com.frauddetection.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI fraudDetectionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Risk & Fraud Detection Platform API")
                        .description("""
                                A portfolio/demo risk engine for transaction analysis.
                                
                                **Note**: This is an educational project demonstrating rule-based risk scoring.
                                It is NOT a production banking fraud detection system.
                                """
                        )
                        .version("1.0.0")
                        .contact(new Contact().name("Portfolio Project"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide a valid JWT token obtained from /api/auth/login")));
    }
}
