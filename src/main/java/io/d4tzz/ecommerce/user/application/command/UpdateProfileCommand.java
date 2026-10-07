package io.d4tzz.ecommerce.user.application.command;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileCommand(
    @NotNull Long userId,
    String fullName,
    String phoneNumber,
    String avatarUrl
) {}
