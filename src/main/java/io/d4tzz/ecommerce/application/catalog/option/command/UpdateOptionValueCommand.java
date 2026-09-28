package io.d4tzz.ecommerce.application.catalog.option.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateOptionValueCommand(
    @NotNull @Positive Long optionId,
    @NotNull @Positive Long valueId,
    @NotBlank String name
) {}
