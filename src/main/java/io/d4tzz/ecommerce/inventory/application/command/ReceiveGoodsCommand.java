package io.d4tzz.ecommerce.inventory.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReceiveGoodsCommand(
    @NotNull Long warehouseId,
    @NotBlank @Size(max = 100) String receiptCode,
    @NotNull String note,
    @NotEmpty List<@Valid Item> items
) {
    public record Item(@NotNull Long variantId, @Positive int quantity) {}
}
