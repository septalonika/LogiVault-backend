package com.logivault.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, @Schema(example = "Siti Rahma") String name, @Schema(example = "siti@logivault.local") String email, Role role, boolean active, Instant createdAt) {
}
