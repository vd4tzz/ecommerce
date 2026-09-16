package io.d4tzz.ecommerce.application.catalog.option;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddOptionValueCommand(@NotNull @Positive Long optionId, @NotBlank String name) {}
