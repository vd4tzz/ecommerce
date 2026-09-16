package io.d4tzz.ecommerce.application.catalog.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AddProductVariantCommand(@NotNull Long productId, @NotEmpty List<@Valid Variant> variants) {
    public record Variant(
        @NotNull String sku,
        @NotNull BigDecimal price,
        String mediaId,
        @NotNull Integer length,
        @NotNull Integer width,
        @NotNull Integer height,
        @NotNull Integer weight,
        @NotNull Map<Long, Long> combination
    ) {}
}
