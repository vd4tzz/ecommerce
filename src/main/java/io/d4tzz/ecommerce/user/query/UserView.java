package io.d4tzz.ecommerce.user.query;

import java.time.Instant;

public record UserView(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    String avatarUrl,
    String role,
    String status,
    boolean emailVerified,
    Address address,
    Instant lastLoginAt,
    Instant createdAt
) {
    public record Address(String province, String district, String ward, String detail) {}
}
