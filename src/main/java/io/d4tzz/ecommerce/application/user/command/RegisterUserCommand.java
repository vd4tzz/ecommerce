package io.d4tzz.ecommerce.application.user.command;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserCommand(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String fullName
) {}
