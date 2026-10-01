package com.logivault.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Typed view of the {@code logivault.*} settings in application.yml. Validated at startup (fail fast).
 */
@Validated
@ConfigurationProperties("logivault")
public record LogiVaultProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Login login,
        @NotNull ZoneId businessZone,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Admin admin
) {

    public record Jwt(
            @NotBlank(message = "JWT_SECRET must be set") String secret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl
    ) {
    }

    public record Login(
            @Min(1) int maxAttempts,
            @NotNull Duration lockDuration
    ) {
    }

    public record Cors(@DefaultValue List<String> allowedOrigins) {
    }

    /** Seed values for the first ADMIN account; only read by the Flyway seed migration. */
    public record Admin(String email, String password) {
    }
}
