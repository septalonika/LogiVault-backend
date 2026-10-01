package com.logivault.auth;

import com.logivault.auth.dto.LoginRequest;
import com.logivault.auth.dto.RefreshRequest;
import com.logivault.auth.dto.TokenResponse;
import com.logivault.config.LogiVaultProperties;
import com.logivault.exception.BusinessException;
import com.logivault.exception.ErrorCode;
import com.logivault.security.AuthUser;
import com.logivault.security.JwtService;
import com.logivault.user.User;
import com.logivault.user.UserMapper;
import com.logivault.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    // A syntactically valid BCrypt hash that matches no real password, so a login attempt for an
    // unknown email still runs a password check (same response time as a wrong-password attempt).
    private static final String DUMMY_PASSWORD_HASH = new BCryptPasswordEncoder(12).encode("logivault-dummy-password");

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final LogiVaultProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService, UserMapper userMapper,
                        LogiVaultProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !user.isActive() || !passwordMatches) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .filter(token -> token.isUsable(Instant.now(clock)))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.getUser().isActive()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        stored.setRevokedAt(Instant.now(clock));
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now(clock));
                    refreshTokenRepository.save(token);
                });
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(new AuthUser(user.getId(), user.getEmail(), user.getRole()));

        String rawRefreshToken = generateRefreshToken();
        Instant now = Instant.now(clock);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .expiresAt(now.plus(properties.jwt().refreshTokenTtl()))
                .createdAt(now)
                .build());

        return new TokenResponse(accessToken, rawRefreshToken, "Bearer",
                properties.jwt().accessTokenTtl().toSeconds(), userMapper.toSummary(user));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
