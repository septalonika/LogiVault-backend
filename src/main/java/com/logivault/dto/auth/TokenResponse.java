package com.logivault.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import com.logivault.dto.user.UserSummary;

public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(example = "b1f6c0de-0000-4a1b-9c3d-7e8f9a0b1c2d") String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresIn,
        UserSummary user
) {
}
