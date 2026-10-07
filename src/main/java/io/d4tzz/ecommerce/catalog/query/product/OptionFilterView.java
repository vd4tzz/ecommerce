package io.d4tzz.ecommerce.catalog.query.product;

public record OptionFilterView(
    Long optionId,
    Long valueId,
    String optionName,
    String valueName
) {}
