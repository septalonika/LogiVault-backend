package com.logivault.config;

import com.logivault.exception.ProblemDoc;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.core.converter.ModelConverters;
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
                        .description("Inventory and order API. Log in at /api/v1/auth/login, then send the "
                                + "accessToken as a Bearer token."))
                
                .components(new Components()
                        .schemas(ModelConverters.getInstance().read(ProblemDoc.class))
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Adds the error responses every endpoint shares so controllers only document their own:
     * 400 when there is a body, 401 on protected endpoints, 403 on role-restricted ones.
     * Public endpoints opt out of the global scheme with an empty {@code @SecurityRequirements}.
     */
    @Bean
    public OperationCustomizer commonErrorResponses() {
        return (operation, handlerMethod) -> {
            boolean isPublic = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (operation.getRequestBody() != null) {
                operation.getResponses().addApiResponse("400", problem("Validation failed (VALIDATION_ERROR)"));
            }
            if (!isPublic) {
                operation.getResponses().addApiResponse("401", problem("Missing or invalid access token (UNAUTHORIZED)"));
            }
            if (handlerMethod.hasMethodAnnotation(PreAuthorize.class)) {
                operation.getResponses().addApiResponse("403", problem("Role not allowed (FORBIDDEN)"));
            }
            return operation;
        };
    }

    private static ApiResponse problem(String description) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/Problem");
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        new MediaType().schema(schema)));
    }
}
