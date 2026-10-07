package io.d4tzz.ecommerce.catalog.application.product.command;

import io.d4tzz.ecommerce.catalog.domain.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeProductStatusCommand(@NotNull Long productId, @NotNull ProductStatus status) {}
