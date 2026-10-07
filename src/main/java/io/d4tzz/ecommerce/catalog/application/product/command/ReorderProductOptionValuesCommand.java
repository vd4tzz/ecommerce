package io.d4tzz.ecommerce.catalog.application.product.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderProductOptionValuesCommand(
    @NotNull Long productId,
    @NotNull Long optionId,
    @NotNull List<Long> valueIds
) {}
