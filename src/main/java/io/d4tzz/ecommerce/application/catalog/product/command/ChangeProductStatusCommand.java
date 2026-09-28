package io.d4tzz.ecommerce.application.catalog.product.command;

import io.d4tzz.ecommerce.domain.catalog.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeProductStatusCommand(@NotNull Long productId, @NotNull ProductStatus status) {}
