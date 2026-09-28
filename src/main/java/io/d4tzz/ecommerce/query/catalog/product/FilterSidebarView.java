package io.d4tzz.ecommerce.query.catalog.product;

import java.util.List;
import java.util.Map;

public record FilterSidebarView(
    PriceRange priceRange,
    List<CategoryFilterView> subCategories,
    Map<String, List<OptionFilterView>> options,
    Map<String, List<AttributeFilterView>> attributes
) {}
