package com.flow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flow — Workflow Automation API")
                        .description(
                                "A distributed workflow automation backend " +
                                        "similar to n8n and Make.com. " +
                                        "Supports webhook triggers, cron scheduling, " +
                                        "HTTP nodes, condition nodes, AI nodes (Gemini), " +
                                        "and Slack notifications with async Redis queue " +
                                        "and exponential backoff retry."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your@email.com")
                                .url("https://github.com/yourusername/flow-backend")
                        )
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development"),
                        new Server()
                                .url("https://your-app.railway.app")
                                .description("Production")
                ))
                // JWT security scheme
                .addSecurityItem(
                        new SecurityRequirement().addList("Bearer Authentication")
                )
                .components(new Components()
                        .addSecuritySchemes(
                                "Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Enter your JWT token. " +
                                                        "Get it from POST /api/auth/login"
                                        )
                        )
                );
    }
}
