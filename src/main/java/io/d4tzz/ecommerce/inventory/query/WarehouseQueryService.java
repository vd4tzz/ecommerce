package io.d4tzz.ecommerce.inventory.query;

import java.util.List;
import java.util.Optional;

/** Read side of warehouses. */
public interface WarehouseQueryService {
    /** All warehouses, active ones first, then by code. */
    List<WarehouseView> getWarehouses();

    Optional<WarehouseView> getWarehouse(Long id);
}
