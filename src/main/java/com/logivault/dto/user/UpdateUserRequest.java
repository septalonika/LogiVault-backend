package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Schema(example = "Siti Rahma") @NotBlank @Size(max = 100) String name,
        @NotNull Role role
) {
}
