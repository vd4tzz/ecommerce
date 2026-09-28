package io.d4tzz.ecommerce.application.catalog.product.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderProductOptionsCommand(@NotNull Long productId, @NotNull List<Long> optionIds) {}
