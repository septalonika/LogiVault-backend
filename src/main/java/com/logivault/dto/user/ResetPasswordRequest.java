package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@Schema(example = "new-pass-456") @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
