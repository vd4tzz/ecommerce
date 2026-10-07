package io.d4tzz.ecommerce.catalog.query.product;

import java.math.BigDecimal;

public record ProductCardView(
    Long id,
    String name,
    String slug,
    String mainImageUrl,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Long categoryId,
    String categoryName,
    String categorySlug
) {}
