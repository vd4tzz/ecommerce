package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class RefreshToken {

    private RefreshTokenId id;
    private UserId userId;
    private String tokenHash;
    private UUID familyId;
    private boolean revoked;
    private Instant expiresAt;
    private Instant createdAt;
    private String rawToken;

    public RefreshToken(
        RefreshTokenId id,
        UserId userId,
        String tokenHash,
        UUID familyId,
        boolean revoked,
        Instant expiresAt,
        Instant createdAt
    ) {
        this(id, userId, tokenHash, familyId, revoked, expiresAt, createdAt, null);
    }

    public RefreshToken(
        RefreshTokenId id,
        UserId userId,
        String tokenHash,
        UUID familyId,
        boolean revoked,
        Instant expiresAt,
        Instant createdAt,
        String rawToken
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.revoked = revoked;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.rawToken = rawToken;
    }

    public static RefreshToken create(UserId userId, String rawToken, Duration validity) {
        Instant now = Instant.now();
        return new RefreshToken(
            null,
            userId,
            hash(rawToken),
            UUID.randomUUID(),
            false,
            now.plus(validity),
            now,
            rawToken
        );
    }

    public RefreshToken rotate(String newRawToken, Duration validity) {
        this.revoke();
        Instant now = Instant.now();
        return new RefreshToken(
            null,
            this.userId,
            hash(newRawToken),
            this.familyId,
            false,
            now.plus(validity),
            now,
            newRawToken
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public RefreshTokenId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getRawToken() {
        return rawToken;
    }

    public static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
