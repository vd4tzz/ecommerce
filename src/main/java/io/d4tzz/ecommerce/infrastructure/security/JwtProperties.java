package io.d4tzz.ecommerce.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String secretKey,
    Duration accessTokenValidity,
    Duration refreshTokenValidity
) {}
