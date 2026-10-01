package com.logivault.common.security;

import com.logivault.user.Role;

import java.util.UUID;

// Authentication principal: what JwtAuthenticationFilter puts in the SecurityContext.
public record AuthUser(UUID id, String email, Role role) {
}
