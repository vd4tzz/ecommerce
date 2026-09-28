package io.d4tzz.ecommerce.application.cart.command;

import jakarta.validation.constraints.NotNull;

public record ClearCartCommand(
    @NotNull Long userId
) {}
