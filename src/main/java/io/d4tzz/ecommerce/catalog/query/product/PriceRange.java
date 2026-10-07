package io.d4tzz.ecommerce.catalog.query.product;

import java.math.BigDecimal;

public record PriceRange(
    BigDecimal minPrice,
    BigDecimal maxPrice
) {}
