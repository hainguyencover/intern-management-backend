package com.holaho.intern.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class OpenAPIConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Internship Management System API")
                                                .version("1.0")
                                                .description("API documentation for the Internship Management System"))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .name("bearerAuth")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")));
        }

        @Bean
        public OperationCustomizer customizeTenantHeader() {
                return (operation, handlerMethod) -> {
                        // Skip adding X-Tenant-ID for Swagger docs metadata retrieval itself (if matching)
                        operation.addParametersItem(new HeaderParameter()
                                        .name("X-Tenant-ID")
                                        .description("ID of the Tenant (Company/Organization) for scoping requests")
                                        .required(true)
                                        .schema(new StringSchema()._default("1")));
                        return operation;
                };
        }
}
