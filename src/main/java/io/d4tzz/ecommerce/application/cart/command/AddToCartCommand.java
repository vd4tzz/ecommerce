package io.d4tzz.ecommerce.application.cart.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartCommand(
    @NotNull Long userId,
    @NotNull Long variantId,
    @Min(1) int quantity
) {}
