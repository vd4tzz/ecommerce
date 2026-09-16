package io.d4tzz.ecommerce.query.catalog;

import java.math.BigDecimal;

public record AdminProductTableRowView(
    Long id,
    String name,
    String slug,
    String mainImageUrl,
    Long categoryId,
    String categoryName,
    String status,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    int variantCount,
    int mediaCount
) {}
