package io.d4tzz.ecommerce.application.catalog.product.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddProductOptionCommand(@NotNull Long productId, @NotNull Long optionId, @NotEmpty List<Long> valueIds) {}
