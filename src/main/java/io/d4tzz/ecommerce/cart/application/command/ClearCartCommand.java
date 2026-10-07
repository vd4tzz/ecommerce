package io.d4tzz.ecommerce.cart.application.command;

import jakarta.validation.constraints.NotNull;

public record ClearCartCommand(
    @NotNull Long userId
) {}
