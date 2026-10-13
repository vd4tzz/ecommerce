package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.catalog.domain.product.VariantId;
import java.util.Map;

/** What one warehouse could send: sellable units per variant, and whether it is near where the order goes. */
public record WarehouseStock(WarehouseId warehouseId, boolean nearDestination, Map<VariantId, Integer> available) {
    public WarehouseStock {
        available = Map.copyOf(available);
    }

    /** Sellable units of the variant, 0 when the warehouse has none. */
    public int availableOf(VariantId variantId) {
        return Math.max(0, available.getOrDefault(variantId, 0));
    }
}
