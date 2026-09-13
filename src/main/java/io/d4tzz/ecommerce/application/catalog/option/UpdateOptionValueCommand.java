package io.d4tzz.ecommerce.application.catalog.option;

public record UpdateOptionValueCommand(Long optionId, Long valueId, String name) {}
