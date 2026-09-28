package io.d4tzz.ecommerce.query.catalog.product;

import java.math.BigDecimal;

public record PriceRange(
    BigDecimal minPrice,
    BigDecimal maxPrice
) {}
