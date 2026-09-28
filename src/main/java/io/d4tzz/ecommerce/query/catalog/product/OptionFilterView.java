package io.d4tzz.ecommerce.query.catalog.product;

public record OptionFilterView(
    Long optionId,
    Long valueId,
    String optionName,
    String valueName
) {}
