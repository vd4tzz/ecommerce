package io.d4tzz.ecommerce.catalog.application.product.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderProductMediaCommand(@NotNull Long productId, @NotEmpty List<String> mediaIds) {}
