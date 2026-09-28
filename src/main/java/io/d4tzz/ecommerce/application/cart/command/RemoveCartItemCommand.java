package io.d4tzz.ecommerce.application.cart.command;

import jakarta.validation.constraints.NotNull;

public record RemoveCartItemCommand(
    @NotNull Long userId,
    @NotNull Long variantId
) {}
