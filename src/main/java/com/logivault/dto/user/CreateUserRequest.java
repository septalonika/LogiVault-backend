package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(example = "Siti Rahma") @NotBlank @Size(max = 100) String name,
        @Schema(example = "siti@logivault.local") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "s3cret-pass") @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {
}
