package com.example.studentdashboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html — includes a "Bearer token" auth scheme so protected endpoints are testable in-browser. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI academyOpenApi() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Student Performance & Attendance Analytics Dashboard API")
                        .description("Backend for the Brightfield Academy dashboard — students, teachers, classes, "
                                + "subjects, results, attendance, dashboards, analytics, reports, and notifications.")
                        .version("1.0.0")
                        .contact(new Contact().name("Brightfield Academy")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token returned by POST /api/auth/login (without \"Bearer \" prefix — Swagger adds it).")));
    }
}
