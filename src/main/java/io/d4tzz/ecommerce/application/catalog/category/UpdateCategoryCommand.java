package io.d4tzz.ecommerce.application.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCategoryCommand(
    @NotNull @Positive Long categoryId,
    @NotBlank String name,
    @NotBlank String slug
) {}

