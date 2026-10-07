package io.d4tzz.ecommerce.user.application.command;

import jakarta.validation.constraints.NotBlank;

public record RequestPasswordResetCommand(
    @NotBlank String email
) {}
