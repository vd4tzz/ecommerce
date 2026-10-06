package io.d4tzz.ecommerce.inventory.query;

/** Sellable stock of a variant in one active warehouse (input for choosing a warehouse). */
public record VariantAvailabilityView(Long variantId, Long warehouseId, String warehouseCode, int available) {}
