package com.logivault.common.security;

import com.logivault.common.config.LogiVaultProperties;
import com.logivault.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(LogiVaultProperties properties, Clock clock) {
        byte[] keyBytes = Decoders.BASE64.decode(properties.jwt().secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("logivault.jwt.secret must decode to at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
        this.clock = clock;
    }

    public String createAccessToken(AuthUser user) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID id = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));
            return Optional.of(new AuthUser(id, email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
