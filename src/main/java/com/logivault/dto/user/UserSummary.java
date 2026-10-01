package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.Role;

import java.util.UUID;

public record UserSummary(UUID id, @Schema(example = "Siti Rahma") String name, @Schema(example = "siti@logivault.local") String email, Role role) {
}
