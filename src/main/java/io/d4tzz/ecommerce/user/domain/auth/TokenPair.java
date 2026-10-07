package io.d4tzz.ecommerce.user.domain.auth;

public record TokenPair(
    AccessToken accessToken,
    RefreshToken refreshToken
) {}
