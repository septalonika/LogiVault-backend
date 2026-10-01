package com.logivault.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(example = "b1f6c0de-0000-4a1b-9c3d-7e8f9a0b1c2d") @NotBlank String refreshToken
) {
}
