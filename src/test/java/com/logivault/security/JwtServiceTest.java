package com.logivault.security;

import com.logivault.config.LogiVaultProperties;
import com.logivault.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "jwt-secret-for-logivault-be-9999".getBytes());

    @Test
    void createAndParse_roundTripsTheSameUser() {
        JwtService jwtService = jwtService(Clock.systemUTC());
        AuthUser user = new AuthUser(UUID.randomUUID(), "admin@logivault.test", Role.ADMIN);

        String token = jwtService.createAccessToken(user);

        assertThat(jwtService.parse(token)).contains(user);
    }

    @Test
    void parse_rejectsExpiredToken() {
        Instant fixedNow = Instant.parse("2026-01-01T00:00:00Z");
        Clock issuingClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        JwtService issuer = jwtService(issuingClock);
        String token = issuer.createAccessToken(new AuthUser(UUID.randomUUID(), "a@b.com", Role.STAFF));

        Clock laterClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(16)), ZoneOffset.UTC);
        JwtService verifier = jwtService(laterClock);

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void parse_rejectsTokenSignedWithADifferentSecret() {
        JwtService signedWithKeyA = jwtService(Clock.systemUTC());
        String otherSecret = Base64.getEncoder().encodeToString("a-completely-different-32-byte-key".getBytes());
        JwtService signedWithKeyB = new JwtService(properties(otherSecret), Clock.systemUTC());

        String token = signedWithKeyA.createAccessToken(new AuthUser(UUID.randomUUID(), "a@b.com", Role.STAFF));

        assertThat(signedWithKeyB.parse(token)).isEmpty();
    }

    @Test
    void parse_rejectsGarbageToken() {
        assertThat(jwtService(Clock.systemUTC()).parse("not-a-jwt")).isEmpty();
    }

    @Test
    void constructor_rejectsSecretShorterThan32Bytes() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new JwtService(properties(shortSecret), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtService jwtService(Clock clock) {
        return new JwtService(properties(SECRET), clock);
    }

    private static LogiVaultProperties properties(String secret) {
        return new LogiVaultProperties(
                new LogiVaultProperties.Jwt(secret, Duration.ofMinutes(15), Duration.ofDays(7)),
                new LogiVaultProperties.Login(5, Duration.ofMinutes(15)),
                ZoneOffset.UTC,
                new LogiVaultProperties.Cors(List.of()),
                new LogiVaultProperties.Admin("admin@logivault.test", "Admin12345!")
        );
    }
}
