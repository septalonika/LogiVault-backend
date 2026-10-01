package com.logivault.config;

import com.logivault.exception.ErrorResponseDoc;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI logiVaultOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LogiVault")
                        .version("v1")
                        .description("Inventory and order API. Log in at /api/v1/auth/login, then send "
                                + "data.accessToken as a Bearer token. Every response is wrapped in "
                                + "{ status, message, data }; errors add a stable code."))
                .components(new Components()
                        .schemas(ModelConverters.getInstance().read(ErrorResponseDoc.class))
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Adds the error responses every endpoint shares so controllers only document their own:
     * 400 when there is a body, 401 on protected endpoints, 403 on role-restricted ones.
     * Every 4xx/5xx then gets the ErrorResponse schema instead of the endpoint's success schema.
     * Public endpoints opt out of the global scheme with an empty {@code @SecurityRequirements}.
     */
    @Bean
    public OperationCustomizer commonErrorResponses() {
        return (operation, handlerMethod) -> {
            boolean isPublic = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (operation.getRequestBody() != null) {
                operation.getResponses().addApiResponse("400", new ApiResponse().description("Validation failed (VALIDATION_ERROR)"));
            }
            if (!isPublic) {
                operation.getResponses().addApiResponse("401", new ApiResponse().description("Missing or invalid access token (UNAUTHORIZED)"));
            }
            if (handlerMethod.hasMethodAnnotation(PreAuthorize.class)) {
                operation.getResponses().addApiResponse("403", new ApiResponse().description("Role not allowed (FORBIDDEN)"));
            }
            operation.getResponses().forEach((status, response) -> {
                if (status.startsWith("4") || status.startsWith("5")) {
                    response.setContent(errorContent());
                }
            });
            return operation;
        };
    }

    private static Content errorContent() {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
        return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(schema));
    }
}
