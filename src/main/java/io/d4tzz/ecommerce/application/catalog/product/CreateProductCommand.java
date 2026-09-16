package io.d4tzz.ecommerce.application.catalog.product;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateProductCommand(
    @NotNull String name,
    @NotNull String slug,
    @NotNull String description,
    @NotNull Long categoryId,
    @NotNull Map<String, Object> attributes
) {}
