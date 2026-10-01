package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(example = "old-pass-123") @NotBlank String oldPassword,
        @Schema(example = "new-pass-456") @NotBlank @Size(min = 8) String newPassword
) {
}
