package io.d4tzz.ecommerce.application.catalog.product;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderProductOptionValuesCommand(
    @NotNull Long productId,
    @NotNull Long optionId,
    @NotNull List<Long> valueIds
) {}
