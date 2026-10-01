package com.logivault.user.dto;

import com.logivault.user.Role;

import java.util.UUID;

public record UserSummary(UUID id, String name, String email, Role role) {
}
