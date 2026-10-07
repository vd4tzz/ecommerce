package io.d4tzz.ecommerce.catalog.application.product.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddProductOptionCommand(@NotNull Long productId, @NotNull Long optionId, @NotEmpty List<Long> valueIds) {}
