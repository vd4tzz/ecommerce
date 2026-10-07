package io.d4tzz.ecommerce.cart.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Items of a guest cart to merge after login. A quantity above the item limit is lowered, not rejected. */
public record MergeCartCommand(
    @NotNull Long userId,
    @NotNull List<@Valid Item> items
) {
    public record Item(
        @NotNull Long variantId,
        @Min(1) int quantity
    ) {}
}
