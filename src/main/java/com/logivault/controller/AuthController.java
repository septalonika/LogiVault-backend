package com.logivault.controller;

import com.logivault.dto.WebResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.auth.LoginRequest;
import com.logivault.dto.auth.RefreshRequest;
import com.logivault.dto.auth.TokenResponse;
import com.logivault.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Login, token refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", description = "Tokens issued")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
    @ApiResponse(responseCode = "429", description = "TOO_MANY_ATTEMPTS")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<WebResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return WebResponse.ok("Login successful", authService.login(request));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @ApiResponse(responseCode = "200", description = "Tokens issued")
    @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<WebResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return WebResponse.ok("Token refreshed", authService.refresh(request));
    }

    @Operation(summary = "Revoke a refresh token")
    @ApiResponse(responseCode = "200", description = "Token revoked")
    @PostMapping("/logout")
    public ResponseEntity<WebResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return WebResponse.ok("Logged out");
    }
}
