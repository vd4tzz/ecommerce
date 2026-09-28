package io.d4tzz.ecommerce.application.user.command;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
    @NotBlank String email,
    @NotBlank String password
) {}
