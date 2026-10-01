package com.logivault.auth.dto;

import com.logivault.user.dto.UserSummary;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
}
