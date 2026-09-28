package io.d4tzz.ecommerce.application.catalog.category.command;

import io.d4tzz.ecommerce.domain.catalog.category.AttributeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateCategoryAttributesCommand(
    @NotNull @Positive Long categoryId,
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
