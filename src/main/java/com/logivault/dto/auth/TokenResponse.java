package com.logivault.dto.auth;

import com.logivault.dto.user.UserSummary;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
}
