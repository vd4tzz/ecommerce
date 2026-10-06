package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ReserveStockCommand(@NotNull Long orderId, @NotNull Long warehouseId, @NotEmpty List<@Valid Line> lines) {
    public record Line(@NotNull Long variantId, @Positive int quantity) {}
}
