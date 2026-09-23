package io.d4tzz.ecommerce.application.user;

import io.d4tzz.ecommerce.domain.user.User;
import java.time.Instant;

public record UserDto(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    String avatarUrl,
    String role,
    String status,
    boolean emailVerified,
    Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId() != null ? user.getId().value() : null,
            user.getEmail().value(),
            user.getFullName().value(),
            user.getPhoneNumber() != null ? user.getPhoneNumber().value() : null,
            user.getAvatarUrl(),
            user.getRole().name(),
            user.getStatus().name(),
            user.isEmailVerified(),
            user.getCreatedAt()
        );
    }
}
