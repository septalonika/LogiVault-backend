package com.logivault.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@logivault.local") @NotBlank @Email String email,
        @Schema(example = "s3cret-pass") @NotBlank String password
) {
}
