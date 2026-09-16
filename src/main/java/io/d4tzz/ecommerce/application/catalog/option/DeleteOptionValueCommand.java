package io.d4tzz.ecommerce.application.catalog.option;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeleteOptionValueCommand(@NotNull @Positive Long optionId, @NotNull @Positive Long valueId) {}
