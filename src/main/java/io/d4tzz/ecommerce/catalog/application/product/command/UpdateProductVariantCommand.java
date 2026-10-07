package io.d4tzz.ecommerce.catalog.application.product.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateProductVariantCommand(
    @NotNull Long productId,
    @NotNull Long variantId,
    @NotNull BigDecimal price,
    BigDecimal compareAt,
    String mediaId,
    @NotNull Integer length,
    @NotNull Integer width,
    @NotNull Integer height,
    @NotNull Integer weight
) {}
