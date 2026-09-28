package io.d4tzz.ecommerce.application.user.command;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailCommand(
    @NotBlank String email,
    @NotBlank String code
) {}
