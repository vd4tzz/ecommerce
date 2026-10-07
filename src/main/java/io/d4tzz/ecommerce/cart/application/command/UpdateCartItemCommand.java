package io.d4tzz.ecommerce.cart.application.command;

import io.d4tzz.ecommerce.cart.domain.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sets the quantity of one variant. To remove it, use {@link RemoveCartItemCommand}. */
public record UpdateCartItemCommand(
    @NotNull Long userId,
    @NotNull Long variantId,
    @Min(1) @Max(CartItem.MAX_QUANTITY) int quantity
) {}
