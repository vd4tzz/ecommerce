package io.d4tzz.ecommerce.domain.security;

public record TokenPair(
    AccessToken accessToken,
    RefreshToken refreshToken
) {}
