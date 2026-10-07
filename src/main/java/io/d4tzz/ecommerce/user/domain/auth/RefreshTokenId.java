package io.d4tzz.ecommerce.user.domain.auth;

public record RefreshTokenId(Long value) {

    public static RefreshTokenId of(Long value) {
        return new RefreshTokenId(value);
    }
}
