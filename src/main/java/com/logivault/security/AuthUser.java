package com.logivault.security;

import com.logivault.entity.Role;

import java.util.UUID;

// Authentication principal: what JwtAuthenticationFilter puts in the SecurityContext.
public record AuthUser(UUID id, String email, Role role) {
}
