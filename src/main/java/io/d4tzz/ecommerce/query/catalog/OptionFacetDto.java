package io.d4tzz.ecommerce.query.catalog;

public record OptionFacetDto(
    Long optionId,
    Long valueId,
    String optionName,
    String valueName
) {}
