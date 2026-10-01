package com.logivault.security;

import com.logivault.entity.Role;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthUser get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return authUser;
    }

    public static UUID id() {
        return get().id();
    }

    public static Role role() {
        return get().role();
    }
}
