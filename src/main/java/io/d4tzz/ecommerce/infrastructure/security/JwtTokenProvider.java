package io.d4tzz.ecommerce.infrastructure.security;

import io.d4tzz.ecommerce.domain.security.AccessToken;
import io.d4tzz.ecommerce.domain.security.RefreshToken;
import io.d4tzz.ecommerce.domain.security.TokenPair;
import io.d4tzz.ecommerce.domain.security.TokenProvider;
import io.d4tzz.ecommerce.domain.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public TokenPair generateTokens(User user) {
        Instant now = Instant.now();
        AccessToken accessToken = buildAccessToken(user, now);
        RefreshToken refreshToken = buildRefreshToken(user, now);
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public TokenPair rotate(RefreshToken currentToken, User user) {
        Instant now = Instant.now();
        AccessToken accessToken = buildAccessToken(user, now);
        RefreshToken rotatedToken = buildRefreshToken(user, now, currentToken);
        return new TokenPair(accessToken, rotatedToken);
    }

    private AccessToken buildAccessToken(User user, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenValidity());
        String token = Jwts.builder()
            .subject(user.getId().value().toString())
            .claim("email", user.getEmail().value())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
        return new AccessToken(token, user.getId(), user.getEmail(), user.getRole(), expiresAt);
    }

    private RefreshToken buildRefreshToken(User user, Instant now) {
        return buildRefreshToken(user, now, null);
    }

    private RefreshToken buildRefreshToken(User user, Instant now, RefreshToken currentToken) {
        Instant expiresAt = now.plus(properties.refreshTokenValidity());
        String rawToken = Jwts.builder()
            .subject(user.getId().value().toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();

        if (currentToken != null) {
            return currentToken.rotate(rawToken, properties.refreshTokenValidity());
        }
        return RefreshToken.create(user.getId(), rawToken, properties.refreshTokenValidity());
    }
}
