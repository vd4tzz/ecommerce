package io.d4tzz.ecommerce.application.user.command;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
    @NotBlank String refreshToken
) {}
