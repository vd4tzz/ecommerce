package io.d4tzz.ecommerce.application.catalog.category;

public record UpdateCategoryCommand(
    Long categoryId,
    String name,
    String slug
) {}
