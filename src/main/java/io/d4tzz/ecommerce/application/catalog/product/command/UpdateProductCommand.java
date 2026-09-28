package io.d4tzz.ecommerce.application.catalog.product.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record UpdateProductCommand(
    @NotNull Long productId,
    @NotNull String name,
    @NotNull String slug,
    @NotNull String description,
    @NotNull Long categoryId,
    @NotNull Map<String, List<String>> attributes
) {}
