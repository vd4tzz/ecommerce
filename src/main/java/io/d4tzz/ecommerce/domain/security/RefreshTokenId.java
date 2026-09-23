package io.d4tzz.ecommerce.domain.security;

public record RefreshTokenId(Long value) {

    public static RefreshTokenId of(Long value) {
        return new RefreshTokenId(value);
    }
}
