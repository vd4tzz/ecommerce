package io.d4tzz.ecommerce.query.catalog;

import java.math.BigDecimal;

public record PriceRange(
    BigDecimal minPrice,
    BigDecimal maxPrice
) {}
