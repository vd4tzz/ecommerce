package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** What an order needs, and the province it goes to (warehouses there are preferred). */
public record AllocateStockCommand(@NotBlank String province, @NotEmpty List<@Valid Line> lines) {
    public record Line(@NotNull Long variantId, @Positive int quantity) {}
}
