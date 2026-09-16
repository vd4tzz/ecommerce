package io.d4tzz.ecommerce.application.catalog.option;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOptionCommand(@NotBlank String name, @NotNull List<@NotBlank String> values) {}
