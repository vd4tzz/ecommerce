package io.d4tzz.ecommerce.user.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangePasswordCommand(
    @NotNull Long userId,
    @NotBlank String oldPassword,
    @NotBlank String newPassword
) {}
