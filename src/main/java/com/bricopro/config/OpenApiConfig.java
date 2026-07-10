package com.bricopro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "BricoPro API",
                version = "1.0.0",
                description = "REST API for BricoPro — Plateforme de services a domicile au Maroc. " +
                        "To authenticate: POST /api/v1/auth/login -> copy accessToken -> " +
                        "click Authorize below and paste it.",
                contact = @io.swagger.v3.oas.annotations.info.Contact(
                        name = "Bricopro",
                        email = "contact@bricopro.ma"
                )
        ),
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"),
        tags = {
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication",  description = "Register, login, OTP verification, token refresh and logout"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Users",           description = "User profiles, worker search, availability calendar"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Tasks",           description = "Task lifecycle: create, accept, start, complete, cancel, review"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Payments",        description = "Initiate payments and view payment history"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Messaging",       description = "Conversations and real-time messages (WebSocket + REST)"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Notifications",   description = "In-app and push notifications"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Analytics",       description = "Platform-wide and per-worker analytics dashboards"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Admin",           description = "Admin-only: user management, CIN verification, dispute resolution"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Geolocation",     description = "Find nearby workers by GPS coordinates"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Matching",        description = "Smart worker-to-task matching engine"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Upload",          description = "File and image upload with local serving"),
                @io.swagger.v3.oas.annotations.tags.Tag(name = "Offline Sync",    description = "Replay queued client actions after connectivity is restored")
        }
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter the JWT access token returned by POST /api/v1/auth/login"
)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BricoPro API")
                        .version("1.0.0")
                        .description("Backend REST API for BricoPro — Services à domicile au Maroc")
                        .contact(new Contact()
                                .name("Bricopro")
                                .email("contact@bricopro.ma")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token here")));
    }
}