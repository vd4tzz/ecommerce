package io.d4tzz.ecommerce.application.catalog.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddProductMediaCommand(@NotNull Long productId, @NotEmpty List<String> mediaIds) {}
