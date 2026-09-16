package io.d4tzz.ecommerce.query.catalog;

import java.util.List;
import java.util.Map;

public record FilterSidebarView(
    PriceRange priceRange,
    List<CategoryFilterFacetView> subCategories,
    Map<String, List<OptionFacetDto>> options,
    Map<String, List<AttributeFacet>> attributes
) {}
