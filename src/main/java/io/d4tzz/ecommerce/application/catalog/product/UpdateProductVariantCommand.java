package io.d4tzz.ecommerce.application.catalog.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateProductVariantCommand(
    @NotNull Long productId,
    @NotNull Long variantId,
    @NotNull BigDecimal price,
    @NotNull BigDecimal compareAt,
    @NotBlank String mediaId,
    @NotNull Integer length,
    @NotNull Integer width,
    @NotNull Integer height,
    @NotNull Integer weight
) {}
