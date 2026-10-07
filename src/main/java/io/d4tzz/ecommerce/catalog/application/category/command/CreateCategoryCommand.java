package io.d4tzz.ecommerce.catalog.application.category.command;

import io.d4tzz.ecommerce.catalog.domain.category.AttributeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCategoryCommand(
    @NotBlank String name,
    @NotBlank String slug,
    Long parentId,
    @NotNull List<@Valid Attribute> attributes
) {
    public record Attribute(
        @NotBlank String code,
        @NotBlank String label,
        @NotNull AttributeType type,
        @NotNull List<@Valid Value> values
    ) {
        public record Value(@NotBlank String code, @NotBlank String label) {}
    }
}
