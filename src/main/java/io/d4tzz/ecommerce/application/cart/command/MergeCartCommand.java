package io.d4tzz.ecommerce.application.cart.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MergeCartCommand(
    @NotNull Long userId,
    @NotNull List<@Valid Item> items
) {
    public record Item(
        @NotNull Long variantId,
        @Min(1) int quantity
    ) {}
}
