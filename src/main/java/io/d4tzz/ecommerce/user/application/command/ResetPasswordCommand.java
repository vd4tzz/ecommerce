package io.d4tzz.ecommerce.user.application.command;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordCommand(
    @NotBlank String email,
    @NotBlank String code,
    @NotBlank String newPassword
) {}
