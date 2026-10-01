package com.logivault.dto.user;

import com.logivault.entity.Role;

import java.util.UUID;

public record UserSummary(UUID id, String name, String email, Role role) {
}
