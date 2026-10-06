package io.d4tzz.ecommerce.inventory.domain;

import io.d4tzz.ecommerce.domain.catalog.product.VariantId;
import java.util.Collection;
import java.util.Map;

public interface InventoryItemRepository {
    /**
     * Loads and pessimistically locks ({@code SELECT ... FOR UPDATE}) the items of the given variants,
     * always in ascending variant id order to avoid deadlocks. Variants without an item are absent from the map.
     */
    Map<VariantId, InventoryItem> findAllForUpdate(WarehouseId warehouseId, Collection<VariantId> variantIds);

    void saveAll(Collection<InventoryItem> items);
}
