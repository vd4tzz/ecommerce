package io.d4tzz.ecommerce.query.catalog;

public record CategoryFilterFacetView(
    Long id,
    String name,
    String slug,
    Long productCount
) {}
