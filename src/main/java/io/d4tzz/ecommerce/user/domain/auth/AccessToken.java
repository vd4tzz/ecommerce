package io.d4tzz.ecommerce.user.domain.auth;

import io.d4tzz.ecommerce.user.domain.*;

import java.time.Instant;

public record AccessToken(String value, UserId userId, Email email, UserRole role, Instant expiresAt) {
    public AccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Access token value must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt must not be null");
        }
    }

    public AccessToken(String value, UserId userId, String email, String role, Instant expiresAt) {
        this(value, userId, Email.of(email), UserRole.valueOf(role), expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
